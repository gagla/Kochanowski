package com.github.LiquidPL.kochanowski.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.LiquidPL.kochanowski.R;
import com.github.LiquidPL.kochanowski.db.TimeTableContract.ClassTable;
import com.github.LiquidPL.kochanowski.parse.MasterlistDownloadRunnable;
import com.github.LiquidPL.kochanowski.parse.ThreadManager;
import com.github.LiquidPL.kochanowski.util.DbUtils;
import com.github.LiquidPL.kochanowski.util.PrefUtils;

import java.util.ArrayList;
import java.util.List;

public class SyncActivity extends BaseActivity implements AdapterView.OnItemSelectedListener
{
    private ThreadManager manager;

    private List<String> urls;

    private SQLiteDatabase db;

    public TextView currentDownload;
    public TextView currentCount;
    public ProgressBar progressBar;

    protected TextView syncResult;
    protected Button continueButton;

    protected Spinner classSelect;

    private class ClassSelectAdapter extends ArrayAdapter<String>
    {
        private Cursor cur;

        public ClassSelectAdapter (Context context, int resource)
        {
            super (context, resource);
            cur = DbUtils.getReadableDatabase ().rawQuery ("SELECT * FROM classes ORDER BY longname ASC", null);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent)
        {
            return getCustomView (R.layout.sync_spinner_item, position, parent);
        }

        @Override
        public View getDropDownView (int position, View convertView, ViewGroup parent)
        {
            return getCustomView (R.layout.sync_spinner_item, position, parent);
        }

        private View getCustomView (int resource, int position, ViewGroup parent)
        {
            TextView view = (TextView) LayoutInflater.from (parent.getContext ()).inflate (resource, parent, false);

            cur.moveToPosition (position);
            String shortname = cur.getString (cur.getColumnIndexOrThrow (ClassTable.COLUMN_NAME_NAME_SHORT));
            String longname = cur.getString (cur.getColumnIndexOrThrow (ClassTable.COLUMN_NAME_NAME_LONG));

            view.setText (longname + " (" + shortname + ")");
            return view;
        }

        @Override
        public int getCount ()
        {
            return cur.getCount ();
        }
    }

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_sync);

        currentDownload = (TextView) findViewById (R.id.current_download);
        currentCount = (TextView) findViewById (R.id.current_count);
        progressBar = (ProgressBar) findViewById (R.id.progress_bar);

        syncResult = (TextView) findViewById (R.id.sync_result);
        continueButton = (Button) findViewById (R.id.continue_button);

        classSelect = (Spinner) findViewById (R.id.class_select);

        db = DbUtils.getReadableDatabase ();

        if (!PrefUtils.hasSyncedTimeTables (this))
        {
            initSync ();
        }
        else
        {
            progressBar.setProgress (progressBar.getMax ());
            currentCount.setText (progressBar.getMax () + "/" + progressBar.getMax ());

            syncResult.setVisibility (View.VISIBLE);
            classSelect.setVisibility (View.VISIBLE);
        }
    }

    public void initSync ()
    {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo ();

        if (networkInfo != null && networkInfo.isConnected ())
        {
            urls = new ArrayList<> ();

            currentDownload.setText (getString (R.string.downloading_metadata));
            currentCount.setText ("");

            progressBar.setVisibility (View.VISIBLE);
            progressBar.setIndeterminate (true);

            syncResult.setVisibility (View.INVISIBLE);
            continueButton.setVisibility (View.INVISIBLE);
            classSelect.setVisibility (View.INVISIBLE);

            DbUtils.resetTables ();

            Handler syncActivityHandler = new Handler ();
            Runnable runnable = new MasterlistDownloadRunnable (urls, this, syncActivityHandler);

            new Thread (runnable).start ();
        }
        else
        {
            progressBar.setVisibility (View.INVISIBLE);

            syncResult.setVisibility (View.VISIBLE);
            syncResult.setText (getString (R.string.no_internet));

            continueButton.setVisibility (View.VISIBLE);
            continueButton.setText (getString (R.string.button_retry));
        }
    }

    public void beginSync (List<String> urls)
    {
        progressBar.setIndeterminate (false);
        progressBar.setMax (urls.size ());
        currentCount.setText ("0/" + urls.size ());
        manager.setTimeTableCount (urls.size ());

        manager = ThreadManager.getInstance ();

        manager.setContext (this);

        for (String url : urls)
        {
            manager.parseTimeTable (url);
        }
    }

    public void finishSync ()
    {
        switch (manager.getResult ())
        {
            case ThreadManager.DOWNLOAD_FAILED:
                syncResult.setText (R.string.sync_result_failure);
                PrefUtils.setTimeTablesSynced (this, false);

                break;
            case ThreadManager.TASK_COMPLETED:
                syncResult.setText (R.string.sync_result_success);
                PrefUtils.setTimeTablesSynced (this, true);

                getSupportActionBar ().setDisplayHomeAsUpEnabled (true);
                break;
        }

        syncResult.setVisibility (View.VISIBLE);
        continueButton.setVisibility (View.VISIBLE);

        classSelect.setVisibility (View.VISIBLE);

        ClassSelectAdapter adapter = new ClassSelectAdapter (this, R.layout.sync_spinner_item);

        classSelect.setAdapter (adapter);
        classSelect.setOnItemSelectedListener (this);

        manager.resetManager ();
    }

    @Override
    protected void onStop ()
    {
        super.onStop ();

        if (!PrefUtils.hasSyncedTimeTables (this)) ThreadManager.cancelAll ();
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater ().inflate (R.menu.menu_sync, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId ();

        switch (id)
        {
            case R.id.home:
                NavUtils.navigateUpFromSameTask (this);
                finish ();
                return true;
        }

        return super.onOptionsItemSelected (item);
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig)
    {
        super.onConfigurationChanged (newConfig);
    }

    public void onContinueClick (View view)
    {
        if (PrefUtils.hasSyncedTimeTables (this))
        {
            Intent intent = new Intent (this, MainActivity.class);
            startActivity (intent);
            finish ();
        }
        else
        {
            initSync ();
        }
    }

    @Override
    public void onItemSelected (AdapterView<?> parent, View view, int position, long id)
    {
        Cursor cur = db.rawQuery ("SELECT * FROM " + ClassTable.TABLE_NAME + " ORDER BY " + ClassTable.COLUMN_NAME_NAME_LONG + " ASC", null);
        cur.moveToPosition (position);
        String shortname = cur.getString (cur.getColumnIndexOrThrow (ClassTable.COLUMN_NAME_NAME_SHORT));

        PrefUtils.setTableName (this, shortname);
    }

    @Override
    public void onNothingSelected (AdapterView<?> parent)
    {

    }

    public Context getActivity ()
    {
        return this;
    }
}
