package io.treehouses.remote.SSHConsole

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import io.treehouses.remote.SSHConsole.BaseSSHConsole
import io.treehouses.remote.utils.LogUtils
import java.lang.ref.WeakReference

class URLItemListener internal constructor(context: Context) : AdapterView.OnItemClickListener {
    private val contextRef: WeakReference<Context> = WeakReference(context)
    override fun onItemClick(arg0: AdapterView<*>?, view: View, position: Int, id: Long) {
        val context = contextRef.get() ?: return
        try {
            val urlView = view as TextView
            val url = urlView.text.toString()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            LogUtils.log("${BaseSSHConsole.TAG} couldn't open URL $e")
            // We should probably tell the user that we couldn't find a handler...
        }
    }
}