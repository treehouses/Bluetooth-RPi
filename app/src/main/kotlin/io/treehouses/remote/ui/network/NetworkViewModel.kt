package io.treehouses.remote.ui.network

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import io.treehouses.remote.Constants
import io.treehouses.remote.MainApplication
import io.treehouses.remote.R
import io.treehouses.remote.bases.FragmentViewModel
import io.treehouses.remote.pojo.NetworkProfile
import io.treehouses.remote.utils.*

class NetworkViewModel(application: Application) : FragmentViewModel(application) {
    private val context = getApplication<MainApplication>().applicationContext
    var networkMode: MutableLiveData<String> = MutableLiveData()
    var ipAddress: MutableLiveData<String> = MutableLiveData()
    var wifiUserError: MutableLiveData<Boolean> = MutableLiveData()
    var showHome: MutableLiveData<Boolean> = MutableLiveData()
    val downloadUpload: MutableLiveData<String> = MutableLiveData()
    var dialogCheck: MutableLiveData<Boolean> = MutableLiveData()
    var showNetworkProgress: MutableLiveData<Boolean> = MutableLiveData()
    var checkBoxChecked: MutableLiveData<Boolean> = MutableLiveData()

    private fun updateNetworkText(mode: String) {
        logD( "Current Network Mode: $mode" )
        networkMode.value = "Current Network Mode: $mode"
        showNetworkProgress.value = false
    }

    fun onLoad() {
        getNetworkMode()
        sendMessage(getString(R.string.TREEHOUSES_NETWORKMODE_INFO))
    }

    private fun showIpAddress(output: String) {
        var ip = output.substringAfter("ip: ").substringBefore(", has")
        logD( "Current ip: $ip" )
        if (ip == "") ip = "N/A"
        ipAddress.value = "IP Address: " + ip
    }

