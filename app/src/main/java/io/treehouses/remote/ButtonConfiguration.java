package io.treehouses.remote;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.content.Context;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import io.treehouses.remote.Fragments.NetworkFragment;
import io.treehouses.remote.Fragments.NetworkFragment;

public abstract class ButtonConfiguration {
    protected Button btnStartConfiguration;
    protected Button btnWifiSearch;
    protected static TextInputEditText etSsid;

    public void buttonProperties(Boolean clickable, int color, View v) {
        Button btnStartConfiguration = v.findViewById(R.id.btn_start_config);
        NetworkFragment.getInstance().setButtonConfiguration(this);
        btnStartConfiguration.setClickable(clickable);
        btnStartConfiguration.setTextColor(color);
    }

    protected void buttonWifiSearch(Context context) {

        btnWifiSearch.setOnClickListener(v1 -> {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Toast.makeText(context, "Wifi scan requires at least android API 23", Toast.LENGTH_LONG).show();
            } else {
                NetworkFragment.getInstance().showWifiDialog(v1);
            }
        });
    }

    public static TextInputEditText getSSID() {
        return etSsid;
    }
}
