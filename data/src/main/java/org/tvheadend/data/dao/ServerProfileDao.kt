package org.tvheadend.data.dao

import androidx.room.*
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.ServerProfile.Companion.HTSP_PROFILE
import org.tvheadend.data.entity.ServerProfile.Companion.HTTP_PROFILE
import org.tvheadend.data.entity.ServerProfile.Companion.RECORDING_PROFILE

@Dao
interface ServerProfileDao {

    @get:Query("SELECT COUNT (*) FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCountSync: Int

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.type = '" + HTSP_PROFILE + "'")
    fun loadHtspPlaybackProfilesSync(): List<ServerProfile>

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.type = '" + HTTP_PROFILE + "'")
    fun loadHttpPlaybackProfilesSync(): List<ServerProfile>

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.type = '" + RECORDING_PROFILE + "'")
    fun loadAllRecordingProfilesSync(): List<ServerProfile>

    @Insert
    fun insert(serverProfile: ServerProfile)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(serverProfiles: List<ServerProfile>)

    @Update
    fun update(serverProfile: ServerProfile)

    @Delete
    fun delete(serverProfile: ServerProfile)

    @Query("DELETE FROM server_profiles")
    fun deleteAll()

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.id = :id")
    fun loadProfileByIdSync(id: Int): ServerProfile?

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.uuid = :uuid")
    fun loadProfileByUuidSync(uuid: String): ServerProfile?

    companion object {

        const val CONNECTION_IS_ACTIVE = " p.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }
}
