package edu.uiowa.tsz.drivingapp;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Date;
import java.util.Locale;

public class RecordListFragment extends Fragment {

    private OnRecordSelectedListener listener;
    private RecordCursorAdapter adapter;

    public interface OnRecordSelectedListener {
        public void onRecordSelected(Record record);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnRecordSelectedListener) {
            listener = (OnRecordSelectedListener)activity;
        } else {
            throw new ClassCastException(String.format(Locale.US, "%s must implement RecordListFragment.OnRecordSelectedListener!", activity.toString()));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get all Record data as a cursor and bind it to the listview
        Cursor cursor = DatabaseManager.getAllRecords(getActivity());
        this.adapter = new RecordCursorAdapter(this.getActivity(), R.layout.fragment_record_row, cursor, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate View
        View view = inflater.inflate(R.layout.fragment_record_list, container, false);

        // Bind adapter to listview and define listener to fire when a list item is touched
        ListView recordListView = (ListView)view.findViewById(R.id.record_list_view);
        recordListView.setAdapter(adapter);
        recordListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowID) {
                // Build Record at _ID == 'position' and pass it to the Detail view.
                Cursor c = (Cursor)adapter.getItem(position);
                Record r = Record.buildRecordFromCursor(c);
                listener.onRecordSelected(r);
            }
        });
        return view;
    }


}
