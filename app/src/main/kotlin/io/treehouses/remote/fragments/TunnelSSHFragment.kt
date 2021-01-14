package io.treehouses.remote.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.text.Editable
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import io.treehouses.remote.Constants
import io.treehouses.remote.R
import io.treehouses.remote.Tutorials
import io.treehouses.remote.adapter.TunnelPortAdapter
import io.treehouses.remote.bases.BaseTunnelSSHFragment
import io.treehouses.remote.databinding.ActivityTunnelSshFragmentBinding
import io.treehouses.remote.utils.TunnelUtils
import io.treehouses.remote.utils.logD

class TunnelSSHFragment : BaseTunnelSSHFragment(), View.OnClickListener {
    lateinit var addPortCloseButton: ImageButton
    lateinit var addHostCloseButton: ImageButton
    lateinit var addKeyCloseButton: ImageButton

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bind = ActivityTunnelSshFragmentBinding.inflate(inflater, container, false)
        bind!!.switchNotification.isEnabled = false
        bind!!.notifyNow.isEnabled = false
        portList = bind!!.sshPorts
        initializeDialog1()
        addPortButton = bind!!.btnAddPort
        addHostButton = bind!!.btnAddHosts
        arrayOf("1", "2", "three")
        hostsName = ArrayList()
        val adapter: ArrayAdapter<String> = ArrayAdapter(this.requireContext(), R.layout.support_simple_spinner_dropdown_item, hostsName!!)
        dropdown?.adapter = adapter
        addListeners()
        return bind!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind?.let { Tutorials.tunnelSSHTutorials(it, requireActivity()) }
    }

    private fun addListeners() {
        bind!!.switchNotification.setOnCheckedChangeListener { _, isChecked -> switchButton(isChecked) }
        addPortButton!!.setOnClickListener(this)
        addHostButton!!.setOnClickListener(this)
        addingPortButton.setOnClickListener(this)
        addingHostButton.setOnClickListener(this)
        addPortCloseButton.setOnClickListener(this)
        addHostCloseButton.setOnClickListener(this)
        addKeyCloseButton.setOnClickListener(this)
        bind!!.notifyNow.setOnClickListener(this)
        bind!!.btnKeys.setOnClickListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initializeDialog1() {
        dialog = Dialog(requireContext()); dialogHosts = Dialog(requireContext()); dialogKeys = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_sshtunnel_ports)
        dialogHosts.setContentView(R.layout.dialog_sshtunnel_hosts)
        dialogKeys.setContentView(R.layout.dialog_sshtunnel_key)
        dropdown = dialog.findViewById(R.id.hosts)
        inputExternal = dialog.findViewById(R.id.ExternalTextInput)
        inputInternal = dialog.findViewById(R.id.InternalTextInput)
        inputExternalHost = dialogHosts.findViewById(R.id.ExternalTextInput)
        inputInternalHost = dialogHosts.findViewById(R.id.InternalTextInput)
        addingPortButton = dialog.findViewById(R.id.btn_adding_port)
        addingHostButton = dialogHosts.findViewById(R.id.btn_adding_host)
        addCloseButtons()
        portsName = ArrayList(); hostsName = ArrayList(); hostsPosition = ArrayList()
        val window = dialog.window;
        val windowHost = dialogHosts.window
        window!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        windowHost!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        windowHost.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        try { initializeDialog2() }
        catch (exception: Exception) { }
    }

    private fun addCloseButtons() {
        addPortCloseButton = dialog.findViewById(R.id.addPortCloseButton)
        addHostCloseButton = dialogHosts.findViewById(R.id.addHostCloseButton)
        addKeyCloseButton = dialogKeys.findViewById(R.id.addKeyCloseButton)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initializeDialog2() {
        portList!!.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.CustomAlertDialogStyle))
            if (portsName!![position].contains("@")) {
                builder.setTitle("Delete Host  " + portsName!![position] + " ?")
                builder.setPositiveButton("Confirm") { dialog, _ ->
                    val parts = portsName!![position].split(":")[0]
                    writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_REMOVE_HOST, parts))
                    addHostButton!!.text = "deleting host..."
                    portList!!.isEnabled = false; addHostButton!!.isEnabled = false
                    dialog.dismiss()
                }
            }
            initializeDialog3(builder, position)
            builder.setNegativeButton("Cancel", null)
            val dialog = builder.create()
            dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        }
        initializeDialog4()
    }

    private fun initializeDialog3(builder: AlertDialog.Builder, position: Int) {
        builder.setTitle("Delete Port " + portsName!![position] + " ?")
        builder.setPositiveButton("Confirm") { dialog, _ ->
            var myPos: Int = 0
            for (pos in hostsPosition!!.indices) {
                if (hostsPosition!![pos] > position) {
                    myPos = pos
                    break
                }
            }
            if (portsName!!.size > 1 && position == portsName!!.size - 1) { writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_REMOVE_ALL)) }
            else {
                if (hostsPosition!!.last() < position) myPos = hostsPosition!!.lastIndex
                logD("dasda ${myPos.toString()}")
                val portName = TunnelUtils.getPortName(portsName, position)
                val formatArgs = portName + " " + hostsName!![myPos].split(":")[0]
                writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_REMOVE_PORT, formatArgs))
                addPortButton!!.text = "deleting port ....."
                portList!!.isEnabled = false; addPortButton!!.isEnabled = false
            }
            dialog.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initializeDialog4() {
        showKeys = dialogKeys.findViewById(R.id.btn_show_keys)
        saveKeys = dialogKeys.findViewById(R.id.btn_save_keys)
        val profileText = dialogKeys.findViewById<EditText>(R.id.sshtunnel_profile).text
        publicKey = dialogKeys.findViewById(R.id.public_key)
        privateKey = dialogKeys.findViewById(R.id.private_key)
        progressBar = dialogKeys.findViewById(R.id.progress_bar)
        saveKeys.setOnClickListener { keyClickListener(profileText); }
        showKeys.setOnClickListener {
            keyClickListener(profileText);
            handleShowKeys(profileText)
        }
    }

    private fun keyClickListener(profileText: Editable) {
        var profile = profileText.toString()
        writeMessage("treehouses remote key send $profile")
        jsonSend(true)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun handleShowKeys(profileText: Editable) {
        var profile = profileText.toString()
        if (profile.isBlank()) profile = "default"
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("SSHKeyPref", Context.MODE_PRIVATE)
        var storedPublicKey: String? = sharedPreferences.getString("${profile}_public_key", "key")
        var storedPrivateKey: String? = sharedPreferences.getString("${profile}_private_key", "key")
        if (storedPublicKey != null && storedPrivateKey != null) {
            if (storedPublicKey.isBlank()) storedPublicKey = "No public key found"
            if (storedPrivateKey.isBlank()) storedPrivateKey = "No private key found"
        }

        val strPhonePublicKey : Spanned; val strPhonePrivateKey : Spanned
        if ((Build.VERSION.SDK_INT) >= 24) {
            strPhonePublicKey = Html.fromHtml("<b>Phone Public Key for ${profile}:</b> <br>$storedPublicKey\n", Html.FROM_HTML_MODE_LEGACY)
            strPhonePrivateKey = Html.fromHtml("<b>Phone Private Key for ${profile}:</b> <br>$storedPrivateKey", Html.FROM_HTML_MODE_LEGACY)
        } else {
            strPhonePublicKey = Html.fromHtml("<b>Phone Public Key for ${profile}:</b> <br>$storedPublicKey\n")
            strPhonePrivateKey = Html.fromHtml("<b>Phone Private Key for ${profile}:</b> <br>$storedPrivateKey")
        }
        publicKey.text = strPhonePublicKey; privateKey.text = strPhonePrivateKey
    }

    private fun switchButton(isChecked: Boolean) {
        bind!!.switchNotification.isEnabled = false
        if (isChecked) writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_NOTICE_ON))
        else writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_NOTICE_OFF))
    }

    private fun addingHostButton() {
        if (inputExternalHost.text.toString().isNotEmpty() && inputInternalHost.text.toString().isNotEmpty()) {
            if (inputExternalHost.text.toString().contains("@")) {
                val s1 = inputInternalHost.text.toString()
                val s2 = inputExternalHost.text.toString()
                writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_ADD_HOST, s1, s2))
                addHostButton!!.text = "Adding......"
                addHostButton!!.isEnabled = false
            } else Toast.makeText(requireContext(), "Invalid host name", Toast.LENGTH_SHORT).show()
            dialogHosts.dismiss()
        }
    }

    private fun addingPortButton() {
        if (inputExternal.text!!.isNotEmpty() && inputInternal.text!!.isNotEmpty()) {
            val s1 = inputInternal.text.toString()
            val s2 = inputExternal.text.toString()
            val parts = dropdown?.selectedItem.toString().split(":")[0]
            writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_ADD_PORT_ACTUAL, s2, s1, parts))
            addPortButton!!.text = "Adding......"
            addPortButton!!.isEnabled = false
            dialog.dismiss()
        }
    }

    override fun onClick(v: View?) {
        fun showDialog(dialog: Dialog) {
            dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        }
        when (v?.id) {
            R.id.btn_adding_host -> addingHostButton()
            R.id.btn_adding_port -> addingPortButton()
            R.id.notify_now -> {
                bind!!.notifyNow.isEnabled = false
                writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_NOTICE_NOW))
            }
            R.id.btn_add_port -> showDialog(dialog)
            R.id.btn_add_hosts -> showDialog(dialogHosts)
            R.id.btn_keys -> showDialog(dialogKeys)
            R.id.addPortCloseButton -> dialog.dismiss()
            R.id.addHostCloseButton -> dialogHosts.dismiss()
            R.id.addKeyCloseButton -> dialogKeys.dismiss()
        }
    }

    override fun setUserVisibleHint(visible: Boolean) {
        if (visible) {
            mChatService = listener.getChatService()
            mChatService.updateHandler(mHandler)
            writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_NOTICE))
            bind!!.sshPorts
            portsName = ArrayList(); adapter = TunnelPortAdapter(requireContext(), portsName!!)
        }
    }

    override fun getMessage(msg: Message) {
        if (msg.what == Constants.MESSAGE_READ) {
            val readMessage: String = msg.obj as String
            logD("SSHTunnel reply $readMessage")
            if (lastMessage == getString(R.string.TREEHOUSES_REMOTE_KEY_SEND)) logD("Key send: $readMessage")
            val modifyKeywords = arrayOf("Added", "Removed")
            if (readMessage.contains("Host / port not found")) handleHostNotFound()
            else if (readMessage.trim().contains("Removed") && lastMessage == getString(R.string.TREEHOUSES_SSHTUNNEL_REMOVE_ALL)) {
                portsName!!.clear()
                adapter?.notifyDataSetChanged()
                writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_NOTICE));
            } else if ((modifyKeywords.filter { it in readMessage }).isNotEmpty()) handleModifiedList()
            else if (readMessage.contains("@") && lastMessage == getString(R.string.TREEHOUSES_SSHTUNNEL_PORTS)) handleNewList(readMessage);
            else if (readMessage.contains("the command 'treehouses sshtunnel ports' returns nothing")) handleNoPorts()
            else if (readMessage.contains("Status: on")) handleOnStatus()
            else getOtherMessages(readMessage)
        }
    }
}

