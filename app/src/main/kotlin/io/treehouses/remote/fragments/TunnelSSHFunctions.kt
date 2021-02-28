package io.treehouses.remote.fragments

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.treehouses.remote.R
import io.treehouses.remote.bases.BaseTunnelSSHFragment
import io.treehouses.remote.utils.DialogUtils

open class TunnelSSHFunctions: BaseTunnelSSHFragment() {

    /*
       checks if the inputs in the host dialog are not empty. If so, check if their corresponding textLayouts don't have errors.
       If so, then enable addingHostButton.
         */
    protected fun checkAddingHostButtonEnable(){
        if(inputUserName.editableText.isNotEmpty() && inputDomainIP.editableText.isNotEmpty() && inputPortNumber.editableText.isNotEmpty())
            if(!textLayoutUserName.isErrorEnabled && !textLayoutDomainName.isErrorEnabled && !textLayoutPortName.isErrorEnabled )
                addingHostButton.isEnabled = true
    }

    /*
       adds a syntax check to textInputEditText. If input in textInputEditText does not match regex, outputs error message in textInputLayout
       and disables addingHostButton
         */
    protected fun addSyntaxCheck(textInputEditText: TextInputEditText, textInputLayout: TextInputLayout, regex: String, error: String){
        textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                textInputLayout.setErrorEnabled(true)
                if(s!!.isEmpty()){
                    addingHostButton.isEnabled = false
                } else {
                    if(!s!!.toString().matches(regex.toRegex()) ){
                        addingHostButton.isEnabled = false
                        textInputLayout.setError(error)
                    } else {
                        textInputLayout.setErrorEnabled(false)
                        checkAddingHostButtonEnable()
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    protected fun addPortListListener() {
        portList!!.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            if (portsName!!.size > 1 && position == portsName!!.size - 1) {
                DialogUtils.createAlertDialog(context, "Delete All Hosts and Ports?") { writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_REMOVE_ALL)) }
            }
        }
    }

    protected fun switchButton(isChecked: Boolean) {
        bind!!.switchNotification.isEnabled = false
        if (isChecked) writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_NOTICE_ON))
        else writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_NOTICE_OFF))
    }

    protected fun addingHostButton() {
        val m1 = inputPortNumber.text.toString()
        val m2 = inputUserName.text.toString() + "@" + inputDomainIP.text.toString()
        writeMessage(getString(R.string.TREEHOUSES_SSHTUNNEL_ADD_HOST, m1, m2))
        addHostButton!!.text = "Adding......"
        addHostButton!!.isEnabled = false
        dialogHosts.dismiss()

    }

    protected fun addingPortButton() {
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


}