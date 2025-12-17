package org.tvheadend.tvhclient.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.service.htsp.HtspServiceHandler
import org.tvheadend.tvhclient.util.extensions.connectionDataSource
import timber.log.Timber

class ConnectionService : Service() {
    private lateinit var connection: Connection
    private lateinit var serviceHandler: ServiceInterface

    override fun onCreate() {
        Timber.d("Starting service")

        connection = connectionDataSource.activeItem
        serviceHandler = HtspServiceHandler(this, connection)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return serviceHandler.onStartCommand(intent)
    }

    override fun onDestroy() {
        serviceHandler.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    interface ServiceInterface {
        fun onStartCommand(intent: Intent?): Int
        fun onDestroy()
    }
}
