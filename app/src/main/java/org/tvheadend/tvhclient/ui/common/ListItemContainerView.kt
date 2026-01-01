package org.tvheadend.tvhclient.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.ViewOutlineProvider
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import org.tvheadend.tvhclient.R

class ListItemContainerView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private val backgroundShape = MaterialShapeDrawable()
    private val selectionTintList: ColorStateList
    private val smallCornerSize = 4f.dp()
    private val largeCornerSize = 16f.dp()

    enum class Role {
        NormalItem,
        SingleItem,
        FirstItem,
        LastItem
    }

    var role: Role = Role.SingleItem
        set(value) {
            if (value != field) {
                field = value
                updateBackground()
            }
        }

    var isSelectedPosition: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                backgroundShape.tintList = if (value) selectionTintList else null
            }
        }

    init {
        backgroundShape.fillColor = ColorStateList.valueOf(
            MaterialColors.getColor(this, R.attr.colorSurfaceContainerLow)
        )
        updateBackground()

        val selectionColor = MaterialColors.compositeARGBWithAlpha(
            MaterialColors.getColor(this, R.attr.colorPrimaryContainer),
            80
        )
        selectionTintList = ColorStateList.valueOf(selectionColor)

        background = backgroundShape
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
    }

    fun assignRole(position: Int, totalItemCount: Int, selected: Boolean) {
        role = when {
            totalItemCount == 1 -> Role.SingleItem
            position == 0 -> Role.FirstItem
            position == totalItemCount - 1 -> Role.LastItem
            else -> Role.NormalItem
        }
        isSelectedPosition = selected
    }

    private fun updateBackground() {
        val topCornerSize = when (role) {
            Role.NormalItem -> smallCornerSize
            Role.SingleItem -> largeCornerSize
            Role.FirstItem -> largeCornerSize
            Role.LastItem -> smallCornerSize
        }
        val bottomCornerSize = when (role) {
            Role.NormalItem -> smallCornerSize
            Role.SingleItem -> largeCornerSize
            Role.FirstItem -> smallCornerSize
            Role.LastItem -> largeCornerSize
        }
        backgroundShape.shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setTopLeftCornerSize(topCornerSize)
            .setTopRightCornerSize(topCornerSize)
            .setBottomLeftCornerSize(bottomCornerSize)
            .setBottomRightCornerSize(bottomCornerSize)
            .build()
    }

    private fun Float.dp() = context.resources.displayMetrics.density * this
}