package io.treehouses.remote

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Message
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import io.treehouses.remote.fragments.*
import io.treehouses.remote.network.BluetoothChatService
import io.treehouses.remote.bases.PermissionActivity
import io.treehouses.remote.callback.HomeInteractListener
import io.treehouses.remote.callback.NotificationCallback
import io.treehouses.remote.databinding.ActivityInitial2Binding
import io.treehouses.remote.ui.system.SystemFragment
import io.treehouses.remote.ui.home.HomeFragment
import io.treehouses.remote.ui.network.NetworkFragment
import io.treehouses.remote.ui.sshconfig.SSHConfigFragment
import io.treehouses.remote.ui.services.ServicesFragment
import io.treehouses.remote.ui.status.StatusFragment
import io.treehouses.remote.utils.LogUtils
import io.treehouses.remote.utils.SettingsUtils
import io.treehouses.remote.utils.logD

open class BaseInitialActivity: PermissionActivity(), NavigationView.OnNavigationItemSelectedListener, HomeInteractListener, NotificationCallback {
    protected var validBluetoothConnection = false
    protected var mConnectedDeviceName: String? = null
    protected lateinit var bind: ActivityInitial2Binding
    protected lateinit var mActionBarDrawerToggle: ActionBarDrawerToggle
    protected var preferences: SharedPreferences? = null

    /** Defines callbacks for service binding, passed to bindService()  */

    protected lateinit var currentTitle: String

    override fun setChatService(service: BluetoothChatService) {
        mChatService = service
        mChatService.updateHandler(mHandler)
        checkStatusNow()
    }

    override fun getChatService(): BluetoothChatService {
        return mChatService
    }

    /**
     * Sends a message.
     *
     * @param s A string of text to send.
     */


    override fun sendMessage(s: String) {
        // Check that we're actually connected before trying anything
        logD(s)
        if (mChatService.state != Constants.STATE_CONNECTED) {
            Toast.makeText(this@BaseInitialActivity, R.string.not_connected, Toast.LENGTH_SHORT).show()
            LogUtils.mIdle()
            return
        }

        // Check that there's actually something to send
        if (s.isNotEmpty()) {
            // Get the message bytes and tell the BluetoothChatService to write
            val send = s.toByteArray()
            mChatService.write(send)

            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
        }
    }

    override fun redirectHome() {
        val menu = bind.navView.menu.findItem(R.id.menu_home)
        onNavigationItemSelected(menu)
        bind.navView.setCheckedItem(menu)
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
//            FragmentActivity activity = getActivity();
            //InitialActivity activity = InitialActivity.this;
            when (msg.what) {
                Constants.MESSAGE_DEVICE_NAME -> {
                    // save the connected device's name
                    mConnectedDeviceName = msg.data.getString(Constants.DEVICE_NAME)
                    if (mConnectedDeviceName != "" || mConnectedDeviceName != null) {
                        logD("DEVICE$mConnectedDeviceName")
                        checkStatusNow()
                    }
                }
            }
        }
    }

    fun checkStatusNow() {
        validBluetoothConnection = when (mChatService.state) {
            Constants.STATE_CONNECTED -> {
                LogUtils.mConnect()
                true
            }
            Constants.STATE_NONE -> {
                LogUtils.mOffline()
                false
            }
            else -> {
                LogUtils.mIdle()
                false
            }
        }
        logD("BOOLEAN $validBluetoothConnection")
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId
        checkStatusNow()
        if (validBluetoothConnection) onNavigationItemClicked(id)
        else {
            when (id) {
                R.id.menu_about -> openCallFragment(AboutFragment())
                R.id.menu_home -> openCallFragment(HomeFragment())
                R.id.menu_ssh -> openCallFragment(SSHConfigFragment())
            }
        }
        title = item.title; currentTitle = item.title.toString()
        bind.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun onNavigationItemClicked(id: Int) {
        val fragment = when (id) {
            R.id.menu_home -> HomeFragment()
            R.id.menu_network -> NetworkFragment()
            R.id.menu_system -> SystemFragment()
            R.id.menu_terminal -> TerminalFragment()
            R.id.menu_services -> ServicesFragment()
            R.id.menu_about -> AboutFragment()
            R.id.menu_status -> StatusFragment()
            R.id.menu_tunnel2 -> TunnelFragment()
            R.id.menu_ssh -> SSHConfigFragment()
            else -> HomeFragment()
        }

        openCallFragment(fragment)
    }

    override fun openCallFragment(f: Fragment) {
        val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        SettingsUtils.openFragment(true, fragmentTransaction, f)
        //        menuItem.setChecked(true);
//        title = "Treehouses Remote"
        //        drawer.closeDrawers();
    }

    //
    override fun setNotification(notificationStatus: Boolean) {
        if (notificationStatus) bind.navView.menu.getItem(6).setIcon(R.drawable.status_notification) else bind.navView.menu.getItem(6).setIcon(R.drawable.status)
    }

    fun hasValidConnection() : Boolean {
        return validBluetoothConnection
    }

    companion object {
        @JvmStatic
        var instance: BaseInitialActivity? = null
        lateinit var mChatService: BluetoothChatService
    }
}