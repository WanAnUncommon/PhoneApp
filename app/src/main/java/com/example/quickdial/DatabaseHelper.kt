import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "QuickDialDB"
        private const val DATABASE_VERSION = 1
        private const val TABLE_CONTACTS = "contacts"
        
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_PHONE = "phone"
        private const val KEY_AVATAR = "avatar"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_CONTACTS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_NAME TEXT,
                $KEY_PHONE TEXT,
                $KEY_AVATAR TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONTACTS")
        onCreate(db)
    }

    fun addContact(contact: Contact): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, contact.name)
            put(KEY_PHONE, contact.phoneNumber)
            put(KEY_AVATAR, contact.avatarUri)
        }
        return db.insert(TABLE_CONTACTS, null, values)
    }

    fun getAllContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_CONTACTS, null, null, null, null, null, null)

        with(cursor) {
            while (moveToNext()) {
                contacts.add(
                    Contact(
                        getLong(getColumnIndexOrThrow(KEY_ID)),
                        getString(getColumnIndexOrThrow(KEY_NAME)),
                        getString(getColumnIndexOrThrow(KEY_PHONE)),
                        getString(getColumnIndexOrThrow(KEY_AVATAR))
                    )
                )
            }
        }
        cursor.close()
        return contacts
    }

    fun updateContact(contact: Contact): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, contact.name)
            put(KEY_PHONE, contact.phoneNumber)
            put(KEY_AVATAR, contact.avatarUri)
        }
        return db.update(TABLE_CONTACTS, values, "$KEY_ID = ?", arrayOf(contact.id.toString()))
    }

    fun getContact(id: Long): Contact? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_CONTACTS,
            null,
            "$KEY_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                Contact(
                    it.getLong(it.getColumnIndexOrThrow(KEY_ID)),
                    it.getString(it.getColumnIndexOrThrow(KEY_NAME)),
                    it.getString(it.getColumnIndexOrThrow(KEY_PHONE)),
                    it.getString(it.getColumnIndexOrThrow(KEY_AVATAR))
                )
            } else null
        }
    }

    fun deleteContact(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_CONTACTS, "$KEY_ID = ?", arrayOf(id.toString()))
    }
} 