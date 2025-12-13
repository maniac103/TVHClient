package org.tvheadend.tvhclient.ui.features.settings

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.preference.ListPreference
import androidx.preference.ListPreferenceDialogFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialListPreferenceDialog : ListPreferenceDialogFragmentCompat() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val builder = MaterialAlertDialogBuilder(activity)
            .setTitle(preference.dialogTitle)
            .setIcon(preference.dialogIcon)
            .setPositiveButton(preference.positiveButtonText, this)
            .setNegativeButton(preference.negativeButtonText, this)
        val contentView = onCreateDialogView(activity)
        if (contentView != null) {
            onBindDialogView(contentView)
            builder.setView(contentView)
        } else {
            builder.setMessage(preference.dialogMessage)
        }
        onPrepareDialogBuilder(builder)

        return builder.create()
    }

    companion object {
        fun create(pref: ListPreference) = MaterialListPreferenceDialog().apply {
            arguments = bundleOf(ARG_KEY to pref.key)
        }
    }
}
