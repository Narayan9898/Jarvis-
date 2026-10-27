package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.WorkflowDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.WorkflowEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ChatMessageEntity::class,
        ReminderEntity::class,
        WorkflowEntity::class,
        MemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun reminderDao(): ReminderDao
    abstract fun workflowDao(): WorkflowDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_ai_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: JarvisDatabase) {
                // Initial Default Workflows
                val workflowDao = database.workflowDao()
                val initialWorkflows = listOf(
                    WorkflowEntity(
                        name = "Good Morning Routine",
                        triggerPhrase = "Good morning JARVIS",
                        description = "Greets, announces date, checks alarms, and prepares for the day",
                        stepsJson = """[
                            {"type":"speak","text":"Good morning! Systems are online and all parameters normal."},
                            {"type":"date"},
                            {"type":"reminders"}
                        ]"""
                    ),
                    WorkflowEntity(
                        name = "Going to Sleep",
                        triggerPhrase = "I am going to sleep",
                        description = "Reviews tomorrow's agenda and preps night rest",
                        stepsJson = """[
                            {"type":"speak","text":"Entering night standby mode. Reviewing pending reminders."},
                            {"type":"reminders"},
                            {"type":"speak","text":"Good night. All alerts armed."}
                        ]"""
                    ),
                    WorkflowEntity(
                        name = "Focus / Work Mode",
                        triggerPhrase = "Activate focus mode",
                        description = "Silences non-essential distractions and displays task list",
                        stepsJson = """[
                            {"type":"speak","text":"Focus protocol activated. Clear vision ahead."}
                        ]"""
                    )
                )
                workflowDao.insertWorkflows(initialWorkflows)

                // Initial Memory
                val memoryDao = database.memoryDao()
                memoryDao.insertOrUpdateMemory(
                    MemoryEntity(
                        category = "PREFERENCE",
                        key = "assistant_name",
                        value = "JARVIS"
                    )
                )
                memoryDao.insertOrUpdateMemory(
                    MemoryEntity(
                        category = "STYLE",
                        key = "personality",
                        value = "Professional, witty, precise"
                    )
                )
                memoryDao.insertOrUpdateMemory(
                    MemoryEntity(
                        category = "PREFERENCE",
                        key = "preferred_language",
                        value = "auto"
                    )
                )
            }
        }
    }
}
