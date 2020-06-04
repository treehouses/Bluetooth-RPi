package io.treehouses.remote.Fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.treehouses.remote.BuildConfig
import io.treehouses.remote.R
import io.treehouses.remote.callback.NotificationCallback
import java.text.SimpleDateFormat
import java.util.*

class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view:View = inflater.inflate(R.layout.fragment_about, container, false)

        val gitHub = view.findViewById<Button>(R.id.btn_github)
        val images = view.findViewById<Button>(R.id.btn_image)
        val gitter = view.findViewById<Button>(R.id.btn_gitter)
        val version = view.findViewById<Button>(R.id.btn_version)
        val contributors = view.findViewById<Button>(R.id.btn_contributors)
        val tvCopyright = view.findViewById<TextView>(R.id.tv_copyright)
        hyperLinks(gitHub, "https://github.com/treehouses/remote")
        hyperLinks(images, "https://treehouses.io/#!pages/download.md")
        hyperLinks(gitter, "https://gitter.im/open-learning-exchange/raspberrypi")
        hyperLinks(contributors, "https://github.com/treehouses/remote/graphs/contributors")
        version.setOnClickListener { v: View? ->
            var versionName = BuildConfig.VERSION_NAME
            if (versionName == "1.0.0") {
                versionName = "latest version"
            }
            Toast.makeText(context, versionName, Toast.LENGTH_LONG).show()
        }
        val format = SimpleDateFormat("yyyy")
        tvCopyright.text = String.format(getString(R.string.copyright), format.format(Date()) + "")
        return view
    }

    private fun hyperLinks(view: View, url: String) {
        view.setOnClickListener { view1: View? ->
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }
}