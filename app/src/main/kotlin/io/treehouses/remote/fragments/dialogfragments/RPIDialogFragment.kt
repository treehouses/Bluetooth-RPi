package io.treehouses.remote.fragments.dialogfragments

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import io.treehouses.remote.R
import io.treehouses.remote.adapter.RPIListAdapter
import io.treehouses.remote.bases.BaseDialogFragment
import io.treehouses.remote.callback.DeviceDeleteListener
import io.treehouses.remote.databinding.ActivityRpiDialogFragmentBinding
import io.treehouses.remote.pojo.DeviceInfo
import io.treehouses.remote.ui.home.HomeViewModel
import io.treehouses.remote.utils.DialogUtils
import io.treehouses.remote.utils.logD
import java.util.*

class RPIDialogFragment : BaseDialogFragment(), DeviceDeleteListener {
    private val raspberryDevices: MutableList<BluetoothDevice> = ArrayList()
    private val allDevices: MutableList<BluetoothDevice> = ArrayList()
    private var pairedDevices: Set<BluetoothDevice>? = null
    private var mArrayAdapter: ArrayAdapter<*>? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mDialog: AlertDialog? = null
    private val raspberryDevicesText: MutableList<DeviceInfo> = ArrayList()
    private val allDevicesText: MutableList<DeviceInfo> = ArrayList()
    private var pDialog: ProgressDialog? = null

