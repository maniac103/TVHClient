package org.tvheadend.tvhclient

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.google.android.material.color.DynamicColors
import org.tvheadend.data.AppRepository
import org.tvheadend.data.di.DaggerRepositoryComponent
import org.tvheadend.data.di.RepositoryModule
import org.tvheadend.tvhclient.di.component.DaggerMainComponent
import org.tvheadend.tvhclient.di.component.MainComponent
import org.tvheadend.tvhclient.di.module.ContextModule
import org.tvheadend.tvhclient.di.module.SharedPreferencesModule
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.ui.features.playback.external.ExpandedControlsActivity
import org.tvheadend.tvhclient.util.MigrateUtils
import org.tvheadend.tvhclient.util.logging.DebugTree
import org.tvheadend.tvhclient.util.logging.FileLoggingTree
import timber.log.Timber
import javax.inject.Inject

// TODO snackbar locale changes
// TODO when a notification is dismissed, it reappears when the recording gets updated,
//  save the dismissed id in the viewmodel and don't add another notification if the id was already dismissed

class MainApplication : MultiDexApplication(), OptionsProvider, SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)

        // Create the repository component which is then be used for the dependency injection.
        val repositoryComponent = DaggerRepositoryComponent
                .builder()
                .repositoryModule(RepositoryModule(applicationContext))
                .build()

        // Setup the required modules and components for dependency injection
        component = DaggerMainComponent
                .builder()
                .contextModule(ContextModule(applicationContext))
                .sharedPreferencesModule(SharedPreferencesModule())
                .repositoryComponent(repositoryComponent)
                .build()
        component.inject(this)

        instance = this

        // Initialize the logging. Log to the console only when in debug mode.
        Timber.plant(DebugTree())

        // Log to a file when in release mode and the user has activated the setting
        if (!BuildConfig.DEBUG && sharedPreferences.getBoolean("debug_mode_enabled", resources.getBoolean(R.bool.pref_default_debug_mode_enabled))) {
            Timber.plant(FileLoggingTree(applicationContext))
        }

        Timber.d("Application build time is ${BuildConfig.BUILD_TIME}, git commit hash is ${BuildConfig.GIT_SHA}")

        // Execute some additional tasks before starting the application.
        // These tasks are for example migrating connections, updating or
        // removing preferences, removing old information from the database and others
        MigrateUtils(applicationContext, appRepository, sharedPreferences).doMigrate()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updateDefaultNightMode()
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    /**
     * Provides CastOptions, which affects discovery and session management of a Cast device
     *
     * @param context Required context object
     * @return CastOptions
     */
    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
                .setTargetActivityClassName(ExpandedControlsActivity::class.java.name)
                .build()
        val mediaOptions = CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .setExpandedControllerActivityClassName(ExpandedControlsActivity::class.java.name)
                .build()

        return CastOptions.Builder()
                .setReceiverApplicationId(BuildConfig.CAST_ID)
                .setCastMediaOptions(mediaOptions)
                .build()
    }

    /**
     * Provides a list of custom SessionProvider instances for non-Cast devices.
     *
     * @param context Required context object
     * @return List of session providers
     */
    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        when (key) {
            "selected_theme" -> updateDefaultNightMode()
        }
    }

    private fun updateDefaultNightMode() {
        val selected = sharedPreferences.getString("selected_theme", getString(R.string.pref_default_theme))
        val mode = when (selected) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ->
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    companion object {

        lateinit var instance: MainApplication
        lateinit var component: MainComponent
    }
}
