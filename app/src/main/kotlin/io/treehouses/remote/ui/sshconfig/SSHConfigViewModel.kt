package io.treehouses.remote.ui.sshconfig

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.treehouses.remote.Constants
import io.treehouses.remote.MainApplication
import io.treehouses.remote.R
import io.treehouses.remote.ssh.beans.HostBean
import io.treehouses.remote.bases.FragmentViewModel
import io.treehouses.remote.utils.KeyUtils
import io.treehouses.remote.utils.SaveUtils
import io.treehouses.remote.utils.Utils.toast
import io.treehouses.remote.utils.logD
import java.util.regex.Pattern

open class SSHConfigViewModel(application: Application) : FragmentViewModel(application) {
    private val context = getApplication<MainApplication>().applicationContext
    var sshTextInputText: MutableLiveData<String> = MutableLiveData() //bind.sshTextInput.setText
    var sshTextInputError: MutableLiveData<String> = MutableLiveData() //bind.sshTextInput.error
    var noHostsVisibility: MutableLiveData<Boolean> = MutableLiveData()  //bind.noHosts.visibility
    var pastsHostsVisibility: MutableLiveData<Boolean> = MutableLiveData()  //bind.pastHosts.visibility
    var pastHostsList: MutableLiveData<List<HostBean>> = MutableLiveData()  //pastHosts
    var connectSshEnabled: MutableLiveData<Boolean> = MutableLiveData()  //bind.connectSsh.isEnabled
    var connectSshClickable: MutableLiveData<Boolean> = MutableLiveData()  //bind.connectSsh.isClickable = bool
    var smartConnectEnabled: MutableLiveData<Boolean> = MutableLiveData()  //bind.smartConnect.isEnabled = bool
    var smartConnectClickable: MutableLiveData<Boolean> = MutableLiveData()  //bind.smartConnect.isClickable = bool


    override fun onRead(output: String) {
        super.onRead(output)
        if (output.isNotEmpty()) getIP(output)

    }

    fun createView(){
        if (mChatService.state == Constants.STATE_CONNECTED) {
            sendMessage(getString(R.string.TREEHOUSES_NETWORKMODE_INFO))
            mChatService.updateHandler(mHandler)
        }
    }

    fun sshTextChangedListener(sshPattern: Pattern, sshText: String){
        if (sshPattern.matcher(sshText).matches()) {
            sshTextInputError.value = null
            setEnabled(true)
        } else {
            sshTextInputError.value = "Unknown Format"
            setEnabled(false)
        }
    }

    fun checkForSmartConnectKey(): Boolean {
        if (!KeyUtils.getAllKeyNames(context).contains("SmartConnectKey")) {
            if (mChatService.state == Constants.STATE_CONNECTED) {
                val key = KeyUtils.createSmartConnectKey(context)
                sendMessage(getString(R.string.TREEHOUSES_SSHKEY_ADD, KeyUtils.getOpenSSH(key)))
            } else {
                context.toast("Bluetooth not connected. Could not send key to Pi.")
                return false
            }
        }
        return true
    }

    private fun getIP(s: String) {
        if (s.contains("eth0")) {
            val ipAddress = s.substringAfterLast("ip: ").trim()
            val hostAddress = "pi@$ipAddress"
            sshTextInputText.value = hostAddress
            logD("GOT IP $ipAddress")
        } else if (s.contains("ip") || s.startsWith("essid")) {
            val ipString = s.split(", ")[1]
            val ipAddress = ipString.substring(4)
            val hostAddress = "pi@$ipAddress"
            sshTextInputText.value = hostAddress
            logD("GOT IP $ipAddress")
        }
    }

    fun setEnabled(bool: Boolean) {
        connectSshEnabled.value = bool
        connectSshClickable.value = bool
        smartConnectEnabled.value = bool
        smartConnectClickable.value = bool
    }

    fun getPastHost(){
        pastHostsList.value = SaveUtils.getAllHosts(context).reversed()
    }

    fun setNoHostPastHost(){
        if (pastHostsList.value!!.isEmpty()) {
            noHostsVisibility.value = true
            pastsHostsVisibility.value = false
        }
    }

    fun recylerOnClick(prettyFormat: String){
        sshTextInputText.value = prettyFormat
    }

}