    private val viewModel: HomeViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private var bind: ActivityRpiDialogFragmentBinding? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        instance = this
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothCheck()
        if (mBluetoothAdapter!!.isDiscovering) mBluetoothAdapter!!.cancelDiscovery()
        mBluetoothAdapter!!.startDiscovery()
        bind = ActivityRpiDialogFragmentBinding.inflate(requireActivity().layoutInflater)
        initDialog()
        pairedDevices = mBluetoothAdapter!!.bondedDevices
        setAdapterNotNull(raspberryDevicesText)
        for (d in pairedDevices!!) {
            logD("DEVICE " + d)
            if (checkPiAddress(d.address)) {
                addToDialog(d, raspberryDevicesText, raspberryDevices, false)
                bind!!.progressBar.visibility = View.INVISIBLE
            }
        }
        intentFilter()
        mDialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        return mDialog!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeConnectionStatus()
    }

    private fun observeConnectionStatus() {
//        viewModel.connectionStatus.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
//            when(it) {
//                Constants.STATE_CONNECTED -> {
//                    Log.e("RPIDialogFragment", "Bluetooth Connection Status Change: State Listen")
//                    pDialog?.dismiss()
//                    mBluetoothAdapter?.cancelDiscovery()
//                    Toast.makeText(requireContext(), "Bluetooth Connected", Toast.LENGTH_LONG).show()
//                }
//                Constants.STATE_CONNECTING -> {
//                    Log.e("HEREERER", "NICEE")
//                    pDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
//                    pDialog!!.setTitle("Connecting...")
//                    pDialog!!.setMessage("""
//    Device Name: ${mainDevice!!.name}
//    Device Address: ${mainDevice!!.address}
//    """.trimIndent())
//                    pDialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
//                    pDialog!!.show()
//                }
//                Constants.STATE_NONE -> {
//                    pDialog?.dismiss()
//                    Toast.makeText(requireContext(), "Connection Failed: Please Try Again", Toast.LENGTH_LONG).show()
//                    Log.e("RPIDialogFragment", "Bluetooth Connection Status Change: State None")
//                }
//            }
//        })
    }

    private fun initDialog() {
        pDialog = ProgressDialog(ContextThemeWrapper(context, R.style.CustomAlertDialogStyle))
        mDialog = getAlertDialog(bind!!.root)
        mDialog!!.setTitle(R.string.select_device)
        listViewOnClickListener(bind!!.listView)
        bind!!.rpiCloseButton.setOnClickListener {
            bluetoothCheck("unregister")
            dismiss()
        }
        bind!!.rpiSwitch.isChecked = true
        switchViewOnClickListener()
    }

    private fun intentFilter() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        requireActivity().registerReceiver(mReceiver, filter)
    }

    private fun listViewOnClickListener(mView: View) {
        bind!!.listView.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val deviceList: List<BluetoothDevice> = if (bind!!.rpiSwitch.isChecked) raspberryDevices else allDevices
            if (checkPiAddress(deviceList[position].address)) {
                viewModel.connect(deviceList[position])
                mDialog!!.cancel()
                finish(mView)
                logD("Connecting Bluetooth. Position: $position ;; Status: ${viewModel.connectionStatus.value}")
            } else {
                Toast.makeText(context, "Device Unsupported", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun switchViewOnClickListener() {
        bind!!.rpiSwitch.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            if (isChecked) {
                setAdapterNotNull(raspberryDevicesText)
                buttonView.setText(R.string.paired_devices)
                if (raspberryDevices.isEmpty()) bind!!.progressBar.visibility = View.VISIBLE
            } else {
                setAdapterNotNull(allDevicesText)
                buttonView.setText(R.string.all_devices)
                bind!!.progressBar.visibility = View.INVISIBLE
            }
        }
    }


    private fun finish(mView: View) {
        val mDialog = getAlertDialog(mView)
        mDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
//        if (status == 3) mDialog.setTitle("BLUETOOTH IS CONNECTED") else if (status == 2) mDialog.setTitle("BLUETOOTH IS CONNECTING...") else mDialog.setTitle("BLUETOOTH IS NOT CONNECTED")
        setAdapterNotNull(ArrayList())
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            requireContext().unregisterReceiver(mReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAlertDialog(mView: View): AlertDialog {
        return DialogUtils.createAlertDialog(context, mView, R.drawable.dialog_icon).create()
    }

    fun bluetoothCheck(vararg args: String) {
        if (mBluetoothAdapter == null) {
            Toast.makeText(activity, "Your Bluetooth Is Not Enabled or Not Supported", Toast.LENGTH_LONG).show()
            dismiss()
//            targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, requireActivity().intent)
            try {
                requireContext().unregisterReceiver(mReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (args.isNotEmpty() && args[0] == "unregister") {
            try {
                requireContext().unregisterReceiver(mReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
//            val intent = Intent()
//            intent.putExtra("mChatService", mChatService)
//            targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
        }
    }

    private fun setAdapterNotNull(listVal: List<DeviceInfo>) {
        mArrayAdapter = RPIListAdapter(requireContext(), listVal)
        (mArrayAdapter as RPIListAdapter).deviceListener = this
        bind!!.listView.adapter = mArrayAdapter
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (checkPiAddress(device!!.address)) {
                    addToDialog(device, raspberryDevicesText, raspberryDevices, true)
                    bind!!.progressBar.visibility = View.INVISIBLE
                }
                addToDialog(device, allDevicesText, allDevices, true)
                logD("Broadcast BT ${device.name}${device.address}".trimIndent())
            }
        }
    }

    private fun addToDialog(device: BluetoothDevice, textList: MutableList<DeviceInfo>, mDevices: MutableList<BluetoothDevice>, inRange: Boolean) {
        if (!mDevices.contains(device)) {
            mDevices.add(device)
            textList.add(DeviceInfo("""
    ${device.name}
    ${device.address}
    """.trimIndent(), pairedDevices!!.contains(device), inRange))
        } else textList[mDevices.indexOf(device)].isInRange = true
        mArrayAdapter!!.notifyDataSetChanged()
    }


    companion object {
        var instance: RPIDialogFragment? = null
            private set
        private var mainDevice: BluetoothDevice? = null
        fun newInstance(num: Int): DialogFragment {
            val rpiDialogFragment = RPIDialogFragment()
            val bundle = Bundle()
            bundle.putInt("num", num)
            rpiDialogFragment.arguments = bundle
            return rpiDialogFragment
        }

        fun checkPiAddress(deviceHardwareAddress: String): Boolean {
            val piAddress: Set<String> = HashSet(listOf("B8:27:EB", "DC:A6:32", "E4:5F:01",
                                                        "B8-27-EB", "DC-A6-32", "E4-5F-01",
                                                        "B827.EB", "DCA6.32", "E45F.01",
                                                        "b8:27:eb", "dc:a6:32", "e4:5f:01",
                                                        "b8-27-eb", "dc-a6-32", "e4-5f-01",
                                                        "b827.eb", "dca6.32", "e45f.01"))
            for (item in piAddress) {
                if (deviceHardwareAddress.contains(item)) return true
            }
            return false
        }

    }

    override fun onDeviceDeleted(position: Int) {
        var device = raspberryDevices[position]
        AlertDialog.Builder(activity).setMessage(R.string.delete_device_message)
                .setPositiveButton("Yes") { _, _ ->
                    try {
                        device::class.java.getMethod("removeBond").invoke(device)
                        raspberryDevices.removeAt(position)
                        raspberryDevicesText.removeAt(position)
                        allDevices.remove(device)
                        mArrayAdapter?.notifyDataSetChanged()
                    } catch (e: Exception) {
                        Toast.makeText(activity, "Unable to delete device", Toast.LENGTH_LONG).show()
                    }
                }.setNegativeButton("No", null).show()

    }
}