    fun rebootHelper() {
        try {
            sendMessage(getString(R.string.REBOOT))
            Thread.sleep(1000)
            if (mChatService.state != Constants.STATE_CONNECTED) {
                Toast.makeText(context, "Bluetooth Disconnected: Reboot in progress", Toast.LENGTH_LONG).show()
                showHome.value = true
            } else {
                Toast.makeText(context, "Reboot Unsuccessful", Toast.LENGTH_LONG).show()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }


    fun resetNetwork() {
        val msg = getString(R.string.TREEHOUSES_DEFAULT_NETWORK)
        sendMessage(msg)
    }

    override fun onRead(output: String) {
        super.onRead(output)
        when (match(output)) {
            RESULTS.NETWORKMODE, RESULTS.DEFAULT_NETWORK -> updateNetworkText(output)
            RESULTS.NETWORKMODE_INFO -> showIpAddress(output)
            RESULTS.DEFAULT_CONNECTED -> {
                //update network mode
                getNetworkMode()
            }
            RESULTS.ERROR -> {
                // showDialog(context, "Error", output)
                showNetworkProgress.value = false
            }
            RESULTS.HOTSPOT_CONNECTED, RESULTS.WIFI_CONNECTED, RESULTS.BRIDGE_CONNECTED -> {

                // showDialog(context, "Network Switched", output)
                //update network mode
                getNetworkMode()
                logD("HOTSPOT CONNECTED")
                // Utils.sendMessage(listener, Pair(msg, "Network Mode retrieved"), context, Toast.LENGTH_LONG)
                showNetworkProgress.value = false
            }
            RESULTS.BOOLEAN -> {
                updateInternet(output)
            }
            RESULTS.SPEED_TEST -> {
                updateSpeed(output)
            }
            else -> logE("NewNetworkFragment: Result not Found")
        }
    }

     fun getNetworkMode() {
        val msg = getString(R.string.TREEHOUSES_NETWORKMODE)
        sendMessage(msg)
    }


    fun bridgeStartConfigListener(stringMap: Map<String, String>) {
        showNetworkProgress.value = true
        logD(stringMap.getValue("etEssid"));
        sendMessage(getString(R.string.TREEHOUSES_BRIDGE, stringMap.getValue("etEssid"), stringMap.getValue("etHotspotEssid"),
                stringMap.getValue("etPassword"), stringMap.getValue("etHotspotPassword")))
        Toast.makeText(context, "Connecting...", Toast.LENGTH_LONG).show()
    }

    fun bridgeSetAddProfileListener(stringMap: Map<String, String>) {
        val networkProfile = NetworkProfile(stringMap.getValue("etEssid"), stringMap.getValue("etHotspotEssid"),
                stringMap.getValue("etPassword"), stringMap.getValue("etHotspotPassword"))
        SaveUtils.addProfile(context, networkProfile)
        Toast.makeText(context, "Bridge Profile Added", Toast.LENGTH_LONG).show()
    }

    fun ethernetStartConfigListener(ip: String, mask: String, gateway: String, dns: String){
        sendMessage(getString(R.string.TREEHOUSES_ETHERNET, ip, mask, gateway, dns))
    }

    fun hotspotStartConfigListener(etHotspotSsid: String, etHotspotPassword: String,
                                   checkBoxHiddenHotspot: Boolean, spnHotspotType: String) {
        if (checkBoxHiddenHotspot) sendHotspotMessage(R.string.TREEHOUSES_AP_HIDDEN, spnHotspotType,
                etHotspotSsid, etHotspotPassword)
        else sendHotspotMessage(R.string.TREEHOUSES_AP, spnHotspotType, etHotspotSsid, etHotspotPassword)
        Toast.makeText(context, "Connecting...", Toast.LENGTH_LONG).show()
    }


    private fun sendHotspotMessage(command : Int, spnHotspotType: String, etHotspotSsid: String, etHotspotPassword: String) {
        showNetworkProgress.value = true
        sendMessage(getString(command, spnHotspotType,
                etHotspotSsid, etHotspotPassword))

    }

    fun hotspotSetAddProfileListener(checkBoxHiddenHotspot: Boolean, spnHotspotType: String,
                                     etHotspotSsid: String, etHotspotPassword: String) {
        SaveUtils.addProfile(context,
                NetworkProfile(etHotspotSsid, etHotspotPassword,
                        spnHotspotType, checkBoxHiddenHotspot))
        Toast.makeText(context, "Hotspot Profile Saved", Toast.LENGTH_LONG).show()
    }


    fun sendWifiMessage(booleanMap: Map<String, Boolean>, ssid:String, password: String, username: String) {
        if (booleanMap.getValue("checkBoxEnterprise") && username.isEmpty()) {
            wifiUserError.value = true
            return
        }
        wifiUserError.value = false
        val hidden = booleanMap.getValue("checkBoxHiddenWifi")
        val enterprise = booleanMap.getValue("checkBoxEnterprise")
        when {
            !enterprise -> sendMessage(getString(if (hidden) R.string.TREEHOUSES_WIFI_HIDDEN else R.string.TREEHOUSES_WIFI, ssid, password))
            enterprise -> sendMessage(getString(if (hidden) R.string.TREEHOUSES_WIFI_HIDDEN_ENTERPRISE else R.string.TREEHOUSES_WIFI_ENTERPRISE, ssid, password, username))
        }
        showNetworkProgress.value = true
        Toast.makeText(context, "Connecting...", Toast.LENGTH_LONG).show()
    }

    fun wifiSetAddProfileListener(editTextSSID: String, wifipassword: String, checkBoxHiddenWifi: Boolean) {
        SaveUtils.addProfile(context, NetworkProfile(editTextSSID,
                wifipassword, checkBoxHiddenWifi))
        Toast.makeText(context, "WiFi Profile Saved", Toast.LENGTH_LONG).show()
    }

    fun hiddenOrEnterprise(isChecked: Boolean) {
        checkBoxChecked.value = isChecked
    }

    fun treehousesInternet(){
        dialogCheck.value = true
        sendMessage("treehouses internet")
    }

    fun updateInternet(output: String){
        if (output.contains("true")) {
            downloadUpload.value = "Internet check passed. Performing speed test......"
            sendMessage("treehouses speedtest")
        } else{
            downloadUpload.value = "Internet check failed. Connect to network"
        }
    }

    fun updateSpeed(output: String){
        if (output.contains("Download:") && output.contains("Upload:")){
            downloadUpload.value = getSubString("Download:", output)
            downloadUpload.value += "\n" + getSubString("Upload", output)
        } else if (output.contains("Download:")){
            downloadUpload.value = getSubString("Download:", output)
        } else {
            downloadUpload.value += "\n" + getSubString("Upload", output)
        }
    }

    fun getSubString(stringStart: String, output: String) : String {
        var startIndex = output.indexOf(stringStart)
        var endIndex = output.indexOf("/s", startIndex)
        return output.substring(startIndex, endIndex + 2)
    }

}