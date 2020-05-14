/*
* Copyright 2017 The Android Open Source Project, Inc.
*
* Licensed to the Apache Software Foundation (ASF) under one or more contributor
* license agreements. See the NOTICE file distributed with this work for additional
* information regarding copyright ownership. The ASF licenses this file to you under
* the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at

* http://www.apache.org/licenses/LICENSE-2.0

* Unless required by applicable law or agreed to in writing, software distributed under
* the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
* ANY KIND, either express or implied. See the License for the specific language
* governing permissions and limitations under the License.

*/
package io.treehouses.remote.Network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.preference.PreferenceManager
import io.treehouses.remote.Constants
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import java.util.*

/**
 * Created by yubo on 7/11/17.
 */
/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
class BluetoothChatService(handler: Handler, applicationContext: Context) : Serializable {
    // Member fields
    private val mAdapter: BluetoothAdapter
    private var mDevice: BluetoothDevice? = null

    //    private AcceptThread mSecureAcceptThread;
    //private AcceptThread mInsecureAcceptThread;
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    /**
     * Return the current connection state.
     */
    @get:Synchronized
    var state: Int
        private set
    private var mNewState: Int
    private var bNoReconnect = false
    private val context: Context
    fun updateHandler(handler: Handler) {
        mHandler = handler
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    @Synchronized
    private fun updateUserInterfaceTitle() {
        state = state
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + state)
        mNewState = state

        // Give the new state to the Handler so the UI Activity can update
        mHandler.sendMessage(mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1))
    }

    var connectedDeviceName: String

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    @Synchronized
    fun start() {
        Log.d(TAG, "start")
        bNoReconnect = false
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Start the thread to listen on a BluetoothServerSocket
//        if (mSecureAcceptThread == null) {
//            mSecureAcceptThread = new AcceptThread(true);
//            mSecureAcceptThread.start();
//        }
//        if (mInsecureAcceptThread == null) {
//            mInsecureAcceptThread = new AcceptThread(false);
//            mInsecureAcceptThread.start();
//        }
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    @Synchronized
    fun connect(device: BluetoothDevice, secure: Boolean) {
        Log.d(TAG, "connect to: $device")

        // Cancel any thread attempting to make a connection
        if (state == Constants.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread!!.cancel()
                mConnectThread = null
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(device, secure)
        mConnectThread!!.start()
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    @Synchronized
    fun connected(socket: BluetoothSocket?, device: BluetoothDevice, socketType: String) {
        Log.d(TAG, "connected, Socket Type:$socketType")
        connectedDeviceName = device.name
        mDevice = device
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Cancel the accept thread because we only want to connect to one device
//        if (mSecureAcceptThread != null) {
//            mSecureAcceptThread.cancel();
//            mSecureAcceptThread = null;
//        }
//        if (mInsecureAcceptThread != null) {
//            mInsecureAcceptThread.cancel();
//            mInsecureAcceptThread = null;
//        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket!!, socketType)
        mConnectedThread!!.start()

        // Send the name of the connected device back to the UI Activity
        updateUserInterfaceTitle()
        val msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(Constants.DEVICE_NAME, device.name)
        msg.data = bundle
        mHandler.sendMessage(msg)
        // Update UI title
        Log.e(TAG, "Connected")
    }

    /**
     * Stop all threads
     */
    @Synchronized
    fun stop() {
        Log.d(TAG, "stop")
        bNoReconnect = true
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

//        if (mSecureAcceptThread != null) {
//            mSecureAcceptThread.cancel();
//            mSecureAcceptThread = null;
//        }

//        if (mInsecureAcceptThread != null) {
//            mInsecureAcceptThread.cancel();
//            mInsecureAcceptThread = null;
//        }
        state = Constants.STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread.write
     */
    fun write(out: ByteArray?) {
        // Create temporary object
        Log.d(TAG, "write: " + String(out!!))
        var r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (state != Constants.STATE_CONNECTED) return
            r = mConnectedThread
        }
        // Perform the write unsynchronized
        r!!.write(out)
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        // Send a failure message back to the Activity
        callHandler("Unable to connect to device")
        state = Constants.STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        start()
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        // Send a failure message back to the Activity
        callHandler("Device connection was lost")
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        Log.d(TAG, "connectionLost: ")
        if (mDevice != null && !bNoReconnect && preferences.getBoolean("reconnectBluetooth", true)) {
            connect(mDevice!!, true)
        } else {
            state = Constants.STATE_NONE
            // Update UI title
            updateUserInterfaceTitle()
            // Start the service over to restart listening mode
            start()
        }
    }

    fun callHandler(message: String?) {
        val msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, message)
        msg.data = bundle
        mHandler.sendMessage(msg)
    }
    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    // Uncomment this if phone needs to be able to accept connections from other devices
    //    private class AcceptThread extends Thread {
    //        // The local server socket
    //        private final BluetoothServerSocket mmServerSocket;
    //        private String mSocketType;
    //
    //        public AcceptThread(boolean secure) {
    //            BluetoothServerSocket tmp = null;
    //            mSocketType = secure ? "Secure" : "Insecure";
    //
    //            // Create a new listening server socket
    //            try {
    ////                if (secure) {
    //                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
    ////                } else {
    ////                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
    ////                }
    //            } catch (Exception e) {
    //                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
    //            }
    //            mmServerSocket = tmp;
    //            mCurrentState = Constants.STATE_LISTEN;
    //        }
    //
    //        @Override
    //        public void run() {
    //            Log.d(TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this);
    //            setName("AcceptThread" + mSocketType);
    //
    //            // Listen to the server socket if we're not connected
    //            while (mCurrentState != Constants.STATE_CONNECTED) {
    //                try {
    //                    // This is a blocking call and will only return on a successful connection or an exception
    //                    Log.e("TAG", "currentState: " + mCurrentState);
    //
    //                    socket = mmServerSocket.accept();
    //                } catch (Exception e) {
    //                    Log.e(TAG, "Socket Type: " + mSocketType + " accept() failed" + e);
    //                    mCurrentState = Constants.STATE_NONE;
    //                    updateUserInterfaceTitle();
    //                    checkConnection("connectionCheck");
    //                    break;
    //                }
    //
    //                // If a connection was accepted
    //                if (socket != null) {
    //                    synchronized (BluetoothChatService.this) {
    //                        switch (mCurrentState) {
    //                            case Constants.STATE_LISTEN:
    //                            case Constants.STATE_CONNECTING:
    //                                // Situation normal. Start the connected thread.
    //                                connected(socket, socket.getRemoteDevice(), mSocketType);
    //                                break;
    //                            case Constants.STATE_NONE:
    //                            case Constants.STATE_CONNECTED:
    //                                // Either not ready or already connected. Terminate new socket.
    //                                try {
    //                                    mmServerSocket.close();
    //                                    HomeFragment homeFragment = new HomeFragment();
    //                                    homeFragment.checkConnectionState();
    //                                } catch (IOException e) {
    //                                    Log.e(TAG, "Could not close unwanted socket", e);
    //                                }
    //                                break;
    //                        }
    //                    }
    //                }
    //            }
    //            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);
    //
    //        }
    //
    //        public void cancel() {
    //            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
    //            try {
    //                mmServerSocket.close();
    //            } catch (IOException e) {
    //                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
    //            }
    //        }
    //    }
    //
    //    private void checkConnection(String message) {
    //        mCurrentState = getState();
    //        if (mCurrentState == Constants.STATE_CONNECTED) {
    //            Message msg = mHandler.obtainMessage(Constants.MESSAGE_READ);
    //            Bundle bundle = new Bundle();
    //            bundle.putString(Constants.TOAST, message);
    //            msg.setData(bundle);
    //            RPIDialogFragment rpiDialogFragment = new RPIDialogFragment();
    //            rpiDialogFragment.getmHandler().handleMessage(msg);
    //        }
    //    }
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(private val mmDevice: BluetoothDevice, secure: Boolean) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String
        override fun run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:$mSocketType")
            name = "ConnectThread$mSocketType"

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery()

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket!!.connect()
            } catch (e: IOException) {
                // Close the socket
                try {
                    mmSocket!!.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2)
                }
                connectionFailed()
                return
            }

            // Reset the ConnectThread because we're done
            synchronized(this@BluetoothChatService) { mConnectThread = null }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType)
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect $mSocketType socket failed", e)
            }
        }

        init {
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
//                if (secure) {
                tmp = mmDevice.createRfcommSocketToServiceRecord(
                        MY_UUID_SECURE)
                //                } else {
//                    tmp = device.createInsecureRfcommSocketToServiceRecord(
//                            MY_UUID_INSECURE);
//                }
                state = Constants.STATE_CONNECTING
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e)
                state = Constants.STATE_NONE
            }
            mmSocket = tmp
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private inner class ConnectedThread(socket: BluetoothSocket, socketType: String) : Thread() {
        private val mmSocket: BluetoothSocket
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(10000)
            var bytes: Int
            var out: String

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream!!.read(buffer)
                    out = String(buffer, 0, bytes)
                    Log.d(TAG, "out = " + out + "size of out = " + out.length + ", bytes = " + bytes)
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, out)
                            .sendToTarget()
                    //                    mEmulatorView.write(buffer, bytes);
                    // Send the obtained bytes to the UI Activity
                    //mHandler.obtainMessage(BlueTerm.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    break
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        fun write(buffer: ByteArray?) {
            try {
                Log.d(TAG, "write: I am in inside write method")
                mmOutStream!!.write(buffer)

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
                Log.d(TAG, "write: i am in inside write method exception")
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }

        init {
            Log.d(TAG, "create ConnectedThread: $socketType")
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.inputStream
                Log.d(TAG, " tmpIn = $tmpIn")
                tmpOut = socket.outputStream
                Log.d(TAG, " tmpOut = $tmpOut")
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
            state = Constants.STATE_CONNECTED
        }
    }

    companion object {
        // Debugging
        private const val TAG = "BluetoothChatService"

        // Name for the SDP record when creating server socket
        private const val NAME_SECURE = "BluetoothChatSecure"

        //private static final String NAME_INSECURE = "BluetoothChatInsecure";
        // well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB
        private val MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        //private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private var connectedDeviceName = "NULL"
        private var mHandler: Handler
    }
    //    private BluetoothSocket socket = null;
    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    init {
        mAdapter = BluetoothAdapter.getDefaultAdapter()
        state = Constants.STATE_NONE
        mNewState = state
        mHandler = handler
        context = applicationContext
    }
}