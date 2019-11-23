package io.treehouses.remote;

import android.app.Application;
import java.util.ArrayList;

import io.treehouses.remote.utils.SaveUtils;

public class MainApplication extends Application {

    private static ArrayList terminalList, tunnelList, commandList;

    @Override
    public void onCreate() {
        super.onCreate();
        terminalList = new ArrayList();
        tunnelList = new ArrayList();
        commandList = new ArrayList();
        SaveUtils.initCommandsList(getApplicationContext());
    }

    public static ArrayList getTerminalList() {
        return terminalList;
    }

    public static ArrayList getTunnelList() {
        return tunnelList;
    }

    public static ArrayList getCommandList() {
        return commandList;
    }
}
