package com.liquid.kochanowski;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.liquid.kochanowski.TimeTableContract.ClassTable;
import com.liquid.kochanowski.TimeTableContract.LessonTable;
import com.liquid.kochanowski.TimeTableContract.TeacherTable;

/**
 * Created by liquid on 09.12.14.
 */
public class LessonListAdapter extends RecyclerView.Adapter<LessonListAdapter.LessonViewHolder>
{
    private Cursor cur;

    private Context context;

    private String tableName;
    private String tableType;
    private int dayId;
    private int groupId;

    private int lastPosition = -1;

    public static class LessonViewHolder extends RecyclerView.ViewHolder
    {
        public TextView subjectName;
        public TextView teacherName;
        public TextView classroomName;
        public TextView groupName;

        public RelativeLayout container;

        public LessonViewHolder (View v)
        {
            super (v);

            subjectName = (TextView) v.findViewById (R.id.subjectName);
            teacherName = (TextView) v.findViewById (R.id.teacherName);
            classroomName = (TextView) v.findViewById (R.id.classroomName);
            groupName = (TextView) v.findViewById (R.id.groupName);

            container = (RelativeLayout) v;
        }
    }

    public LessonListAdapter (SQLiteDatabase db, String tableName, String tableType, int dayId, int groupId, Context context)
    {
        this.tableName = tableName;
        this.tableType = tableType;
        this.dayId = dayId;
        this.groupId = groupId;
        this.context = context;

        String query = "SELECT * FROM " + LessonTable.TABLE_NAME +
                " JOIN " + TeacherTable.TABLE_NAME +
                " ON " + LessonTable.TABLE_NAME + "." + LessonTable.COLUMN_NAME_TEACHER_CODE +
                "=" + TeacherTable.TABLE_NAME + "." + TeacherTable.COLUMN_NAME_TEACHER_CODE +
                " JOIN " + ClassTable.TABLE_NAME +
                " ON " + LessonTable.TABLE_NAME + "." + LessonTable.COLUMN_NAME_CLASS_NAME_SHORT +
                "=" + ClassTable.TABLE_NAME + "." + ClassTable.COLUMN_NAME_NAME_SHORT;

        switch (tableType)
        {
            case "class":
                query += " WHERE " + LessonTable.TABLE_NAME + "." + LessonTable.COLUMN_NAME_CLASS_NAME_SHORT + "='" + tableName + "'";
                break;
            case "teacher":
                query += " WHERE " + LessonTable.TABLE_NAME + "." + LessonTable.COLUMN_NAME_TEACHER_CODE + "='" + tableName + "'";
                break;
            case "classroom":
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

        Log.i ("query", query);

        cur = db.rawQuery (query, null);
    }

    @Override
    public void onBindViewHolder (LessonViewHolder holder, int position)
    {
        cur.moveToPosition (position);

        String subject = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_SUBJECT));
        String classroom = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_CLASSROOM));
        String add = "";
        String add2 = "";
        String add3 = "";

        //holder.subjectName.setText (subject);
        //holder.classroomName.setText (classroom);

        switch (tableType)
        {
            case "class":
                add = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_TEACHER_CODE));
                add2 = cur.getString (cur.getColumnIndexOrThrow (TeacherTable.COLUMN_NAME_TEACHER_NAME));
                add3 = cur.getString (cur.getColumnIndexOrThrow (TeacherTable.COLUMN_NAME_TEACHER_SURNAME));

                holder.subjectName.setText (subject);
                holder.classroomName.setText (classroom);
                holder.teacherName.setText (add2 + " " + add3 + " (" + add + ")");

                //Log.i ("liquid", subject + " " + classroom + " " + add);

                break;
            case "teacher":
                add = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_CLASS_NAME_SHORT));
                add2 = cur.getString (cur.getColumnIndexOrThrow (ClassTable.COLUMN_NAME_NAME_LONG));

                holder.subjectName.setText (subject);
                holder.classroomName.setText (classroom);
                holder.teacherName.setText (add2 + " (" + add + ")");

                break;
            case "classroom":
                add = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_TEACHER_CODE));
                add2 = cur.getString (cur.getColumnIndexOrThrow (TeacherTable.COLUMN_NAME_TEACHER_NAME));
                add3 = cur.getString (cur.getColumnIndexOrThrow (TeacherTable.COLUMN_NAME_TEACHER_SURNAME));

                String classname = cur.getString (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_CLASS_NAME_SHORT));

                holder.subjectName.setText (subject);
                holder.classroomName.setText (classname);
                holder.teacherName.setText (add2 + " " + add3 + " (" + add + ")");

                break;
        }

        int group = cur.getInt (cur.getColumnIndexOrThrow (LessonTable.COLUMN_NAME_GROUP_ID));
        if (group != 0)
        {
            holder.groupName.setText ("GRUPA " + group);
            holder.groupName.setVisibility (View.VISIBLE);
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
}