package io.treehouses.remote.adapter;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import io.treehouses.remote.R;
import io.treehouses.remote.callback.HomeInteractListener;

public class ViewHolderWifi {
    public TextInputEditText etSsid, etPassword;

    public ViewHolderWifi(View v, final HomeInteractListener listener, final Context context) {
        etSsid = v.findViewById(R.id.et_ssid);
        etPassword = v.findViewById(R.id.et_password);
        v.findViewById(R.id.btn_start_config).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ssid = etSsid.getText().toString();
                String password = etPassword.getText().toString();
                listener.sendMessage(String.format("treehouses wifi \"%s\" \"%s\"", ssid, password));
                Toast.makeText(context, "Connecting...", Toast.LENGTH_LONG).show();
            }
        });
    }
}
