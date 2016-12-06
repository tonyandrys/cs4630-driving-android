package edu.uiowa.tsz.drivingapp;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

/**
 * Binds data from cursor projection (sqlite) to the UI elements in a listview.
 */
public class RecordCursorAdapter extends ResourceCursorAdapter {

    public RecordCursorAdapter(Context context, int layout, Cursor cursor, int flags) {
        super(context, layout, cursor, flags);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView recordRowIDTextView = (TextView)view.findViewById(R.id.record_row_id_textview);
        TextView recordRowNameTextView = (TextView)view.findViewById(R.id.record_row_name_textview);
        TextView recordRowElapsedTextView = (TextView)view.findViewById(R.id.record_row_elapsed_textview);

        // Extract ID, name, and time info and format it appropriately
        long startTime = cursor.getLong(cursor.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_START_TIME));
        long endTime = cursor.getLong(cursor.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_END_TIME));
        long elapsedMinutes = (((endTime-startTime)/1000)/60);
        String elapsedString = String.format(Locale.US, "%d min", elapsedMinutes);
        String id = String.format(Locale.US, "#%s", cursor.getString(cursor.getColumnIndex(RecordContract.RecordEntry._ID)));
        String logName = String.format(Locale.US, "%s", cursor.getString(cursor.getColumnIndex(RecordContract.RecordEntry.COLUMN_NAME_LOG_NAME)));

        // Apply to UI elements
        recordRowIDTextView.setText(id);
        recordRowNameTextView.setText(logName);
        recordRowElapsedTextView.setText(elapsedString);
    }
}
