package io.treehouses.remote.Fragments

import android.graphics.*
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import io.treehouses.remote.Constants
import io.treehouses.remote.Interfaces.FragmentDialogInterface
import io.treehouses.remote.R
import io.treehouses.remote.bases.BaseFragment
import io.treehouses.remote.databinding.ActivityDiscoverFragmentBinding
import kotlinx.android.synthetic.main.activity_discover_fragment.view.*
import java.lang.Exception
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class DiscoverFragment : BaseFragment(), FragmentDialogInterface {
    private lateinit var bind : ActivityDiscoverFragmentBinding
    private var gateway = Gateway()
    private var deviceList = ArrayList<Device>()
    private lateinit var gatewayIcon : ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bind = ActivityDiscoverFragmentBinding.inflate(inflater, container, false)
        deviceList.clear()
        mChatService = listener.getChatService()
        mChatService.updateHandler(mHandler)
        requestNetworkInfo()
        return bind.root
    }

    private fun requestNetworkInfo() {
        Log.d(TAG, "Requesting Network Information")
        try {
            listener.sendMessage(getString(R.string.TREEHOUSES_DISCOVER_GATEWAY_LIST))
            listener.sendMessage(getString(R.string.TREEHOUSES_DISCOVER_GATEWAY))
        }
        catch (e : Exception) {
            Log.e(TAG, "Error Requesting Network Information")
        }
    }

    private fun setupIcons() {
        bind.container.removeAllViewsInLayout()

        val midX = (bind.container.measuredWidth / 2).toFloat()
        val midY = (bind.container.measuredHeight / 2).toFloat()

        val r = this.resources.displayMetrics.widthPixels / 2 * 0.72
        val interval = 2 * PI / (deviceList.size - 1)
        var radians = Random.nextFloat() * 2 * PI

        val size: Int = getSize()

        for (idx in 1 until deviceList.size) {
            val d = deviceList[idx]

            val x = midX + (r * sin(radians)).toFloat() - size / 2
            val y = midY + (r * -cos(radians)).toFloat() - size / 2

            drawLine(midX, midY, x + size / 2, y + size / 2)
            addIcon(x, y, size, d)

            radians = (radians + interval) % (2 * PI)
        }
    }

    private fun addIcon(x: Float, y: Float, size: Int, d: Device) {
        val imageView = ImageView(context)
        imageView.setImageResource(R.drawable.circle_yellow)
        imageView.layoutParams = LinearLayout.LayoutParams(size, size)
        imageView.x = x
        imageView.y = y

        imageView.setOnClickListener {
            val message = ("IP Address: " + d.ip + "\n") +
                    ("MAC Address: " + d.mac)
            message.lines()
            showDialog(context, "Device Information", message)
        }

        bind.container.addView(imageView)
    }

    private fun drawLine(startX: Float, startY: Float, endX: Float, endY: Float) {
        val bitmap = Bitmap.createBitmap(
                bind.container.measuredWidth,
                bind.container.measuredHeight,
                Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        val p4 = Paint()
        p4.isAntiAlias = true
        p4.color = Color.BLACK
        p4.strokeWidth = 10f
        canvas.drawLine(startX, startY, endX, endY, p4)

        val line = ImageView(context)
        line.setImageBitmap(bitmap)
        bind.container.addView(line)
    }

    private fun updateGatewayIcon() {
        gatewayIcon = bind.iconContainer.icon
        bind.iconContainer.removeView(gatewayIcon)
        gatewayIcon.setOnClickListener {
            val message = ("SSID: " + gateway.ssid + "\n") +
                    ("IP Address: " + gateway.device.ip + "\n") +
                    ("MAC Address: " + gateway.device.mac + "\n") +
                    ("Connected Devices: " + (deviceList.size - 1))

            message.lines()
            showDialog(context,"Gateway Information", message)
        }
        bind.iconContainer.addView(gatewayIcon)
    }

    private fun addDevices(readMessage : String) : Boolean {
        val regex = "([0-9]+.){3}[0-9]+\\s+([0-9A-Z]+:){5}[0-9A-Z]+".toRegex()
        val devices = regex.findAll(readMessage)

        devices.forEach {
            val device = Device()

            device.ip = it.value.split("\\s+".toRegex())[0]
            device.mac = it.value.split("\\s+".toRegex())[1]

            if(!deviceList.contains(device))
                deviceList.add(device)
        }

        return !devices.none()
    }

    private fun updateGatewayInfo(readMessage: String) : Boolean {
        var updated = false

        val ip = findRegex("ip address:\\s+([0-9]+.){3}[0-9]", readMessage)
        val ssid = findRegex("ESSID:\"(.)+\"", readMessage)
        val mac = findRegex("MAC Address:\\s+([0-9A-Z]+:){5}[0-9A-Z]+", readMessage)

        if(ip != null) {
            gateway.device.ip = ip.split("ip address:\\s+".toRegex())[1]
            updated = true
        }
        if (ssid != null) {
            var trimmedSsid = ssid.split("ESSID:".toRegex())[1]
            trimmedSsid = trimmedSsid.substring(1, trimmedSsid.length - 1)
            gateway.ssid = trimmedSsid
            updated = true
        }
        if (mac != null) {
            gateway.device.mac = mac.split("MAC Address:\\s+".toRegex())[1]
            updated = true
        }

        return updated
    }

    private fun findRegex(pattern: String, msg: String): String? {
        val regex = pattern.toRegex()
        val res = regex.find(msg)

        if(res != null) return res.value
        else return null
    }

    override fun getMessage(msg: Message) {
        when (msg.what) {
            Constants.MESSAGE_WRITE -> {
                val writeMsg = String((msg.obj as ByteArray))
                Log.d("WRITE", writeMsg)
            }
            Constants.MESSAGE_READ -> {
                val readMessage = msg.obj as String
                Log.d(TAG, "READ = $readMessage")

                if(addDevices(readMessage))
                    setupIcons()
                else if (updateGatewayInfo(readMessage))
                    updateGatewayIcon()
            }
        }
    }

    private fun getSize(): Int {
        return when {
            deviceList.size <= 12 -> ICON_MEDIUM_SIZE
            deviceList.size <= 20 -> ICON_SMALL_SIZE
            else -> ICON_XSMALL_SIZE
        }
    }

    inner class Device {
        lateinit var ip : String
        lateinit var mac : String

        override fun equals(other : Any?) : Boolean {
            return this.ip == (other as Device).ip
        }
    }

    inner class Gateway {
        var device = Device()
        lateinit var ssid : String
    }

    companion object {
        private const val TAG = "Discover Fragment"
        private const val ICON_MEDIUM_SIZE = 200
        private const val ICON_SMALL_SIZE = 120
        private const val ICON_XSMALL_SIZE = 80
    }
}
