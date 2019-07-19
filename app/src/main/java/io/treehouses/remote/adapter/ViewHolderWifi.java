package io.treehouses.remote.adapter;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.content.Context;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import io.treehouses.remote.ButtonConfiguration;
import io.treehouses.remote.Fragments.CustomTextWatcher;
import io.treehouses.remote.R;
import io.treehouses.remote.callback.HomeInteractListener;

class ViewHolderWifi extends ButtonConfiguration {

    private static TextInputEditText etPassword;

    ViewHolderWifi(View v, final HomeInteractListener listener, final Context context) {
        etSsid = v.findViewById(R.id.et_ssid);
        etPassword = v.findViewById(R.id.et_password);
        btnStartConfiguration = v.findViewById(R.id.btn_start_config);
        btnWifiSearch = v.findViewById(R.id.btnWifiSearch);


        etSsid.addTextChangedListener(getTextWatcher(etSsid, v));
        etPassword.addTextChangedListener(getTextWatcher(etPassword, v));
        buttonProperties(false, Color.LTGRAY, btnStartConfiguration);

        buttonWifiSearch(context);

        btnStartConfiguration.setOnClickListener(view -> {
            String ssid = etSsid.getText().toString();
            String password = etPassword.getText().toString();
            listener.sendMessage(String.format("treehouses wifi \"%s\" \"%s\"", ssid, password));

            buttonProperties(false, Color.LTGRAY, btnStartConfiguration);

            Toast.makeText(context, "Connecting...", Toast.LENGTH_LONG).show();

        });
    }
}
