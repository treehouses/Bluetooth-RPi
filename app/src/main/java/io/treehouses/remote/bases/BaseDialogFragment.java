package io.treehouses.remote.bases;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import io.treehouses.remote.callback.HomeInteractListener;

public class BaseDialogFragment extends DialogFragment {
    public HomeInteractListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeInteractListener)
            listener = (HomeInteractListener) context;
    }
}
