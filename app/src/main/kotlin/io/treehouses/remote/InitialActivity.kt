package io.treehouses.remote

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import io.treehouses.remote.Fragments.CommunityFragment
import io.treehouses.remote.Fragments.DialogFragments.FeedbackDialogFragment
import io.treehouses.remote.Fragments.DiscoverFragment
import io.treehouses.remote.Fragments.SettingsFragment
import io.treehouses.remote.callback.BackPressReceiver
import io.treehouses.remote.databinding.ActivityInitial2Binding
import io.treehouses.remote.ui.home.HomeFragment
import io.treehouses.remote.utils.GPSService


class InitialActivity : BaseInitialActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityInitial2Binding.inflate(layoutInflater)
        instance = this
        setContentView(bind.root)
        requestPermission()
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        currentTitle = "Home"
        setUpDrawer()
        title = "Home"
        GPSService(this)
        val a = (application as MainApplication).getCurrentBluetoothService()
        if (a != null) {
            mChatService = a
            mChatService.updateHandler(mHandler)
            openCallFragment(HomeFragment())
        }
        checkStatusNow()
        openCallFragment(HomeFragment())
    }


//    override fun onStart() {
//        super.onStart()
        // Bind to LocalService
//        if (!isBluetoothServiceRunning(BluetoothChatService::class.java)) {
//            Log.e("InitialActivity", "STARTING SERVICE")
//            Intent(this, BluetoothChatService::class.java).also { intent ->
//                bindService(intent, connection, Context.BIND_AUTO_CREATE)
//            }
//        }
//    }

//    override fun onDestroy() {
//        super.onDestroy()
//        try {
//            unbindService(connection)
//        } catch (e: IllegalArgumentException) {
//            e.printStackTrace()
//        }

//    }

//    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            Log.e("RECEIVED", "RECEIVE")
//            Toast.makeText(applicationContext, "received", Toast.LENGTH_SHORT).show()
//            val a = (application as MainApplication).getCurrentBluetoothService()
//            if (a != null ) {
//                setChatService(a)
//                openCallFragment(HomeFragment())
//            }
//        }
//    }

//    override fun onResume() {
//        val filter = IntentFilter()
//        filter.addAction(MainApplication.BLUETOOTH_SERVICE_CONNECTED)
//        applicationContext.registerReceiver(receiver, filter)
//        super.onResume()
//    }
//
//    override fun onPause() {
//        applicationContext.unregisterReceiver(receiver)
//        super.onPause()
//    }

    override fun onResume() {
        super.onResume()
        resetMenuIcon()
    }

    private fun setUpDrawer() {
        mActionBarDrawerToggle = object : ActionBarDrawerToggle(this, bind.drawerLayout, findViewById(R.id.toolbar), R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            override fun onDrawerOpened(drawerView: View) {
                (this@InitialActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(window.decorView.windowToken, 0)
            }
        }
        bind.drawerLayout.addDrawerListener(mActionBarDrawerToggle)
        mActionBarDrawerToggle.syncState()
        bind.navView.setNavigationItemSelectedListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onBackPressed() {
        if (bind.drawerLayout.isDrawerOpen(GravityCompat.START)) bind.drawerLayout.closeDrawer(GravityCompat.START)
        else {
            val f = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (f is HomeFragment) finishAffinity()
            else if (f is SettingsFragment || f is CommunityFragment || f is DiscoverFragment) {
                (supportFragmentManager).popBackStack()
                title = currentTitle
            }
            if (f is BackPressReceiver) f.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.initial, menu)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 99) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@InitialActivity, "Permissions Granted", Toast.LENGTH_SHORT).show()
            } //TODO re-request
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        checkStatusNow()
        for (x in 1 until bind.navView.menu.size() - 2) {
            val item = bind.navView.menu.getItem(x)
            item.isEnabled = validBluetoothConnection
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mChatService.updateHandler(mHandler)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                openCallFragment(SettingsFragment())
                title = getString(R.string.action_settings)
            }
            R.id.action_feedback -> {
                FeedbackDialogFragment().show(supportFragmentManager.beginTransaction(), "feedbackDialogFragment")
            }
            R.id.action_community -> {
                preferences = PreferenceManager.getDefaultSharedPreferences(this)
                val v = layoutInflater.inflate(R.layout.alert_log_map, null)
                if (!preferences?.getBoolean("send_log", false)!!) {
                    createAlertDialog(this@InitialActivity, R.style.CustomAlertDialogStyle, "Sharing is Caring.").setCancelable(false).setMessage("The community map is only available with data sharing. " +
                            "Please enable data sharing to access this feature.")
                            .setPositiveButton("Enable Data Sharing") { _: DialogInterface?, _: Int -> preferences!!.edit().putBoolean("send_log", true).apply() }.setNegativeButton("Cancel") { _: DialogInterface?, _: Int -> MainApplication.showLogDialog = false }.setView(v).show().window!!.setBackgroundDrawableResource(android.R.color.transparent)
                }
                else {
                    openCallFragment(CommunityFragment())
                    title = getString(R.string.action_community)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createAlertDialog(context: Context?, id:Int, title: String): AlertDialog.Builder {
        return AlertDialog.Builder(ContextThemeWrapper(context, id)).setTitle(title)
    }

    fun changeAppBar() {
        mActionBarDrawerToggle = ActionBarDrawerToggle(this, bind.drawerLayout, findViewById(R.id.toolbar), 0, 0)
        mActionBarDrawerToggle.toolbarNavigationClickListener = View.OnClickListener {
            //reset to burger icon
            supportFragmentManager.popBackStack()
            resetMenuIcon()
        }
        //add back button
        bind.drawerLayout.setDrawerListener(mActionBarDrawerToggle)
        mActionBarDrawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        mActionBarDrawerToggle.isDrawerIndicatorEnabled = false
        bind.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun resetMenuIcon() {
        mActionBarDrawerToggle.isDrawerIndicatorEnabled = true
        bind.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }
}