//package io.treehouses.remote;
//
//import android.Manifest;
//import android.app.AlertDialog;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.graphics.drawable.Drawable;
//import android.graphics.drawable.Icon;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.util.Log;
//import android.view.View;
//import android.widget.Toast;
//import com.mikepenz.materialdrawer.AccountHeader;
//import com.mikepenz.materialdrawer.AccountHeaderBuilder;
//import com.mikepenz.materialdrawer.Drawer;
//import com.mikepenz.materialdrawer.DrawerBuilder;
//import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
//import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
//import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
//import com.mikepenz.materialdrawer.model.interfaces.Nameable;
//import java.util.ArrayList;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.Toolbar;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.fragment.app.FragmentTransaction;
//import io.treehouses.remote.Fragments.HomeFragment;
//import io.treehouses.remote.Fragments.NetworkFragment;
//import io.treehouses.remote.Fragments.ServicesFragment;
//import io.treehouses.remote.Fragments.StatusFragment;
//import io.treehouses.remote.Fragments.SystemFragment;
//import io.treehouses.remote.Fragments.TerminalFragment;
//import io.treehouses.remote.MiscOld.Constants;
//import io.treehouses.remote.Network.BluetoothChatService;
//
//public class InitialActivity1 extends AppCompatActivity {
//
//    private Boolean validBluetoothConnection = false;
////    private Boolean validWifiConnection = false;
//    private Toolbar mTopToolbar;
//    AccountHeader headerResult;
//    private Drawer result = null;
//    int REQUEST_COARSE_LOCATION = 99;
//    private static BluetoothChatService mChatService;
////    ProfileDrawerItem[] profileDrawerItem;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_initial);
//
//        checkLocationPermission();
//
//        mChatService = new BluetoothChatService(mHandler);
//        checkStatusNow();
//
//        mTopToolbar = findViewById(R.id.my_toolbar);
//        setSupportActionBar(mTopToolbar);
//        mTopToolbar.setTitleTextColor(Color.WHITE);
//        mTopToolbar.setSubtitleTextColor(Color.WHITE);
//        headerResult = getAccountHeader();
//        createDrawer();
//        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
//        getSupportActionBar().setTitle(R.string.app_project_name);
//        openCallFragment(new HomeFragment());
//
////        if(mChatService == null){
////            showRPIDialog();
////        }else{
////        Log.e("TERMINAL mChatService", ""+mChatService.getState());
////        mOutStringBuffer = new StringBuffer("");
//
////        }
////        Log.e("DEVICE ", ""+device.getName());
////         If BT is not on, request that it be enabled.
////         setupChat() will then be called during onActivityResult
////        if (!mBluetoothAdapter.isEnabled()) {
////            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
////            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
////            // Otherwise, setup the chat session
////        }
////        else {
////            setupChat();
//////            mChatService.connect(device,true);
////        }
//    }
//
////    private String name = null;
//
//    private AccountHeader getAccountHeader() {
////        String val = "";
////        if(validBluetoothConnection){val = "Raspberry Pi 3B+"; }
////        else{ val = "Not Connected"; }
////        Icon icon = new Icon(android.R.drawable.treehouses2);
//        //Create User profile header
//        return new AccountHeaderBuilder()
//                .withActivity(InitialActivity.this)
//                .withTextColor(getResources().getColor(R.color.md_black_1000))
//                .withHeaderBackground(R.color.md_white_1000)
//                .withCloseDrawerOnProfileListClick(false)
//                .withSelectionListEnabled(false)
//                .addProfiles(
////                        new ProfileDrawerItem().withIcon(R.drawable.circle),
//                        new ProfileDrawerItem().withName("Treehouses").withEmail("Remote").withIcon(R.drawable.treehouses2).withIdentifier(0)
//                )
//                .withCompactStyle(true)
//                .withDividerBelowHeader(true)
//                .build();
//
//    }
//
//    private void createDrawer() {
//        com.mikepenz.materialdrawer.holder.DimenHolder dimenHolder = com.mikepenz.materialdrawer.holder.DimenHolder.fromDp(110);
//        result = new DrawerBuilder()
//                .withActivity(this)
//                .withFullscreen(true)
//                .withSliderBackgroundColor(getResources().getColor(R.color.colorPrimary))
//                .withToolbar(mTopToolbar)
//                .withAccountHeader(headerResult)
//                .withHeaderHeight(dimenHolder)
//                .addDrawerItems(getDrawerItems())
//                .withDrawerWidthDp(R.dimen.drawer_width)
//                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
//                    @Override
//                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
//                        if (drawerItem != null) {
//                            if (drawerItem instanceof Nameable) {
//                                menuAction(((Nameable) drawerItem).getName().getTextRes());
//                            }
//                        }
//                        return false;
//                    }
//                })
//                .withDrawerWidthDp(300)
//                .build();
//    }
//    private void menuAction(int selectedMenuId) {
//        checkStatusNow();
//        switch (selectedMenuId) {
//            case R.string.menu_network:
//                if(validBluetoothConnection){ openCallFragment((new NetworkFragment())); }
//                else{showAlertDialog();}
//                break;
//            case R.string.menu_home:
//                openCallFragment(new HomeFragment());
//                break;
//            case R.string.menu_services:
//                if(validBluetoothConnection){openCallFragment(new ServicesFragment()); }
//                else{showAlertDialog(); }
//                break;
//            case R.string.menu_system:
//                if(validBluetoothConnection){ openCallFragment(new SystemFragment()); }
//                else{ showAlertDialog(); }
//                break;
//            case R.string.menu_terminal:
//                if(validBluetoothConnection){ openCallFragment(new TerminalFragment());}
//                else{ showAlertDialog(); }
//                break;
//            case R.string.menu_status:
//                if(validBluetoothConnection){ openCallFragment(new StatusFragment()); }
//                else{ showAlertDialog(); }
//                break;
////            case R.string.menu_courses:
////                openCallFragment(new MyCourseFragment());
////                break;
////            case R.string.menu_feedback:
////                feedbackDialog();
////            case R.string.menu_logout:
////                logout();
////                break;
//            default:
//                openCallFragment(new HomeFragment());
//                break;
//        }
//    }
//    private IDrawerItem[] getDrawerItems() {
//        ArrayList<Drawable> menuImageList = new ArrayList<>();
//        menuImageList.add(getResources().getDrawable(R.drawable.home));
//        menuImageList.add(getResources().getDrawable(R.drawable.network));
//        menuImageList.add(getResources().getDrawable(R.drawable.system));
//        menuImageList.add(getResources().getDrawable(R.drawable.terminal));
//        menuImageList.add(getResources().getDrawable(R.drawable.circle));
//        menuImageList.add(getResources().getDrawable(R.drawable.ssh));
//        menuImageList.add(getResources().getDrawable(R.drawable.about));
//        menuImageList.add(getResources().getDrawable(R.drawable.about));
//
//        return new IDrawerItem[]{
//                changeUX(R.string.menu_home, menuImageList.get(0)).withIdentifier(0),
//                changeUX(R.string.menu_network, menuImageList.get(1)).withIdentifier(1),
//                changeUX(R.string.menu_system, menuImageList.get(2)).withIdentifier(2),
//                changeUX(R.string.menu_terminal, menuImageList.get(3)).withIdentifier(3),
//                changeUX(R.string.menu_services, menuImageList.get(4)).withIdentifier(4),
//                changeUX(R.string.menu_ssh, menuImageList.get(5)).withIdentifier(5),
//                changeUX(R.string.menu_about, menuImageList.get(6)).withIdentifier(6),
//                changeUX(R.string.menu_status, menuImageList.get(7)).withIdentifier(7),
//        };
//    }
//
//    public PrimaryDrawerItem changeUX(int iconText, Drawable drawable) {
//        return new PrimaryDrawerItem().withName(iconText)
//                .withIcon(drawable).withTextColor(getResources().getColor(R.color.textColorPrimary))
//                .withIconColor(getResources().getColor(R.color.textColorPrimary))
//                .withSelectedIconColor(getResources().getColor(R.color.primary_dark))
//                .withIconTintingEnabled(true);
//    }
//
//    @Override
//    public void onBackPressed() {
//        //handle the back press :D close the drawer first and if the drawer is closed close the activity
//        if (result != null && result.isDrawerOpen()) {
//            result.closeDrawer();
//        } else {
//            super.onBackPressed();
//        }
//    }
//
//    public void openCallFragment(androidx.fragment.app.Fragment newfragment) {
//        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
//        fragmentTransaction.replace(R.id.fragment_container, newfragment);
//        fragmentTransaction.addToBackStack("");
//        fragmentTransaction.commit();
//    }
//    protected void checkLocationPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                    REQUEST_COARSE_LOCATION);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case 99: {
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                } else {
//                    //TODO re-request
//                }
//                break;
//            }
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        mChatService.updateHandler(mHandler);
//    }
//
//    /**
// *
// *
// *  START OF COMMON BLUETOOTH COMMUNICATION FEATURES
// *
// * **/
//
//    /**
//     * Name of the connected device
//     */
//    private String mConnectedDeviceName = null;
//
//    /**
//     * String buffer for outgoing messages
//     */
////    private StringBuffer mOutStringBuffer;
//
//    public void setChatService(BluetoothChatService chatService){
//        mChatService = chatService;
//        mChatService.updateHandler(mHandler);
//        checkStatusNow();
//    }
//    public BluetoothChatService getChatService(){return mChatService; }
//
//
//    private void checkStatusNow(){
//        if(mChatService.getState() == Constants.STATE_CONNECTED){
//            mConnect();
//            validBluetoothConnection = true;
//        }else if(mChatService.getState() == Constants.STATE_NONE){
//            mOffline();
//            validBluetoothConnection = false;
//        }else{
//            mIdle();
//            validBluetoothConnection = false;
//        }
//        //start pinging for wifi check
////        final Handler h = new Handler();
////        final int delay = 20000;
////        h.postDelayed(new Runnable(){
////            public void run(){
////                String ping = "ping -c 1 google.com";
////                sendPing(ping);
////                //remove the space at the very end of the readMessage -> eliminate space between items
////                String readMessage = new String(mOutStringBuffer);
////                readMessage = readMessage.substring(0,readMessage.length()-1);
////                //check if ping was successful
////                if(readMessage.contains("1 packets")){validWifiConnection = true;}
////                if(readMessage.contains("Unreachable") || readMessage.contains("failure")){validWifiConnection = false;}
////                h.postDelayed(this, delay);
////            }
////        }, delay);
//
//    }
////    /**
////     * This block is to create a dialog box for creating a new name or changing the password for the PI device
////     * Sets the dialog button to be disabled if no text is in the EditText
////     */
////    private void showDialog(View view) {
////        final EditText input = new EditText(InitialActivity.this);
////        final AlertDialog alertDialog = showAlertDialog(
////                "Rename Hostname",
////                "Please enter new hostname",
////                "treehouses rename ", input);
////
////        alertDialog.getButton(alertDialog.BUTTON_POSITIVE).setClickable(false);
////        alertDialog.getButton(alertDialog.BUTTON_POSITIVE).setEnabled(false);
////
////        input.addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
////            }
////            @Override
////            public void onTextChanged(CharSequence s, int start, int before, int count) {
////                if(s.length() > 0) {
////                    alertDialog.getButton(alertDialog.BUTTON_POSITIVE).setClickable(true);
////                    alertDialog.getButton(alertDialog.BUTTON_POSITIVE).setEnabled(true);
////                }else{
////                    alertDialog.getButton(alertDialog.BUTTON_POSITIVE).setClickable(false);
////                    alertDialog.getButton(alertDialog.BUTTON_POSITIVE).setEnabled(false);
////                }
////            }
////            @Override
////            public void afterTextChanged(Editable s) {
////            }
////        });
////    }
////
//
////
////    /**
////     * Makes this device discoverable for 300 seconds (5 minutes).
////     */
////    private void ensureDiscoverable() {
////        if (mBluetoothAdapter.getScanMode() !=
////                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
////            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
////            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
////            startActivity(discoverableIntent);
////        }
////    }
//
//
//
////
////    public void sendPing(String ping) {
////        // Get the message bytes and tell the BluetoothChatService to write
////        byte[] pSend = ping.getBytes();
////        mChatService.write(pSend);
//////        mOutStringBuffer.setLength(0);
////    }
//
//
//
////    private void setStatus(Object arg) {
////        FragmentActivity activity = getActivity();
////        if (null == activity) {
////            return;
////        }
////        final ActionBar actionBar = activity.getActionBar();
////        if (null == actionBar) {
////            return;
////        }
////        if(arg instanceof Integer){
////            Log.d(TAG, "actionBar.setSubtitle(resId) = " + arg);
////            currentStatus = getString((Integer) arg);
////            actionBar.setSubtitle((Integer) arg);
////        } else if(arg instanceof CharSequence){
////            Log.d(TAG, "actionBar.setSubtitle(subTitle) = " + arg);
////            currentStatus = arg.toString();
////            actionBar.setSubtitle((CharSequence) arg);
////        }
////    }
//
//    /**
//     * The Handler that gets information back from the BluetoothChatService
//     */
//    public final Handler mHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
////            FragmentActivity activity = getActivity();
//            InitialActivity activity = InitialActivity.this;
//            switch (msg.what) {
//                case Constants.MESSAGE_STATE_CHANGE:
//                    switch (msg.arg1) {
//                        case Constants.STATE_LISTEN:
//                        case Constants.STATE_NONE:
////                            setStatus(R.string.title_not_connected);
//                            mIdle();
//                            break;
//                    }
//                    break;
////                case Constants.MESSAGE_WRITE:
////                    isRead = false;
////                    byte[] writeBuf = (byte[]) msg.obj;
////                    // construct a string from the buffer
////                    String writeMessage = new String(writeBuf);
////                    if(!writeMessage.contains("google.com")) {
////                        Log.d(TAG, "writeMessage = " + writeMessage);
////                        mConversationArrayAdapter.add("Command:  " + writeMessage);
////                    }
////                    break;
////                case Constants.MESSAGE_READ:
////                    isRead = true;
//////                    byte[] readBuf = (byte[]) msg.obj;
//////                     construct a string from the valid bytes in the buffer
//////                    String readMessage = new String(readBuf, 0, msg.arg1);
//////                    String readMessage = new String(readBuf);
////                    String readMessage = (String)msg.obj;
////                    Log.d(TAG, "readMessage = " + readMessage);
////                    //TODO: if message is json -> callback from RPi
////                    if(isJson(readMessage)){
////                        //handleCallback(readMessage);
////                    }else{
////                        if(isCountdown){
////                            //mHandler.removeCallbacks(watchDogTimeOut);
////                            isCountdown = false;
////                        }
////                        //remove the space at the very end of the readMessage -> eliminate space between items
////                        readMessage = readMessage.substring(0,readMessage.length()-1);
////                        //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
////
////                        //check if ping was successful
////                        if(readMessage.contains("1 packets")){
////                            mConnect();
////                        }
////                        if(readMessage.contains("Unreachable") || readMessage.contains("failure")){
////                            mOffline();
////                        }
////                        //make it so text doesn't show on chat (need a better way to check multiple strings since mConversationArrayAdapter only takes messages line by line)
////                        if (!readMessage.contains("1 packets") && !readMessage.contains("64 bytes") && !readMessage.contains("google.com") &&
////                                !readMessage.contains("rtt") && !readMessage.trim().isEmpty()){
////                            mConversationArrayAdapter.add(readMessage);
////                        }
////                    }
////                    break;
//                case Constants.MESSAGE_DEVICE_NAME:
//                    // save the connected device's name
//                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
//                    Log.e("DEVICE",""+mConnectedDeviceName);
////                    Toast.makeText(InitialActivity.this, "Connected to "+mConnectedDeviceName, Toast.LENGTH_LONG).show();
////                    if (null != activity) {
////                        Toast.makeText(activity, "Connected to "
////                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
////                    }
//                    break;
////                case Constants.MESSAGE_TOAST:
////                    if (null != activity) {
////                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
////                                Toast.LENGTH_SHORT).show();
////                    }
////                    break;
//            }
//        }
//    };
//
////
////    public boolean isJson(String str) {
////        try {
////            new JSONObject(str);
////        } catch (JSONException ex) {
////            return false;
////        }
////        return true;
////    }
////
//    public void mOffline(){
////        IProfile iProfile = new ProfileDrawerItem().withName("You are conected to:").withEmail("NOTHING").withIcon(R.drawable.wifiicon).withIdentifier(0);
////        headerResult.updateProfile(iProfile);
//        Log.e("STATUS","OFFLINE");
//    }
//
//    public void mIdle(){
////        IProfile iProfile = new ProfileDrawerItem().withName("You are conected to:").withEmail("NOTHING").withIcon(R.drawable.wifiicon).withIdentifier(0);
////        headerResult.updateProfile(iProfile);
//        Log.e("STATUS","IDLE");
//    }
//
//    public void mConnect(){
////        IProfile iProfile = new ProfileDrawerItem().withName("You are conected to:").withEmail(mConnectedDeviceName).withIcon(R.drawable.wifiicon).withIdentifier(0);
////        headerResult.updateProfile(iProfile);
//        Log.e("STATUS","CONNECTED");
//    }
//}
