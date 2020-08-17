package io.treehouses.remote.bases

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.treehouses.remote.Constants
import io.treehouses.remote.Network.BluetoothChatService
import io.treehouses.remote.callback.HomeInteractListener
import java.lang.NullPointerException

open class BaseFragment : Fragment() {
    open lateinit var mChatService: BluetoothChatService
    var mBluetoothAdapter: BluetoothAdapter? = null
    lateinit var listener: HomeInteractListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is HomeInteractListener) context else throw RuntimeException("Implement interface first")
        mChatService = listener.getChatService()
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    fun isListenerInitialized() = ::listener.isInitialized


    open fun chatOpen():Boolean {
        return this::mChatService.isInitialized
    }


    protected fun onLoad(mHandler: Handler?) {
//        mChatService = listener.getChatService()
        mChatService.updateHandler(mHandler!!)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            requireActivity().finish()
        }
        checkStatusNow()
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT)
        } else {
            setupChat()
        }
    }

    protected open val mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constants.MESSAGE_STATE_CHANGE -> {
                    try {
                        Toast.makeText(activity, "Bluetooth disconnected", Toast.LENGTH_LONG).show()
                        listener.redirectHome()
                    } catch (exception:NullPointerException){
                        Log.e("Error", exception.toString())
                    }
                }
                else -> getMessage(msg)
            }
        }
    }
    fun checkStatusNow() {}
    open fun setupChat() {}
    open fun getMessage(msg: Message) {}
}