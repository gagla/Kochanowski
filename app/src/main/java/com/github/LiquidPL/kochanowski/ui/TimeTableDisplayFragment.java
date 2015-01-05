package com.github.LiquidPL.kochanowski.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.LiquidPL.kochanowski.R;
import com.github.LiquidPL.kochanowski.db.TimeTableContract.ClassTable;
import com.github.LiquidPL.kochanowski.db.TimeTableContract.HourTable;
import com.github.LiquidPL.kochanowski.db.TimeTableContract.LessonTable;
import com.github.LiquidPL.kochanowski.db.TimeTableContract.TeacherTable;
import com.github.LiquidPL.kochanowski.util.DbUtils;
import com.github.LiquidPL.kochanowski.util.PrefUtils;
import com.liquid.kochanparser.TimeTableType;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TimeTableDisplayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TimeTableDisplayFragment extends Fragment implements View.OnClickListener
{
    public class Group
    {
        // group type constants
        public static final int GROUP_ONE = 1;
        public static final int GROUP_TWO = 2;
        public static final int GROUP_BOTH = 0;
    }

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_TABLE_NAME = "tablename";
    private static final String ARG_TABLE_TYPE = "tabletype";
    private static final String ARG_DAY_ID     = "dayid";
    private static final String ARG_GROUP_ID   = "groupid";

    // the given fragments parameters
    private String tableName;
    private int tableType;
    private int dayId;
    private int groupId;

    private Activity activity;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private Button syncButton;
    private TextView noTimeTablesAlert;

    private class LessonListAdapter extends RecyclerView.Adapter<LessonListAdapter.LessonViewHolder>
    {
        private Cursor cur;
        private Cursor oldCur;

        private Context context;

        private String tableName;
        private int tableType;
        private int dayId;
        private int groupId;

        private int resource;

        public class LessonViewHolder extends RecyclerView.ViewHolder
        {
            public TextView subjectName;
            public TextView teacherName;
            public TextView classroomName;
            public TextView groupName;
            public TextView hour;

            public LessonViewHolder (View v)
            {
                super (v);

                subjectName = (TextView) v.findViewById (R.id.subject_name);
                teacherName = (TextView) v.findViewById (R.id.teacher_name);
                classroomName = (TextView) v.findViewById (R.id.classroom_name);
                groupName = (TextView) v.findViewById (R.id.group_name);
                hour = (TextView) v.findViewById (R.id.hour_label);
            }
        }

        public LessonListAdapter (int resource, String tableName, int tableType, int dayId, int groupId, Context context)
        {
            this.tableName = tableName;
            this.tableType = tableType;
            this.dayId = dayId;
            this.groupId = groupId;
            this.context = context;

            this.resource = resource;

            cur = formQuery (tableName, tableType, dayId, groupId);
        }

        private Cursor formQuery (String tableName, int tableType, int dayId, int groupId)
        {
            String query = "SELECT * FROM " + LessonTable.TABLE_NAME +
                    " JOIN " + TeacherTable.TABLE_NAME +
                    " ON " + LessonTable.TABLE_NAME + "." + LessonTable.COLUMN_NAME_TEACHER_CODE +
                    "=" + TeacherTable.TABLE_NAME + "." + TeacherTable.COLUMN_NAME_TEACHER_CODE +
                    " JOIN " + ClassTable.TABLE_NAME +
                    " ON " + LessonTable.TABLE_NAME + "." + LessonTable.COLUMN_NAME_CLASS_NAME_SHORT +
                    "=" + ClassTable.TABLE_NAME + "." + ClassTable.COLUMN_NAME_NAME_SHORT +
                    " JOIN " + HourTable.TABLE_NAME +
                    " ON " + LessonTable.TABLE_NAME + "." + LessonTable.COLUMN_NAME_HOUR_ID +
                    "=" + HourTable.TABLE_NAME + "." + HourTable._ID;

            switch (tableType)
            {
                case TimeTableType.CLASS:
                    query += " WHERE " + LessonTable.TABLE_NAME + "." + LessonTable.COLUMN_NAME_CLASS_NAME_SHORT + "='" + tableName + "'";
                    break;
                case TimeTableType.TEACHER:
                    query += " WHERE " + LessonTable.TABLE_NAME + "." + LessonTable.COLUMN_NAME_TEACHER_CODE + "='" + tableName + "'";
                    break;
                case TimeTableType.CLASSROOM:
                    query += " WHERE " + LessonTable.COLUMN_NAME_CLASSROOM + "=" + tableName;
                    break;
            }

            query += " AND " + LessonTable.COLUMN_NAME_DAY + "=" + dayId;

            if (groupId != 0)
            {
                query += " AND (" + LessonTable.COLUMN_NAME_GROUP_ID + "=" + "0" +
                        " OR " + LessonTable.COLUMN_NAME_GROUP_ID + "=" + groupId + ")";
            }

            query += " ORDER BY " + LessonTable.COLUMN_NAME_HOUR_ID + " ASC";

            return DbUtils.getReadableDatabase ().rawQuery (query, null);
        }

        @Override
        public void onBindViewHolder (LessonViewHolder holder, int position)
        {
            cur.moveToPosition (position);

            String subject = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_SUBJECT));
            String classroom = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_CLASSROOM));
            String add;
            String add2;
            String add3;

            int starthour = cur.getInt (cur.getColumnIndexOrThrow (HourTable.COLUMN_NAME_START_HOUR));
            int startminute = cur.getInt (cur.getColumnIndexOrThrow (HourTable.COLUMN_NAME_START_MINUTE));
            int endhour = cur.getInt (cur.getColumnIndexOrThrow (HourTable.COLUMN_NAME_END_HOUR));
            int endminute = cur.getInt (cur.getColumnIndexOrThrow (HourTable.COLUMN_NAME_END_MINUTE));

            //holder.subjectName.setText (subject);
            //holder.classroomName.setText (classroom);

            switch (tableType)
            {
                case TimeTableType.CLASS:
                    add = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_TEACHER_CODE));
                    add2 = cur.getString (cur.getColumnIndexOrThrow (TeacherTable.COLUMN_NAME_TEACHER_NAME));
                    add3 = cur.getString (cur.getColumnIndexOrThrow (TeacherTable.COLUMN_NAME_TEACHER_SURNAME));

                    holder.subjectName.setText (subject);
                    holder.classroomName.setText (classroom);
                    holder.teacherName.setText (add2 + " " + add3 + " (" + add + ")");

                    break;
                case TimeTableType.TEACHER:
                    add = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_CLASS_NAME_SHORT));
                    add2 = cur.getString (cur.getColumnIndexOrThrow (ClassTable.COLUMN_NAME_NAME_LONG));

                    holder.subjectName.setText (subject);
                    holder.classroomName.setText (classroom);
                    holder.teacherName.setText (add2 + " (" + add + ")");

                    break;
                case TimeTableType.CLASSROOM:
                    add = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_TEACHER_CODE));
                    add2 = cur.getString (cur.getColumnIndexOrThrow (TeacherTable.COLUMN_NAME_TEACHER_NAME));
                    add3 = cur.getString (cur.getColumnIndexOrThrow (TeacherTable.COLUMN_NAME_TEACHER_SURNAME));

                    String classname = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_CLASS_NAME_SHORT));

                    holder.subjectName.setText (subject);
                    holder.classroomName.setText (classname);
                    holder.teacherName.setText (add2 + " " + add3 + " (" + add + ")");

                    break;
            }

            String hour = "" + starthour + ":" + startminute; if (startminute == 0) hour += "0";
            hour += "-" + endhour + ":" + endminute; if (endminute == 0) hour += "0";

            holder.hour.setText (hour);

            int group = cur.getInt (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_GROUP_ID));
            if (group != 0)
            {
                holder.groupName.setText ("GRUPA " + group);
                holder.groupName.setVisibility (View.VISIBLE);
            }
            else
            {
                holder.groupName.setText ("");
                holder.groupName.setVisibility (View.GONE);
            }
        }

        @Override
        public LessonViewHolder onCreateViewHolder (ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from (parent.getContext ()).inflate (R.layout.lesson_item, parent, false);

            return new LessonViewHolder (v);
        }

        @Override
        public int getItemCount ()
        {
            return cur.getCount ();
        }

        public void setGroup (int groupId)
        {
            if (this.groupId == groupId) return;

            oldCur = cur;
            cur = formQuery (tableName, tableType, dayId, groupId);

            int previousGroup = this.groupId;
            boolean bothGroups = false;
            boolean toBothGroups = false;

            if (previousGroup == 0) bothGroups = true;

            this.groupId = groupId;

            if (groupId == 0)
            {
                toBothGroups = true;

                switch (previousGroup)
                {
                    case 1:
                        groupId = 2;
                        break;
                    case 2:
                        groupId = 1;
                        break;
                }
            }

            oldCur.moveToFirst ();
            int offset = 0;

            if (!toBothGroups) do
            {
                int group = oldCur.getInt (oldCur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_GROUP_ID));

                if (group != 0 && group != groupId)
                {
                    notifyItemRemoved (oldCur.getPosition () - offset);
                    offset++;
                }

                oldCur.moveToNext ();
            }
            while (!oldCur.isAfterLast ());

            if (bothGroups) return;

            cur.moveToFirst ();

            do
            {
                int group = cur.getInt (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_GROUP_ID));

                if (group == groupId)
                {
                    notifyItemInserted (cur.getPosition ());
                }

                cur.moveToNext ();
            }
            while (!cur.isAfterLast ());
        }
    }


    interface Clickable
    {
        void onSyncButtonClick (View v);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tableName Name of the timetable, corresponding to a column in the database
     * @param tableType The timetable type to display (class/classroom)
     * @param groupId The group to be displayed
     *
     * @return A new instance of fragment TimeTableDisplayFragment.
     */
    public static TimeTableDisplayFragment newInstance (String tableName, int tableType, int dayId, int groupId)
    {
        TimeTableDisplayFragment fragment = new TimeTableDisplayFragment ();
        Bundle args = new Bundle ();

        args.putString (ARG_TABLE_NAME, tableName);
        args.putInt (ARG_TABLE_TYPE, tableType);
        args.putInt (ARG_DAY_ID, dayId);
        args.putInt (ARG_GROUP_ID, groupId);

        fragment.setArguments (args);
        return fragment;
    }

    public TimeTableDisplayFragment ()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        if (getArguments () != null)
        {
            tableName = getArguments ().getString (ARG_TABLE_NAME);
            tableType = getArguments ().getInt (ARG_TABLE_TYPE);
            dayId = getArguments ().getInt (ARG_DAY_ID);
            groupId = getArguments ().getInt (ARG_GROUP_ID);
        }
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate (R.layout.fragment_timetable_display, container, false);
        recyclerView = (RecyclerView) view.findViewById (R.id.lesson_list);

        syncButton = (Button) view.findViewById (R.id.button_sync);

        syncButton.setOnClickListener (this);

        noTimeTablesAlert = (TextView) view.findViewById (R.id.alert_no_timetables);

        resetVisibility ();

        layoutManager = new LinearLayoutManager (activity);
        recyclerView.setLayoutManager (layoutManager);
        recyclerView.setItemAnimator (new DefaultItemAnimator ());

        adapter = new LessonListAdapter (R.layout.lesson_item,
                tableName,
                tableType,
                dayId,
                groupId,
                this.getActivity ());

        recyclerView.setAdapter (adapter);

        return view;
    }

    @Override
    public void onAttach (Activity activity)
    {
        super.onAttach (activity);
        this.activity = activity;
    }

    @Override
    public void onDetach ()
    {
        super.onDetach ();
    }

    public void setDay (int day)
    {
        this.dayId = day;
    }

    private void resetVisibility ()
    {
        if (PrefUtils.hasSyncedTimeTables (getActivity ()))
        {
            recyclerView.setVisibility (View.VISIBLE);
            syncButton.setVisibility (View.INVISIBLE);
            noTimeTablesAlert.setVisibility (View.INVISIBLE);
        }
        else
        {
            syncButton.setVisibility (View.VISIBLE);
            noTimeTablesAlert.setVisibility (View.VISIBLE);
            recyclerView.setVisibility (View.INVISIBLE);
        }
    }

    public void refresh ()
    {
        resetVisibility ();

        adapter = new LessonListAdapter (
                R.layout.lesson_item,
                tableName,
                tableType,
                dayId,
                groupId,
                this.getActivity ());

        recyclerView.setAdapter (adapter);
    }

    public void setGroup (int groupId) // 1,2, or 0 (both groups)
    {
        this.groupId = groupId;

        ((LessonListAdapter) adapter).setGroup (groupId);
    }

    @Override
    public void onClick (View v)
    {
        switch (v.getId ())
        {
            case R.id.button_sync:
                Intent intent = new Intent (getActivity (), SyncActivity.class);
                startActivity (intent);
                break;
        }
    }

    public void setTableName (String table)
    {
        this.tableName = table;
    }

    public String getTableName ()
    {
        return tableName;
    }

    public int getTableType ()
    {
        return tableType;
    }

    public int getDayId ()
    {
        return dayId;
    }

    public int getGroupId ()
    {
        return groupId;
    }
}