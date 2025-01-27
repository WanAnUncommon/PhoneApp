package com.example.quickdial

import Contact
import DatabaseHelper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class EditContactActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var nameEdit: EditText
    private lateinit var phoneEdit: EditText
    private var selectedImageUri: Uri? = null
    private val pickImage = 100
    private var contactId: Long = -1
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contact)

        dbHelper = DatabaseHelper(this)
        imageView = findViewById(R.id.edit_avatar)
        nameEdit = findViewById(R.id.edit_name)
        phoneEdit = findViewById(R.id.edit_phone)

        // 获取联系人ID，如果有的话
        contactId = intent.getLongExtra("contact_id", -1)

        // 如果是编辑现有联系人
        if (contactId != -1L) {
            loadContact()
        } else {
            // 设置默认头像
            imageView.setImageResource(R.drawable.default_avatar)
        }

        imageView.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }

        findViewById<Button>(R.id.save_button).setOnClickListener {
            saveContact()
        }
    }

    private fun loadContact() {
        dbHelper.getContact(contactId)?.let { contact ->
            nameEdit.setText(contact.name)
            phoneEdit.setText(contact.phoneNumber)
            if (contact.avatarUri != null) {
                try {
                    selectedImageUri = Uri.parse(contact.avatarUri)
                    imageView.setImageURI(selectedImageUri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    imageView.setImageResource(R.drawable.default_avatar)
                }
            } else {
                imageView.setImageResource(R.drawable.default_avatar)
            }
        }
    }

    private fun saveContact() {
        val name = nameEdit.text.toString().trim()
        val phone = phoneEdit.text.toString().trim()
        
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "请填写姓名和电话", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 如果选择了新图片，复制到应用私有目录
            val finalUri = selectedImageUri?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                val fileName = "avatar_${System.currentTimeMillis()}.jpg"
                val file = File(getExternalFilesDir(null), fileName)
                
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                Uri.fromFile(file).toString()
            }

            val contact = Contact(
                id = contactId,
                name = name,
                phoneNumber = phone,
                avatarUri = finalUri ?: selectedImageUri?.toString()
            )

            if (contactId == -1L) {
                dbHelper.addContact(contact)
            } else {
                dbHelper.updateContact(contact)
            }
            
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            selectedImageUri = data?.data
            try {
                imageView.setImageURI(selectedImageUri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show()
                imageView.setImageResource(R.drawable.default_avatar)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
} 