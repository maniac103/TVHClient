package org.tvheadend.tvhclient.ui.features.dvr

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonGroup
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.DaysOfWeekButtonBinding

class DaysOfWeekButtonBar(context: Context, attrs: AttributeSet) : MaterialButtonGroup(context, attrs) {
    var selectedDaysBitmask: Int
        get() {
            var result = 0
            (0 until buttons.size)
                .filter { buttons[it].isChecked }
                .forEach { result += 1 shl it }
            return result
        }
        set(value) {
            settingSelectedDays = true
            (0 until buttons.size).forEach { i ->
                buttons[i].isChecked = (value and (1 shl i)) != 0
            }
            settingSelectedDays = false
            onSelectedDaysChangedListener?.invoke(selectedDaysBitmask)
        }

    private val buttons: List<MaterialButton>
    private var settingSelectedDays = false
    private var onSelectedDaysChangedListener: ((Int) -> Unit)? = null

    init {
        val inflater = LayoutInflater.from(context)
        buttons = context.resources.getStringArray(R.array.day_short_names).map { day ->
            DaysOfWeekButtonBinding.inflate(inflater, this, false).root.apply {
                text = day
            }
        }
        buttons.forEach {
            addView(it)
            it.addOnCheckedChangeListener { _, _ ->
                if (!settingSelectedDays) {
                    onSelectedDaysChangedListener?.invoke(selectedDaysBitmask)
                }
            }
        }
    }

    fun setOnSelectedDaysChangedListener(listener: (Int) -> Unit) {
        onSelectedDaysChangedListener = listener
    }
}