package com.geograppy.geopost.classes;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.InputFilter;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.geograppy.geopost.R;

import org.json.JSONException;

/**
 * Created by benito on 22/07/15.
 */
public class HelpDialog extends Dialog implements View.OnClickListener {

    private Button thanks;
    private Context c;

    public HelpDialog(Context context) {
        super(context);
        c = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.help_dialog);

        thanks = (Button) findViewById(R.id.btn_thanks);
        thanks.setOnClickListener(this);
        TextView text = (TextView) findViewById(R.id.help_dialog_text);
        text.setText(Html.fromHtml(c.getResources().getString(R.string.help_text)));
        text.setMovementMethod(new ScrollingMovementMethod());
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_thanks:
                dismiss();
                break;

            default:
                break;
        }
        dismiss();
    }
}
