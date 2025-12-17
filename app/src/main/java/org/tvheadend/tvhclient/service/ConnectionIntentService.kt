package org.tvheadend.tvhclient.service

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.service.htsp.HtspIntentServiceHandler
import org.tvheadend.tvhclient.util.extensions.connectionDataSource
import timber.log.Timber

class ConnectionIntentService : JobIntentService() {
    private lateinit var connection: Connection
    private lateinit var serviceHandler: ServiceInterface

    override fun onCreate() {
        super.onCreate()

        Timber.d("Starting intent service")
        connection = connectionDataSource.activeItem
        serviceHandler = HtspIntentServiceHandler(this, connection)
    }

    override fun onHandleWork(intent: Intent) {
        serviceHandler.onHandleWork(intent)
    }

    override fun onDestroy() {
        Timber.d("Stopping service")
        serviceHandler.onDestroy()
    }

    companion object {
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, ConnectionIntentService::class.java, 1, work)
        }
    }

    interface ServiceInterface {
        fun onHandleWork(intent: Intent)
        fun onDestroy()
    }
}
