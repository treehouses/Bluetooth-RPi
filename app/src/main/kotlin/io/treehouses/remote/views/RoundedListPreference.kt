package io.treehouses.remote.views

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import androidx.preference.ListPreference
import io.treehouses.remote.R

class RoundedListPreference : ListPreference {

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
            context,
            attrs,
            defStyleAttr
    )

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?) : super(context)

    override fun onClick() {

        AlertDialog.Builder(ContextThemeWrapper(context, R.style.CustomAlertDialogStyle))
                .setTitle(title)
                .setSingleChoiceItems(entryValues, 1) { dialog: DialogInterface, index: Int->
                    if(callChangeListener(entryValues[index].toString())){
                        setValueIndex(index)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int  -> dialog.dismiss() }
                .show()
    }

//    private fun getValueIndex() = context.resources.getStringArray(R.array.app_language).indexOf(value)
}