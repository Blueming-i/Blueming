package com.example.guiwulite

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guiwulite.data.AssetItem
import com.example.guiwulite.data.AssetRepository
import com.example.guiwulite.data.AssetStatus
import com.example.guiwulite.databinding.ActivityAddAssetBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddAssetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAssetBinding
    private lateinit var repository: AssetRepository
    private val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
    private val purchaseCalendar = Calendar.getInstance()
    private var selectedIcon = "手机"
    private var selectedImageUri = ""
    private var editingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAssetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AssetRepository(this)
        binding.topBar.setNavigationOnClickListener { finish() }
        setupCategoryInput()
        binding.iconCard.setOnClickListener { showIconMenu() }
        binding.purchaseDateValue.setOnClickListener { showDatePicker() }
        binding.chooseImageText.setOnClickListener { openImagePicker() }
        binding.imageBox.setOnClickListener { openImagePicker() }
        binding.submitButton.setOnClickListener { submitAsset() }
        binding.deleteButton.setOnClickListener { deleteAsset() }
        editingId = intent.getStringExtra(EXTRA_ASSET_ID)
        val editingItem = repository.loadAssets().firstOrNull { it.id == editingId }
        if (editingItem != null) {
            populateForm(editingItem)
        }
        updateDateText()
        renderIcon()
    }

    private fun setupCategoryInput() {
        val categories = repository.loadCategories().filter { it != "全部分类" }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            if (categories.isEmpty()) listOf("未分类") else categories
        )
        binding.categoryAutoComplete.setAdapter(adapter)
        binding.categoryAutoComplete.setText(adapter.getItem(0), false)
    }

    private fun showIconMenu() {
        val popup = PopupMenu(this, binding.iconCard)
        listOf("手机", "电脑", "相机", "耳机", "游戏").forEachIndexed { index, label ->
            popup.menu.add(0, index, index, label)
        }
        popup.setOnMenuItemClickListener {
            selectedIcon = it.title.toString()
            renderIcon()
            true
        }
        popup.show()
    }

    private fun renderIcon() {
        binding.iconPreview.setImageResource(iconResFor(selectedIcon))
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                purchaseCalendar.set(year, month, day)
                updateDateText()
            },
            purchaseCalendar.get(Calendar.YEAR),
            purchaseCalendar.get(Calendar.MONTH),
            purchaseCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateText() {
        binding.purchaseDateValue.text = dateFormat.format(purchaseCalendar.time)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_image)), IMAGE_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            selectedImageUri = uri.toString()
            val label = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
            } ?: getString(R.string.image_selected)
            binding.imageHint.text = label
        }
    }

    private fun submitAsset() {
        val name = binding.nameEdit.text?.toString()?.trim().orEmpty()
        val price = binding.priceEdit.text?.toString()?.trim().orEmpty().toDoubleOrNull()
        if (name.isBlank()) {
            binding.nameLayout.error = getString(R.string.error_name_required)
            return
        } else {
            binding.nameLayout.error = null
        }
        if (price == null) {
            binding.priceLayout.error = getString(R.string.error_price_required)
            return
        } else {
            binding.priceLayout.error = null
        }

        val asset = AssetItem(
            id = editingId ?: java.util.UUID.randomUUID().toString(),
            name = name,
            category = binding.categoryAutoComplete.text?.toString()?.trim().orEmpty().ifBlank { "未分类" },
            price = price,
            purchaseDate = purchaseCalendar.timeInMillis,
            warrantyEnabled = binding.warrantySwitch.isChecked,
            countByUsage = binding.usageSwitch.isChecked,
            targetDailyEnabled = binding.dailySwitch.isChecked,
            inService = binding.serviceSwitch.isChecked,
            extraCost = binding.extraCostEdit.text?.toString()?.trim().orEmpty().toDoubleOrNull() ?: 0.0,
            note = binding.noteEdit.text?.toString()?.trim().orEmpty(),
            imageUri = selectedImageUri,
            iconName = selectedIcon,
            status = if (binding.serviceSwitch.isChecked) AssetStatus.IN_USE else AssetStatus.STORED
        )
        val assets = repository.loadAssets()
        val index = assets.indexOfFirst { it.id == asset.id }
        if (index >= 0) {
            assets[index] = asset
        } else {
            assets.add(0, asset)
        }
        repository.saveAssets(assets)

        val categories = repository.loadCategories()
        if (!categories.contains(asset.category)) {
            categories.add(asset.category)
            repository.saveCategories(categories)
        }

        Toast.makeText(
            this,
            getString(R.string.asset_added_toast, asset.name, DecimalFormat("#,##0").format(asset.totalAmount())),
            Toast.LENGTH_SHORT
        ).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun populateForm(item: AssetItem) {
        binding.topBar.title = getString(R.string.edit_asset)
        binding.submitButton.text = getString(R.string.action_save)
        binding.deleteButton.visibility = android.view.View.VISIBLE
        binding.nameEdit.setText(item.name)
        binding.categoryAutoComplete.setText(item.category, false)
        binding.priceEdit.setText(item.price.toString())
        binding.extraCostEdit.setText(if (item.extraCost == 0.0) "" else item.extraCost.toString())
        binding.noteEdit.setText(item.note)
        binding.warrantySwitch.isChecked = item.warrantyEnabled
        binding.usageSwitch.isChecked = item.countByUsage
        binding.dailySwitch.isChecked = item.targetDailyEnabled
        binding.serviceSwitch.isChecked = item.inService
        selectedIcon = item.iconName
        selectedImageUri = item.imageUri
        purchaseCalendar.timeInMillis = item.purchaseDate
        updateDateText()
        renderIcon()
        if (selectedImageUri.isNotBlank()) {
            binding.imageHint.text = getString(R.string.image_selected)
        }
    }

    private fun deleteAsset() {
        val id = editingId ?: return
        val assets = repository.loadAssets().filterNot { it.id == id }
        repository.saveAssets(assets)
        Toast.makeText(this, R.string.asset_deleted, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun iconResFor(iconName: String): Int = when (iconName) {
        "电脑" -> R.drawable.ic_asset_laptop
        "相机" -> R.drawable.ic_asset_camera
        "耳机" -> R.drawable.ic_asset_headset
        "游戏" -> R.drawable.ic_asset_game
        else -> R.drawable.ic_asset_phone
    }

    companion object {
        private const val IMAGE_REQUEST_CODE = 2001
        const val EXTRA_ASSET_ID = "extra_asset_id"
    }
}
