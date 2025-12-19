package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ServerStatus
import timber.log.Timber
import java.util.*

class ServerStatusDataSource(private val db: AppRoomDatabase) : DataSourceInterface<ServerStatus> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val liveDataActiveItem: LiveData<ServerStatus?>
        get() = db.serverStatusDao.loadActiveServerStatus()

    val activeItem: ServerStatus
        get() = runBlocking(Dispatchers.IO) {
            val activeStatus = db.serverStatusDao.loadActiveServerStatusSync()
            if (activeStatus != null) {
                activeStatus
            } else {
                Timber.d("Active server status is null")
                val connection = db.connectionDao.loadActiveConnectionSync()
                val serverStatus = ServerStatus()
                serverStatus.serverName = "Unknown"
                serverStatus.serverVersion = "Unknown"
                if (connection != null) {
                    Timber.d("Loaded active connection for empty server status")
                    serverStatus.connectionId = connection.id
                    serverStatus.connectionName = connection.name
                    Timber.d("Inserting new server status information for connection ${connection.name}")
                    db.serverStatusDao.insert(serverStatus)
                }
                serverStatus
            }
        }

    override fun addItem(item: ServerStatus) {
        ioScope.launch { db.serverStatusDao.insert(item) }
    }

    override fun updateItem(item: ServerStatus) {
        ioScope.launch { db.serverStatusDao.update(item) }
    }

    override fun removeItem(item: ServerStatus) {
        ioScope.launch { db.serverStatusDao.delete(item) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> = db.serverStatusDao.serverStatusCount

    override fun getLiveDataItems(): LiveData<List<ServerStatus>> = db.serverStatusDao.loadAllServerStatus()

    override fun getLiveDataItemById(id: Any): LiveData<ServerStatus> = db.serverStatusDao.loadServerStatusById(id as Int)

    override fun getItemById(id: Any): ServerStatus? = runBlocking(Dispatchers.IO) {
        db.serverStatusDao.loadServerStatusByIdSync(id as Int)
    }

    override fun getItems(): List<ServerStatus> {
        return ArrayList()
    }
}
