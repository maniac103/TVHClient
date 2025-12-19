package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ChannelTag

class ChannelTagDataSource(private val db: AppRoomDatabase) : DataSourceInterface<ChannelTag> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val liveDataSelectedItemIds get() = db.channelTagDao.loadAllSelectedItemIds()

    val itemCount: Int
        get() {
            var count: Int
            runBlocking(Dispatchers.IO) {
                count = db.channelTagDao.itemCountSync
            }
            return count
        }

    override fun addItem(item: ChannelTag) {
        ioScope.launch { db.channelTagDao.insert(item) }
    }

    fun addItems(items: List<ChannelTag>) {
        ioScope.launch { db.channelTagDao.insert(items) }
    }

    override fun updateItem(item: ChannelTag) {
        ioScope.launch { db.channelTagDao.update(item) }
    }

    override fun removeItem(item: ChannelTag) {
        ioScope.launch { db.channelTagDao.delete(item) }
    }

    fun updateSelectedChannelTags(ids: Set<Int>) {
        ioScope.launch {
            val channelTags = db.channelTagDao.loadAllChannelTagsSync()
            channelTags.forEach { tag ->
                tag.isSelected = ids.contains(tag.tagId)
            }
            db.channelTagDao.update(channelTags)
        }
    }


    override fun getLiveDataItemCount(): LiveData<Int> {
        return MutableLiveData()
    }

    override fun getLiveDataItems(): LiveData<List<ChannelTag>> = db.channelTagDao.loadAllChannelTags()

    override fun getLiveDataItemById(id: Any): LiveData<ChannelTag> {
        return MutableLiveData()
    }

    override fun getItemById(id: Any): ChannelTag? = runBlocking(Dispatchers.IO) {
        db.channelTagDao.loadChannelTagByIdSync(id as Int)
    }

    override fun getItems(): List<ChannelTag> = runBlocking(Dispatchers.IO) {
        db.channelTagDao.loadAllChannelTagsSync()
    }

    fun getNonEmptyItems(loadAll: Boolean = true): List<ChannelTag> = runBlocking(Dispatchers.IO) {
        if (loadAll) {
            db.channelTagDao.loadAllChannelTagsSync()
        } else {
            db.channelTagDao.loadOnlyNonEmptyChannelTagsSync()
        }
    }
}
