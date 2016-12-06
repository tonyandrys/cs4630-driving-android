package edu.uiowa.tsz.pedometer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

public class StartLoggingDialogFragment extends DialogFragment {

    public static String TAG = "StartLoggingDialogFragment";

    private String commentField = "";
    private String nameField = "";

    // implement to grab onClick events
    public interface StartLoggingDialogListener {
        void onStartLoggingDialogPositiveClick(DialogFragment dialog);
        void onStartLoggingDialogNegativeClick(DialogFragment dialog);
    }

    private StartLoggingDialogListener listener;
    private View view;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity a;

        if (context instanceof Activity) {
            a = (Activity) context;

            try {
                // instantiate listener to send events to the host
                listener = (StartLoggingDialogListener) a;
            } catch (ClassCastException e) {
                throw new ClassCastException(a.toString() + "does not implement StartLoggingDialogListener!");
            }
        } else {
            Log.e(TAG, "onAttach is called with a non activity argument");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // inflate and set layout for the dialog
        // IMPORTANT: use view to access textviews. Not getActivity()!
        this.view = inflater.inflate(R.layout.capture_modal, null);
        builder.setView(view)
        .setPositiveButton(R.string.start_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                TextView commentTextView = (TextView)view.findViewById(R.id.modal_comment_textview);
                TextView nameTextView = (TextView)view.findViewById(R.id.modal_filename_textview);
                commentField = commentTextView.getText().toString();
                nameField = nameTextView.getText().toString();
                Log.v(TAG, "commentField value is " + commentField + ", nameField is " + nameField);
                if (listener != null) {
                    listener.onStartLoggingDialogPositiveClick(StartLoggingDialogFragment.this);
                } else {
                    Log.e(TAG, "listener is null and it shouldn't be.");
                }

            };
        })

        .setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listener.onStartLoggingDialogNegativeClick(StartLoggingDialogFragment.this);
            }
        });

        return builder.create();
    };

    @Override
    public void onDismiss(DialogInterface dialog) {
        // Extract comment and name text from views and store them in the instance. Can be retrieved using getComment() and getName() public methods.

    }

    public String getComment() {
        return this.commentField;
    }

    public String getName() {
        return this.nameField;
    }

}
