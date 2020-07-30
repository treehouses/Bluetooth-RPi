package io.treehouses.remote.Interfaces

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.ContextThemeWrapper
import android.view.View
import io.treehouses.remote.R

interface FragmentDialogInterface {
    fun showDialog(context: Context?, title: String, message: String) {
        val alertDialog = CreateAlertDialog(context, R.style.CustomAlertDialogStyle,title,message)
                .setPositiveButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }.create()
        alertDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    fun CreateAlertDialog(context: Context?, id:Int, title:String, message:String): AlertDialog.Builder {
        val alertDialog = AlertDialog.Builder(ContextThemeWrapper(context, id))
                .setTitle(title)
                .setMessage(message)

        return alertDialog

    }

    fun CreateAlertDialog(ctw:ContextThemeWrapper, view: View?, title:Int, icon:Int):AlertDialog.Builder{
        return AlertDialog.Builder(ctw)
                .setView(view)
                .setTitle(title)
                .setIcon(icon)
    }
}