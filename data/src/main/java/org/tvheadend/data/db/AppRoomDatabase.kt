package org.tvheadend.data.db

import android.content.Context
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.tvheadend.data.dao.*
import org.tvheadend.data.entity.*

@Database(
        entities = [
            TimerRecording::class,
            SeriesRecording::class,
            Recording::class,
            Program::class,
            Channel::class,
            ChannelTag::class,
            TagAndChannel::class,
            Connection::class,
            ServerProfile::class,
            ServerStatus::class],
        exportSchema = false,
        version = 15)
abstract class AppRoomDatabase : RoomDatabase() {

    internal abstract val timerRecordingDao: TimerRecordingDao

    internal abstract val seriesRecordingDao: SeriesRecordingDao

    internal abstract val recordingDao: RecordingDao

    internal abstract val channelDao: ChannelDao

    internal abstract val channelTagDao: ChannelTagDao

    internal abstract val tagAndChannelDao: TagAndChannelDao

    internal abstract val programDao: ProgramDao

    internal abstract val connectionDao: ConnectionDao

    internal abstract val serverProfileDao: ServerProfileDao

    internal abstract val serverStatusDao: ServerStatusDao

    companion object {

        private var instance: AppRoomDatabase? = null

        fun getInstance(context: Context): AppRoomDatabase {
            if (instance == null) {
                synchronized(AppRoomDatabase::class.java) {
                    instance = Room.databaseBuilder(context, AppRoomDatabase::class.java, "tvhclient")
                        .addMigrations(MIGRATION_1_2)
                        .addMigrations(MIGRATION_2_3)
                        .addMigrations(MIGRATION_3_4)
                        .addMigrations(MIGRATION_4_5)
                        .addMigrations(MIGRATION_5_6)
                        .addMigrations(MIGRATION_6_7)
                        .addMigrations(MIGRATION_7_8)
                        .addMigrations(MIGRATION_8_9)
                        .addMigrations(MIGRATION_9_10)
                        .addMigrations(MIGRATION_10_11)
                        .addMigrations(MIGRATION_11_12)
                        .addMigrations(MIGRATION_12_13)
                        .addMigrations(MIGRATION_13_14)
                        .addMigrations(MIGRATION_14_15)
                        .build()
                }
            }
            return instance!!
        }


        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE connections ADD COLUMN last_update INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE connections ADD COLUMN sync_required INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE channels ADD COLUMN display_number INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE recordings ADD COLUMN duplicate INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE server_status ADD COLUMN http_playback_server_profile_id INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE recordings ADD COLUMN image TEXT;")
                database.execSQL("ALTER TABLE recordings ADD COLUMN fanart_image TEXT;")
                database.execSQL("ALTER TABLE recordings ADD COLUMN copyright_year INTEGER NOT NULL DEFAULT 0;")
                database.execSQL("ALTER TABLE recordings ADD COLUMN removal INTEGER NOT NULL DEFAULT 0;")

                database.execSQL("ALTER TABLE timer_recordings ADD COLUMN removal INTEGER NOT NULL DEFAULT 0;")
                database.execSQL("ALTER TABLE series_recordings ADD COLUMN removal INTEGER NOT NULL DEFAULT 0;")

                database.execSQL("ALTER TABLE series_recordings ADD COLUMN max_count INTEGER NOT NULL DEFAULT 0;")

                database.execSQL("ALTER TABLE programs ADD COLUMN credits TEXT;")
                database.execSQL("ALTER TABLE programs ADD COLUMN category TEXT;")
                database.execSQL("ALTER TABLE programs ADD COLUMN keyword TEXT;")
                database.execSQL("ALTER TABLE programs ADD COLUMN series_link_uri TEXT;")
                database.execSQL("ALTER TABLE programs ADD COLUMN episode_uri TEXT;")
                database.execSQL("ALTER TABLE programs ADD COLUMN copyright_year INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE channel_tags ADD COLUMN is_selected INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX index_programs_start ON programs(start)")
                database.execSQL("CREATE INDEX index_programs_channel_id ON programs(channel_id)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE channels ADD COLUMN server_order INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE connections ADD COLUMN server_url TEXT;")
                database.execSQL("ALTER TABLE connections ADD COLUMN streaming_url TEXT;")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE programs ADD COLUMN modified_time INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE recordings ADD COLUMN duration INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE server_status ADD COLUMN series_recording_server_profile_id INTEGER NOT NULL DEFAULT 0;")
                database.execSQL("ALTER TABLE server_status ADD COLUMN timer_recording_server_profile_id INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.removeColumns(
                    "channels",
                    listOf(
                        "display_number",
                        "program_id",
                        "program_title",
                        "program_subtitle",
                        "program_start",
                        "program_stop",
                        "program_content_type",
                        "next_program_id",
                        "next_program_title"
                    ),
                    listOf("id", "connection_id")
                )
                database.removeColumns(
                    "programs",
                    listOf(
                        "channel_name",
                        "channel_icon"
                    ),
                    listOf("id", "connection_id")
                )
                database.removeColumns(
                    "recordings",
                    listOf(
                        "channel_name",
                        "channel_icon"
                    ),
                    listOf("id", "connection_id")
                )
                database.removeColumns(
                    "series_recordings",
                    listOf(
                        "channel_name",
                        "channel_icon"
                    ),
                    listOf("id", "connection_id")
                )
                database.removeColumns(
                    "timer_recordings",
                    listOf(
                        "channel_name",
                        "channel_icon"
                    ),
                    listOf("id", "connection_id")
                )
            }
        }

        private fun SupportSQLiteDatabase.removeColumns(table: String, columns: List<String>, primaryKeys: List<String>) {
            val newTableStr = StringBuilder()
            val valuesStr = StringBuilder()
            query("PRAGMA table_info($table)").use { columnInfo ->
                val nameIndex = columnInfo.getColumnIndexOrThrow("name")
                val typeIndex = columnInfo.getColumnIndexOrThrow("type")
                val nullableIndex = columnInfo.getColumnIndexOrThrow("notnull")
                while (columnInfo.moveToNext()) {
                    val name = columnInfo.getStringOrNull(nameIndex)
                    val type = columnInfo.getStringOrNull(typeIndex)
                    val isNullable = columnInfo.getIntOrNull(nullableIndex)
                    if (name !in columns && name != null && type != null && isNullable != null) {
                        newTableStr.append("`$name` $type ${if (isNullable == 0) "" else "NOT NULL"},")
                        valuesStr.append("`$name`,")
                    }
                }
            }
            val primaryKeysString = primaryKeys.joinToString(", ") { "`$it`" }
            execSQL("CREATE TABLE IF NOT EXISTS ${table}_new (${newTableStr} PRIMARY KEY($primaryKeysString)) ")
            execSQL("INSERT INTO ${table}_new SELECT ${valuesStr.dropLast(1)} FROM $table")
            execSQL("DROP TABLE $table")
            execSQL("ALTER TABLE ${table}_new RENAME TO $table")
        }
    }
}
