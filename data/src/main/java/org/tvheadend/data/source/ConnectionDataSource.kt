package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.ServerStatus
import timber.log.Timber

class ConnectionDataSource(private val db: AppRoomDatabase) : DataSourceInterface<Connection> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val liveDataActiveItem: LiveData<Connection?> get() = db.connectionDao.loadActiveConnection()

    val activeItem: Connection get() = runBlocking(Dispatchers.IO) {
        val c = db.connectionDao.loadActiveConnectionSync() ?: Connection().also { it.id = -1 }
        Timber.d("Returning active connection ${c.name} with id ${c.id}")
        c
    }

    override fun addItem(item: Connection) {
        ioScope.launch {
            if (item.isActive) {
                db.connectionDao.disableActiveConnection()
            }
            val newId = db.connectionDao.insert(item)
            // Create a new server status row in the database
            // that is linked to the newly added connection
            val serverStatus = ServerStatus()
            serverStatus.connectionId = newId.toInt()
            db.serverStatusDao.insert(serverStatus)
        }
    }

    override fun updateItem(item: Connection) {
        ioScope.launch {
            if (item.isActive) {
                db.connectionDao.disableActiveConnection()
            }
            db.connectionDao.update(item)
        }
    }

    override fun removeItem(item: Connection) {
        ioScope.launch {
            db.connectionDao.delete(item)
            db.serverStatusDao.deleteByConnectionId(item.id)
        }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.connectionDao.connectionCount
    }

    override fun getLiveDataItems(): LiveData<List<Connection>> = db.connectionDao.loadAllConnections()

    override fun getLiveDataItemById(id: Any): LiveData<Connection> = db.connectionDao.loadConnectionById(id as Int)

    override fun getItemById(id: Any): Connection? = runBlocking(Dispatchers.IO) {
        db.connectionDao.loadConnectionByIdSync(id as Int)
    }

    override fun getItems(): List<Connection> = runBlocking(Dispatchers.IO) {
        db.connectionDao.loadAllConnectionsSync()
    }

    fun setSyncRequiredForActiveConnection() {
        val connection = activeItem
        if (connection.id >= 0) {
            connection.isSyncRequired = true
            connection.lastUpdate = 0
            updateItem(connection)
        }
    }

    fun switchActiveConnection(oldId: Int, newId: Int) {
        Timber.d("Switching active connection from id $oldId to $newId")
        runBlocking(Dispatchers.IO) {
            db.connectionDao.loadConnectionByIdSync(oldId)?.also {
                Timber.d("Currently active connection is ${it.name} with id ${it.id}")
                it.isActive = false
                db.connectionDao.update(it)
            }
            db.connectionDao.loadConnectionByIdSync(newId)?.also {
                Timber.d("New active connection shall be ${it.name} with id ${it.id}")
                it.isActive = true
                it.isSyncRequired = true
                it.lastUpdate = 0
                db.connectionDao.update(it)
            }
            db.connectionDao.loadActiveConnectionSync()?.let {
                Timber.d("New active connection is be ${it.name} with id ${it.id}")
            }
        }
    }
}
