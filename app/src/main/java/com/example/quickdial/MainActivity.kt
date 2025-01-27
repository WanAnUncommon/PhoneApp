package com.example.quickdial

import Contact
import DatabaseHelper
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var gridLayout: androidx.gridlayout.widget.GridLayout
    private lateinit var dbHelper: DatabaseHelper
    private val ADD_CONTACT_REQUEST = 1
    private val EDIT_CONTACT_REQUEST = 2
    private val SETTINGS_REQUEST = 3
    private val PERMISSIONS_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // 初始化视图和数据库
            gridLayout = findViewById(R.id.grid_layout)
            
            // 设置GridLayout的列数和自动换行
            gridLayout.columnCount = 3
            gridLayout.alignmentMode = androidx.gridlayout.widget.GridLayout.ALIGN_MARGINS
            gridLayout.useDefaultMargins = true

            dbHelper = DatabaseHelper(this)

            // 检查权限
            checkPermissions()

            // 添加联系人按钮
            findViewById<FloatingActionButton>(R.id.fab_add)?.setOnClickListener {
                val intent = Intent(this, EditContactActivity::class.java)
                startActivityForResult(intent, ADD_CONTACT_REQUEST)
            }

            loadContacts()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun loadContacts() {
        try {
            gridLayout.removeAllViews()
            val contacts = dbHelper.getAllContacts()

            // 获取屏幕宽度
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels

            // 获取保存的设置
            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            val avatarSize = prefs.getInt("avatar_size", 80)
            val textSize = prefs.getInt("text_size", 16)

            var currentColumn = 0

            contacts.forEach { contact ->
                val contactView = layoutInflater.inflate(R.layout.contact_item, gridLayout, false)
                
                val imageView = contactView.findViewById<ImageView>(R.id.contact_image)
                val textView = contactView.findViewById<TextView>(R.id.contact_name)

                // 设置头像大小
                val imageLayoutParams = imageView.layoutParams
                imageLayoutParams.width = dpToPx(avatarSize)
                imageLayoutParams.height = dpToPx(avatarSize)
                imageView.layoutParams = imageLayoutParams

                // 设置文字大小
                textView.textSize = textSize.toFloat()

                // 设置头像
                if (contact.avatarUri != null) {
                    try {
                        imageView.setImageURI(Uri.parse(contact.avatarUri))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        imageView.setImageResource(R.drawable.default_avatar)
                    }
                } else {
                    imageView.setImageResource(R.drawable.default_avatar)
                }

                textView.text = contact.name

                // 设置事件监听器
                imageView.apply {
                    setOnClickListener {
                        showCallDialog(contact)
                    }
                    
                    setOnLongClickListener {
                        showEditDialog(contact)
                        true
                    }
                }

                contactView.setOnLongClickListener {
                    showEditDialog(contact)
                    true
                }

                // 设置GridLayout的布局参数
                val params = androidx.gridlayout.widget.GridLayout.LayoutParams()
                params.width = androidx.gridlayout.widget.GridLayout.LayoutParams.WRAP_CONTENT
                params.height = androidx.gridlayout.widget.GridLayout.LayoutParams.WRAP_CONTENT
                
                // 计算当前项应该在哪一行和列
                val itemWidth = dpToPx(avatarSize + 32) // 头像大小加上padding
                if (currentColumn * itemWidth > screenWidth - itemWidth) {
                    currentColumn = 0 // 换到新的一行
                }
                
                params.rowSpec = androidx.gridlayout.widget.GridLayout.spec(GridLayout.UNDEFINED, 1f)
                params.columnSpec = androidx.gridlayout.widget.GridLayout.spec(currentColumn)
                params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                
                contactView.layoutParams = params
                currentColumn++

                gridLayout.addView(contactView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "加载联系人失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showEditDialog(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("编辑联系人")
            .setItems(arrayOf("编辑", "删除")) { _, which ->
                when (which) {
                    0 -> { // 编辑
                        val intent = Intent(this, EditContactActivity::class.java).apply {
                            putExtra("contact_id", contact.id)
                        }
                        startActivityForResult(intent, EDIT_CONTACT_REQUEST)
                    }
                    1 -> { // 删除
                        AlertDialog.Builder(this)
                            .setTitle("确认删除")
                            .setMessage("确定要删除联系人 ${contact.name} 吗？")
                            .setPositiveButton("确定") { _, _ ->
                                dbHelper.deleteContact(contact.id)
                                loadContacts() // 重新加载联系人列表
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun showCallDialog(contact: Contact) {
        try {
            // 获取保存的文字大小设置
            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            val textSize = prefs.getInt("text_size", 16).toFloat()

            // 使用 MaterialAlertDialogBuilder 替代 AlertDialog.Builder
            val builder = MaterialAlertDialogBuilder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_call_confirm, null)

            val titleText = dialogView.findViewById<TextView>(R.id.dialog_title)
            val messageText = dialogView.findViewById<TextView>(R.id.dialog_message)
            val callButton = dialogView.findViewById<MaterialButton>(R.id.call_button)
            val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancel_button)

            if (titleText != null && messageText != null && callButton != null && cancelButton != null) {
                // 设置文字大小
                titleText.textSize = textSize
                messageText.textSize = textSize
                callButton.textSize = textSize
                cancelButton.textSize = textSize

                // 设置文字内容
                titleText.text = "拨打电话"
                messageText.text = "是否拨打 ${contact.name} 的电话？\n\n${contact.phoneNumber}"

                // 设置按钮颜色
                callButton.setTextColor(getColor(android.R.color.holo_green_dark))
                cancelButton.setTextColor(getColor(android.R.color.holo_red_dark))

                // 创建并显示对话框
                val dialog = builder
                    .setView(dialogView)
                    .setCancelable(true)
                    .create()

                // 设置对话框宽度为屏幕宽度的90%
                dialog.setOnShowListener {
                    val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
                    dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
                }

                // 设置按钮点击事件
                callButton.setOnClickListener {
                    dialog.dismiss()
                    makePhoneCall(contact.phoneNumber)
                }

                cancelButton.setOnClickListener {
                    dialog.dismiss()
                }

                // 在有效的 Context 中显示对话框
                if (!isFinishing && !isDestroyed) {
                    dialog.show()
                }
            } else {
                Toast.makeText(this, "创建对话框失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "显示对话框失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makePhoneCall(phoneNumber: String) {
        try {
            // 清理电话号码，只保留数字和加号
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            
            if (cleanNumber.isEmpty()) {
                Toast.makeText(this, "无效的电话号码", Toast.LENGTH_SHORT).show()
                return
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                }
                startActivity(intent)
            } else {
                // 请求拨号权限
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    PERMISSIONS_REQUEST_CODE
                )
                Toast.makeText(this, "请授予拨号权限", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "拨号失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 权限被授予，重新尝试拨号
                    Toast.makeText(this, "权限已授予，请重新点击拨号", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "拨号权限被拒绝", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                ADD_CONTACT_REQUEST, EDIT_CONTACT_REQUEST -> {
                    loadContacts()
                    Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
                }
                SETTINGS_REQUEST -> {
                    // 设置更改后重新加载联系人列表以应用新的大小设置
                    loadContacts()
                    Toast.makeText(this, "设置已更新", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivityForResult(intent, SETTINGS_REQUEST)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val scale = resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    companion object {
        private const val ADD_CONTACT_REQUEST = 1
        private const val EDIT_CONTACT_REQUEST = 2
        private const val SETTINGS_REQUEST = 3
        private const val PERMISSIONS_REQUEST_CODE = 100
    }
} 