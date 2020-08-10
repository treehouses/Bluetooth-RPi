package io.treehouses.remote.Fragments

import android.content.Context
import android.graphics.drawable.PictureDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import com.caverock.androidsvg.SVG
import io.treehouses.remote.callback.ServiceAction
import io.treehouses.remote.databinding.ServiceCardBinding
import io.treehouses.remote.pojo.ServiceInfo

class ServiceCardFragment : Fragment(), View.OnClickListener {
    private var actionListener: ServiceAction? = null
    private var binding: ServiceCardBinding? = null
    private lateinit var serviceData: ServiceInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey("serviceData"))
                serviceData = it.getSerializable("serviceData") as ServiceInfo
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ServiceCardBinding.inflate(inflater, container, false)
        binding!!.serviceInfo.movementMethod = LinkMovementMethod.getInstance()
        binding!!.serviceInfo.isFocusable = true
        if (!serviceData.isHeader) {
            setServiceInfo(serviceData.info)
            setServiceSize(serviceData.size)
            showIcon(serviceData.icon)
            updateButtons(serviceData.serviceStatus)
            setAutorun(serviceData.autorun)
            binding!!.installButton.setOnClickListener(this)
            binding!!.startButton.setOnClickListener(this)
            binding!!.openLink.setOnClickListener(this)
            binding!!.autorunChecked.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean -> actionListener!!.onClickAutorun(serviceData, isChecked) }
        }
        return binding!!.root
    }

    private fun setAutorun(autorun: String?) {
        binding!!.autorunChecked.isChecked = autorun!!.contains("true")
    }

    private fun setButtons(started: Boolean, installed: Boolean) {
        var string1 = "Start"
        var visibility1 = View.GONE

        if (started) {
            string1 = "Stop"
            visibility1 = View.VISIBLE
        }

        var string2 = "Install"
        var visibility2 = View.GONE
        var visibility3 = View.GONE

        binding!!.startButton.text = string1
        binding!!.openLink.visibility = visibility1

        if (installed) {
            string2 = "Uninstall"
            visibility2 = View.VISIBLE
            visibility3 = View.VISIBLE
        }

        binding!!.installButton.text = string2
        binding!!.startButton.visibility = visibility2
        binding!!.autorunChecked.visibility = visibility3

    }

    private fun updateButtons(statusCode: Int) {
        when (statusCode) {
            ServiceInfo.SERVICE_AVAILABLE -> setButtons(started = false, installed = false)
            ServiceInfo.SERVICE_INSTALLED -> setButtons(started = false, installed = true)
            ServiceInfo.SERVICE_RUNNING -> setButtons(started = true, installed = true)
        }
    }

    private fun showIcon(s: String?) {
        try {
            Log.d(serviceData.name, "showIcon:" + serviceData.icon)
            val svg = SVG.getFromString(s)
            val pd = PictureDrawable(svg.renderToPicture())
            binding!!.serviceLogo.setImageDrawable(pd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setServiceInfo(s: String?) {
        val spannableString = SpannableString(s)
        Linkify.addLinks(spannableString, Linkify.ALL)
        binding!!.serviceInfo.text = s
        binding!!.serviceInfo.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setServiceSize(sz: Int) {
        if(sz != -1)
            binding!!.serviceSize.text = "$sz MB"
    }

    override fun onClick(v: View) {
        if (binding!!.installButton == v) {
            actionListener!!.onClickInstall(serviceData)
        } else if (binding!!.startButton == v) {
            actionListener!!.onClickStart(serviceData)
        } else if (binding!!.openLink == v) {
            actionListener!!.onClickLink(serviceData)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        actionListener = parentFragment as ServiceAction?
    }

}