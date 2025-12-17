package org.tvheadend.tvhclient.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.internal.EdgeToEdgeUtils
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface

abstract class BaseActivity : AppCompatActivity(), ToolbarInterface {
    private lateinit var toolbar: Toolbar
    protected lateinit var baseViewModel: BaseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseViewModel = ViewModelProvider(this)[BaseViewModel::class.java]
    }

    protected fun setupToolbar(toolbar: Toolbar, appBarLayout: AppBarLayout) {
        this.toolbar = toolbar
        setSupportActionBar(toolbar)
        enableDrawingBehindStatusBar(toolbar, appBarLayout)
    }

    override fun setTitle(title: String) {
        toolbar.title = title
    }

    override fun setSubtitle(subtitle: String?) {
        toolbar.subtitle = subtitle
    }

    private fun enableDrawingBehindStatusBar(toolbar: Toolbar, appBarLayout: AppBarLayout) {
        EdgeToEdgeUtils.applyEdgeToEdge(window, true)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { _, insets ->
            val insetsType = WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars() or
                    WindowInsetsCompat.Type.displayCutout()
            val relevantInsets = insets.getInsets(insetsType)
            appBarLayout.updatePadding(top = relevantInsets.top)
            WindowInsetsCompat.CONSUMED
        }
    }

}