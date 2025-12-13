package org.tvheadend.tvhclient.ui.base

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ThemeUtils
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.internal.EdgeToEdgeUtils
import com.google.android.material.shape.MaterialShapeDrawable
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.util.getThemeId

abstract class BaseActivity : AppCompatActivity(), ToolbarInterface {
    private lateinit var toolbar: Toolbar

    protected lateinit var sharedPreferences: SharedPreferences
    protected lateinit var baseViewModel: BaseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        baseViewModel = ViewModelProvider(this)[BaseViewModel::class.java]
    }

    protected fun setupToolbar(toolbar: Toolbar, appBarLayout: AppBarLayout, contentView: View) {
        this.toolbar = toolbar
        setSupportActionBar(toolbar)
        enableDrawingBehindStatusBar(toolbar, appBarLayout, contentView)
    }

    override fun setTitle(title: String) {
        toolbar.title = title
    }

    override fun setSubtitle(subtitle: String?) {
        toolbar.subtitle = subtitle
    }

    private fun enableDrawingBehindStatusBar(toolbar: Toolbar, appBarLayout: AppBarLayout, contentView: View) {
        val appBarBackgroundColor = ColorStateList.valueOf(ThemeUtils.getThemeAttrColor(this, R.attr.toolbarColorPrimary))
        val appBarBackground = MaterialShapeDrawable.createWithElevationOverlay(this, 0.0f, appBarBackgroundColor)
        appBarLayout.statusBarForeground = appBarBackground

        EdgeToEdgeUtils.applyEdgeToEdge(window, true)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { _, insets ->
            val insetsType = WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars() or
                    WindowInsetsCompat.Type.displayCutout()
            val relevantInsets = insets.getInsets(insetsType)
            appBarLayout.updatePadding(top = relevantInsets.top)
            contentView.updatePadding(bottom = relevantInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

}