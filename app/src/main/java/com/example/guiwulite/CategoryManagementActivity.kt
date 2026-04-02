package com.example.guiwulite

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guiwulite.data.AssetRepository
import com.example.guiwulite.databinding.ActivityCategoryManagementBinding
import com.example.guiwulite.ui.CategoryAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class CategoryManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryManagementBinding
    private lateinit var repository: AssetRepository
    private lateinit var adapter: CategoryAdapter
    private val categories = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AssetRepository(this)
        categories.addAll(repository.loadCategories().filter { it != "全部分类" })
        adapter = CategoryAdapter(::deleteCategory)

        binding.topBar.setNavigationOnClickListener { finish() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.addButton.setOnClickListener { showAddCategoryDialog() }
        render()
    }

    private fun showAddCategoryDialog() {
        val input = TextInputEditText(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_category)
            .setView(input)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val value = input.text?.toString()?.trim().orEmpty()
                        if (value.isBlank()) {
                            input.error = getString(R.string.error_category_required)
                            return@setOnClickListener
                        }
                        if (categories.contains(value)) {
                            input.error = getString(R.string.error_category_duplicate)
                            return@setOnClickListener
                        }
                        categories.add(value)
                        repository.saveCategories(mutableListOf("全部分类") + categories)
                        render()
                        dialog.dismiss()
                    }
                }
            }
            .show()
    }

    private fun deleteCategory(name: String) {
        categories.remove(name)
        repository.saveCategories(mutableListOf("全部分类") + categories)
        Toast.makeText(this, getString(R.string.category_deleted, name), Toast.LENGTH_SHORT).show()
        render()
    }

    private fun render() {
        binding.emptyLayout.isVisible = categories.isEmpty()
        binding.recyclerView.isVisible = categories.isNotEmpty()
        adapter.submitList(categories)
    }
}
