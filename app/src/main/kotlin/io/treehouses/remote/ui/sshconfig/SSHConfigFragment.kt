package io.treehouses.remote.ui.sshconfig

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import io.treehouses.remote.fragments.dialogfragments.SSHAllKeyFragment
import io.treehouses.remote.fragments.dialogfragments.SSHKeyGenFragment
import io.treehouses.remote.Tutorials
import io.treehouses.remote.adapter.ViewHolderSSHRow
import io.treehouses.remote.bases.BaseFragment
import io.treehouses.remote.ssh.beans.HostBean
import io.treehouses.remote.sshconsole.SSHConsole
import io.treehouses.remote.callback.RVButtonClickListener
import io.treehouses.remote.databinding.DialogSshBinding
import io.treehouses.remote.databinding.RowSshBinding
import io.treehouses.remote.fragments.dialogfragments.EditHostDialogFragment
import io.treehouses.remote.ssh.interfaces.OnHostStatusChangedListener
import io.treehouses.remote.ssh.terminal.TerminalManager
import io.treehouses.remote.utils.SaveUtils
import io.treehouses.remote.utils.logD
import io.treehouses.remote.views.RecyclerViewClickListener
import java.lang.Exception
import java.util.regex.Pattern


class SSHConfigFragment : BaseFragment(), RVButtonClickListener, OnHostStatusChangedListener {

    protected val sshPattern = Pattern.compile("^(.+)@(([0-9a-z.-]+)|(\\[[a-f:0-9]+\\]))(:(\\d+))?$", Pattern.CASE_INSENSITIVE)
    protected val viewModel: SSHConfigViewModel by viewModels(ownerProducer = { this })
    private lateinit var bind: DialogSshBinding

    protected lateinit var pastHosts: List<HostBean>
    protected lateinit var adapter : RecyclerView.Adapter<ViewHolderSSHRow>
    protected var bound : TerminalManager? = null
    protected val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            bound = (service as TerminalManager.TerminalBinder).service
            // update our listview binder to find the service
            setUpAdapter()
            if (!bound?.hostStatusChangedListeners?.contains(this@SSHConfigFragment)!!) {
                bound?.hostStatusChangedListeners?.add(this@SSHConfigFragment)
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            bound?.hostStatusChangedListeners?.remove(this@SSHConfigFragment)
            bound = null
            setUpAdapter()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bind = DialogSshBinding.inflate(inflater, container, false)
        viewModel.createView()
        loadObservers1()
        loadObservers2()
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setEnabled(false)
        setListeners()
        Tutorials.sshTutorial(bind, requireActivity())
        setUpAdapter()
    }

    fun setListeners(){
        bind.sshTextInput.addTextChangedListener {
            viewModel.sshTextChangedListener(sshPattern, it.toString())
        }
        bind.connectSsh.setOnClickListener {
            var uriString = bind.sshTextInput.text.toString()
            connect(uriString, false)
        }
        bind.generateKeys.setOnClickListener { SSHKeyGenFragment().show(childFragmentManager, "GenerateKey") }
        bind.smartConnect.setOnClickListener {
            val shouldConnect = viewModel.checkForSmartConnectKey()
            var uriString = bind.sshTextInput.text.toString()
            if (shouldConnect) connect(uriString, true)
        }
        bind.showKeys.setOnClickListener { SSHAllKeyFragment().show(childFragmentManager, "AllKeys") }
    }

    fun loadObservers1(){
        viewModel.sshTextInputText.observe(viewLifecycleOwner, Observer {
            bind.sshTextInput.setText(it.toString())
        })
        viewModel.sshTextInputError.observe(viewLifecycleOwner, Observer {
            bind.sshTextInput.error = it
        })
        viewModel.noHostsVisibility.observe(viewLifecycleOwner, Observer {
            bind.noHosts.visibility = if (!it) View.GONE else View.VISIBLE
        })
        viewModel.pastsHostsVisibility.observe(viewLifecycleOwner, Observer {
            var view = if (it) View.VISIBLE else View.GONE
            bind.pastHosts.visibility = view
        })
        viewModel.pastHostsList.observe(viewLifecycleOwner, Observer {
            pastHosts = it
        })
    }

    fun loadObservers2(){
        viewModel.connectSshEnabled.observe(viewLifecycleOwner, Observer {
            bind.connectSsh.isEnabled = it
        })
        viewModel.connectSshClickable.observe(viewLifecycleOwner, Observer {
            bind.connectSsh.isClickable = it
        })
        viewModel.smartConnectEnabled.observe(viewLifecycleOwner, Observer {
            bind.smartConnect.isEnabled = it
        })
        viewModel.smartConnectClickable.observe(viewLifecycleOwner, Observer {
            bind.smartConnect.isClickable = it
        })
    }

    private fun connect(uriStr: String, isSmartConnect: Boolean) {
        var uriString = uriStr
        if (!uriString.startsWith("ssh://")) uriString = "ssh://$uriString"
        val host = HostBean()
        host.setHostFromUri(Uri.parse(uriString))
        if (isSmartConnect) {
            host.keyName = "SmartConnectKey"
            host.fontSize = 7
        }
        SaveUtils.updateHostList(requireContext(), host)
        logD("HOST URI " + host.uri.toString())
        launchSSH(requireActivity(), host)
    }

    private fun launchSSH(activity: FragmentActivity, host: HostBean) {
        val contents = Intent(Intent.ACTION_VIEW, host.uri)
        contents.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        contents.setClass(activity, SSHConsole::class.java)
        activity.startActivity(contents)
    }

    fun setUpAdapter() {
        viewModel.getPastHost()
        if (!isVisible) return
        viewModel.setNoHostPastHost()
        adapter = object : RecyclerView.Adapter<ViewHolderSSHRow>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderSSHRow {
                val holderBinding = RowSshBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ViewHolderSSHRow(holderBinding, this@SSHConfigFragment)
            }

            override fun getItemCount(): Int { return pastHosts.size }

            override fun onBindViewHolder(holder: ViewHolderSSHRow, position: Int) {
                val host = pastHosts[position]
                holder.bind(host)
                if (bound?.mHostBridgeMap?.get(host)?.get() != null) holder.setConnected(true) else holder.setConnected(false)
            }
        }
        bind.pastHosts.adapter = adapter
        addItemTouchListener()
    }

    fun addItemTouchListener() {
        val listener = RecyclerViewClickListener(requireContext(), bind.pastHosts, object : RecyclerViewClickListener.ClickListener {
            override fun onClick(view: View?, position: Int) {
                val clicked = pastHosts[position]
                viewModel.recylerOnClick(clicked.getPrettyFormat())
            }
            override fun onLongClick(view: View?, position: Int) {}
        })
        bind.pastHosts.addOnItemTouchListener(listener)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.bindService(Intent(context, TerminalManager::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        try {activity?.unbindService(connection)} catch (e: Exception) {logD("SSHConfig $e")}
    }

    override fun onButtonClick(position: Int) {
        val edit = EditHostDialogFragment()
        edit.setOnDismissListener(DialogInterface.OnDismissListener { setUpAdapter() })
        edit.arguments = Bundle().apply { putString(EditHostDialogFragment.SELECTED_HOST_URI, pastHosts[position].uri.toString())}
        edit.show(childFragmentManager, "EditHost")
    }

    override fun onHostStatusChanged() {
        if (context != null) setUpAdapter()
    }
}