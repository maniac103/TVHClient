package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ServerProfile
import java.util.*

class ServerProfileDataSource(private val db: AppRoomDatabase) : DataSourceInterface<ServerProfile, ServerProfile> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val recordingProfileNames: Array<String>
        get() = getProfileNames(recordingProfiles)

    val recordingProfiles: List<ServerProfile>
        get() = runBlocking(Dispatchers.IO) {
            db.serverProfileDao.loadAllRecordingProfilesSync()
        }

    val htspPlaybackProfiles: List<ServerProfile>
        get() = runBlocking(Dispatchers.IO) {
            db.serverProfileDao.loadHtspPlaybackProfilesSync()
        }

    val httpPlaybackProfiles: List<ServerProfile>
        get() = runBlocking(Dispatchers.IO) {
            db.serverProfileDao.loadHttpPlaybackProfilesSync()
        }

    override fun addItem(item: ServerProfile) {
        ioScope.launch { db.serverProfileDao.insert(item) }
    }

    fun addItems(items: List<ServerProfile>) {
        ioScope.launch { db.serverProfileDao.insert(items) }
    }

    override fun updateItem(item: ServerProfile) {
        ioScope.launch { db.serverProfileDao.update(item) }
    }

    override fun removeItem(item: ServerProfile) {
        ioScope.launch { db.serverProfileDao.delete(item) }
    }

    fun removeAll() {
        ioScope.launch { db.serverProfileDao.deleteAll() }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return MutableLiveData()
    }

    override fun getLiveDataItems(): LiveData<List<ServerProfile>> {
        return MutableLiveData()
    }

    override fun getLiveDataItemById(id: Any): LiveData<ServerProfile?> {
        return MutableLiveData()
    }

    override fun getItemById(id: Any): ServerProfile? = runBlocking(Dispatchers.IO) {
        when (id) {
            is Int -> db.serverProfileDao.loadProfileByIdSync(id)
            is String -> db.serverProfileDao.loadProfileByUuidSync(id)
            else -> null
        }
    }

    override fun getItems(): List<ServerProfile> {
        return ArrayList()
    }

    private fun getProfileNames(serverProfiles: List<ServerProfile>) = serverProfiles
        .map { it.name ?: "" }
        .toTypedArray()
}
