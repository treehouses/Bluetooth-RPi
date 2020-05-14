package io.treehouses.remote.Fragments

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import io.treehouses.remote.R

/**
 * This class is the hotspot and wifi dialog validator
 */
class TextBoxValidation {
    private var mDialog: AlertDialog? = null
    private var textWatcher: EditText? = null
    var SSID: EditText? = null
    var PWD: EditText? = null
    private var start: Button? = null
    private var addprofile: Button? = null
    private var textInputLayout: TextInputLayout? = null

    //TODO: this file needs to be refactored and maybe make it usable for the future
    private val IpAddressEditText: EditText? = null
    private val MaskEditText: EditText? = null
    private val GateWayEditText: EditText? = null
    private val DNSEditText: EditText? = null
    private var ESSIDEditText: EditText? = null
    private var HotspotESSIDEditText: EditText? = null
    var PasswordEditText: EditText? = null
    var HotspotPasswordEditText: EditText? = null
    fun setmDialog(mDialog: AlertDialog?) {
        this.mDialog = mDialog
    }

    fun setTextWatcher(textWatcher: EditText?) {
        this.textWatcher = textWatcher
    }

    constructor(context: Context, e1: EditText?, e2: EditText?, type: String) {
        if (type == "wifi") {
            SSID = e1
            PWD = e2
            textboxValidation(context, type, e1)
            textboxValidation(context, type, e2)
        } else if (type == "bridge") {
            ESSIDEditText = e1
            HotspotESSIDEditText = e2
            textboxValidation(context, type, ESSIDEditText)
            textboxValidation(context, type, HotspotESSIDEditText)
        }
    }

    fun setStart(start: Button?) {
        this.start = start
    }

    fun setAddprofile(addprofile: Button?) {
        this.addprofile = addprofile
    }

    fun setTextInputLayout(textInputLayout: TextInputLayout?) {
        this.textInputLayout = textInputLayout
    }

    constructor() {}

    /**
     * Textwatcher for most dialogs
     *
     */
    fun textboxValidation(context: Context, type: String, toWatch: EditText?) {
        toWatch!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (type == "ethernet") {
//                    validateETHERNET(context);
                } else if (type == "wifi") {
                    validateWIFI(context)
                } else if (type == "bridge") {
                    validateBridge(context)
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })
    }

    /**
     * Textwatcher for the change password dialog
     *
     */
    fun changePWValidation(confirmPWD: EditText, context: Context) {
        textWatcher!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                validateChangedPassword(confirmPWD, context)
            }

            override fun afterTextChanged(editable: Editable) {}
        })
    }

    /**
     * Method that sets the dialog positive button to true or false
     */
    fun dialogButtonTrueOrFalse(mDialog: AlertDialog?, button: Boolean?) {
        if (mDialog == null) return
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).isClickable = button!!
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = button
    }

    fun dialogButtonTrueOrFalse(button1: Button?, button2: Button?, enabled: Boolean) {
        if (button1 != null) {
            button1.isClickable = enabled
            button1.isEnabled = enabled
        }
        if (button2 != null) {
            button2.isEnabled = enabled
            button2.isEnabled = enabled
        }
    }

    /**
     * WiFi dialog validator
     *
     */
    private fun validateWIFI(context: Context) {
        var flag = true
        if (SSID!!.length() == 0) {
            dialogButtonTrueOrFalse(start, addprofile, false)
            SSID!!.error = context.getString(R.string.error_ssid_empty)
            flag = false
        }
        if (PWD!!.length() > 0 && PWD!!.length() < 8) {
            dialogButtonTrueOrFalse(start, addprofile, false)
            textInputLayout!!.error = context.getString(R.string.error_pwd_length)
            flag = false
        }
        if (flag) {
            dialogButtonTrueOrFalse(start, addprofile, true)
            textInputLayout!!.error = null
        }
    }
    /**
     * ETHERNET dialog validator
     *
     */
    //    private void validateETHERNET(final Context context) {
    //        if (IpAddressEditText.length() == 0 || MaskEditText.length() == 0 || GateWayEditText.length() == 0 || DNSEditText.length() == 0) {
    //            dialogButtonTrueOrFalse(start,addprofile, false);
    //        }else {
    //            dialogButtonTrueOrFalse(start, addprofile,true);
    //        }
    //    }
    /**
     * ETHERNET dialog validator
     *
     */
    private fun validateBridge(context: Context) {
        var flag = true
        if (ESSIDEditText!!.length() == 0) {
            flag = false
            ESSIDEditText!!.error = "This field cannot be empty"
        }
        if (HotspotESSIDEditText!!.length() == 0) {
            flag = false
            HotspotESSIDEditText!!.error = "This field cannot be empty"
        }
        dialogButtonTrueOrFalse(start, addprofile, flag)
    }

    /**
     * Change password validator
     *
     */
    private fun validateChangedPassword(confirmPWD: EditText, context: Context) {
        if (confirmPWD.text.toString() == PWD!!.text.toString()) {
            dialogButtonTrueOrFalse(mDialog, true)
        } else if (confirmPWD.text.toString() != PWD!!.text.toString()) {
            dialogButtonTrueOrFalse(mDialog, false)
            confirmPWD.error = context.getString(R.string.error_pwd_confirm)
        } else {
            dialogButtonTrueOrFalse(mDialog, true)
        }
    }

    fun getListener(mDialog: AlertDialog) {
        mDialog.setOnShowListener { dialog: DialogInterface? -> dialogButtonTrueOrFalse(mDialog, false) }
    }
}