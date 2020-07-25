package io.treehouses.remote.adapter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi
import io.treehouses.remote.Network.BluetoothChatService
import io.treehouses.remote.R
import io.treehouses.remote.callback.HomeInteractListener
import io.treehouses.remote.pojo.NetworkListItem

class NetworkListAdapter(val context: Context, list: List<NetworkListItem>, private val chatService: BluetoothChatService) : BaseExpandableListAdapter() {
    private val list: List<NetworkListItem>
    private val inflater: LayoutInflater
    private var listener: HomeInteractListener? = null
    private val views: Array<View?>
    fun setListener(listener: HomeInteractListener?) {
        this.listener = listener
        if (listener == null) {
            throw RuntimeException("Please implement home interact listener")
        }
    }

    override fun getGroupCount(): Int {
        return list.size
    }

    override fun getChildrenCount(position: Int): Int {
        return if (position > list.size - 1) 0 else 1
    }

    override fun getGroup(i: Int): Any {
        return list[i].title
    }

    override fun getChild(i: Int, i1: Int): Any {
        return list[i].layout
    }

    override fun getGroupId(i: Int): Long {
        return 0
    }

    override fun getChildId(i: Int, i1: Int): Long {
        return 0
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(i: Int, b: Boolean, convertView: View?, parent: ViewGroup): View {
        val convertView: View?
        convertView = inflater.inflate(R.layout.list_group, parent, false)
        val listHeader = convertView.findViewById<TextView>(R.id.lblListHeader)
        listHeader.text = getGroup(i).toString()
        return convertView
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun getChildView(i: Int, i1: Int, b: Boolean, convertView: View?, parent: ViewGroup): View {

        // Needs to recycle views instead of creating new ones each time.
        // if (convertView == null) creating bugs
        val convertView: View?
        if (views[i] != null) {
            return views[i]!!
        }
        convertView = inflater.inflate(list[i].layout, parent, false)
        layout = list[i].layout
        position = i
        when (layout) {
            R.layout.open_vnc -> ViewHolderVnc(convertView, context, listener!!)
            R.layout.configure_tethering -> ViewHolderTether(convertView, listener!!, context)
            R.layout.configure_ssh_key -> ViewHolderSSHKey(convertView, context, listener!!)
            R.layout.configure_camera -> ViewHolderCamera(convertView, context, listener!!)
            R.layout.configure_wificountry -> ViewHolderWifiCountry(convertView, context, listener!!)
            R.layout.configure_blocker -> ViewHolderBlocker(convertView, context, listener!!)
            R.layout.configure_sshtunnel_key -> ViewHolderSSHTunnelKey(convertView, context, listener!!)
            R.layout.configure_discover -> ViewHolderDiscover(convertView, listener!!)
        }
        views[i] = convertView
        return convertView
    }

    override fun isChildSelectable(i: Int, i1: Int): Boolean {
        return false
    }

    companion object {
        var layout = 0
            private set
        var position = 0
    }

    init {
        inflater = LayoutInflater.from(context)
        this.list = list
        views = arrayOfNulls(8)
    }
}