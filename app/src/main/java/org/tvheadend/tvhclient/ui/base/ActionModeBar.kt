package org.tvheadend.tvhclient.ui.base

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.ActionBarContextView
import androidx.core.view.ViewCompat
import androidx.core.view.children

@SuppressLint("RestrictedApi")
class ActionModeBar(context: Context, attrs: AttributeSet) : ActionBarContextView(context, attrs) {
    init {
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            // Override static color set to status bar guard (see updateStatusGuardColor() in AppCompatDelegateImpl)
            (parent as? ViewGroup)?.children
                ?.firstOrNull { it.id == NO_ID }
                ?.let { v -> v.background = background.constantState?.newDrawable() }

            insets
        }
    }
}