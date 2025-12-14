package org.tvheadend.tvhclient.ui.features.programs

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.MiscContentActivityBinding
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

class ProgramDetailsActivity : BaseActivity(), LayoutControlInterface {
    private lateinit var binding: MiscContentActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MiscContentActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, binding.appBar)

        if (savedInstanceState == null) {
            val fragment = ProgramDetailsFragment.newInstance(
                    intent.getIntExtra("eventId", 0),
                    intent.getIntExtra("channelId", 0))
            supportFragmentManager.beginTransaction().add(R.id.main, fragment).commit()
        }
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun enableSingleScreenLayout() {
        Timber.d("Dual pane is not active, hiding details layout")
        val mainFrameLayout: FrameLayout = findViewById(R.id.main)
        val detailsFrameLayout: FrameLayout? = findViewById(R.id.details)
        detailsFrameLayout?.gone()
        mainFrameLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f)
    }

    override fun enableDualScreenLayout() {
        Timber.d("Dual pane is active, showing details layout")
        val mainFrameLayout: FrameLayout = findViewById(R.id.main)
        val detailsFrameLayout: FrameLayout? = findViewById(R.id.details)
        detailsFrameLayout?.visible()
        mainFrameLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.65f)
    }

    override fun forceSingleScreenLayout() {
        enableSingleScreenLayout()
    }
}
