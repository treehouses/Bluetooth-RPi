package io.treehouses.remote;

import android.os.Build;
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

    public void buttonProperties(Boolean clickable, int color) {
        NetworkFragment.getInstance().setButtonConfiguration(this);
        btnStartConfiguration.setClickable(clickable);
        btnStartConfiguration.setTextColor(color);
    }

    protected void buttonWifiSearch(Context context) {
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Toast.makeText(context, "Wifi scan requires at least android API 23", Toast.LENGTH_LONG).show();
        } else {
            btnWifiSearch.setOnClickListener(v1 -> NetworkFragment.getInstance().showWifiDialog(v1));
        }
    }

    public static TextInputEditText getSSID() {
        return etSsid;
    }
}
