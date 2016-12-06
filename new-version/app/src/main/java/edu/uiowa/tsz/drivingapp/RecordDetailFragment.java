package edu.uiowa.tsz.drivingapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment that displays when a Record listview row is touched by the user.
 * It doesn't really do anything useful except provide the "Email" button in the ActionBar (which opens an email intent w/ all associated logfiles).
 * It could use some work.
 */
public class RecordDetailFragment extends Fragment {

    private Record record;

    /**
     * Constructs a new RecordDetailFragment loaded with the Record object passed as an argument to be displayed.
     * @param record Record to be displayed
     * @return RecordDetailFragment
     */
    public static RecordDetailFragment newInstance(Record record) {
        RecordDetailFragment fragment = new RecordDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(Record.INTENT_EXTRA_RECORD, record);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.record = (Record)getArguments().getSerializable(Record.INTENT_EXTRA_RECORD);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record_detail, container, false);
        // populate textViews with information from Record.
        TextView recordTitleTextView = (TextView)view.findViewById(R.id.record_title_textView);
        TextView recordBodyTextView = (TextView)view.findViewById(R.id.record_body_textView);
        recordTitleTextView.setText(this.record.getRecordName());
        recordBodyTextView.setText(Long.toString(this.record.getStartTime().getTime()));
        return view;
    }
}
