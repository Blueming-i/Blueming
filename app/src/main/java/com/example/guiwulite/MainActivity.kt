package com.example.guiwulite

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guiwulite.data.AssetItem
import com.example.guiwulite.data.AssetRepository
import com.example.guiwulite.data.AssetStatus
import com.example.guiwulite.data.DateRange
import com.example.guiwulite.data.SortMode
import com.example.guiwulite.data.WishItem
import com.example.guiwulite.databinding.ActivityMainBinding
import com.example.guiwulite.databinding.DialogAddWishBinding
import com.example.guiwulite.databinding.ItemWishBinding
import com.example.guiwulite.ui.AssetAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: AssetRepository
    private lateinit var adapter: AssetAdapter

    private val amountFormat = DecimalFormat("#,##0.00")
    private val compactFormat = DecimalFormat("#,##0")
    private val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.CHINA)
    private val monthLabelFormat = SimpleDateFormat("yyyy年M月", Locale.CHINA)
    private val yearLabelFormat = SimpleDateFormat("yyyy年", Locale.CHINA)
    private val dayLabelFormat = SimpleDateFormat("M月d日", Locale.CHINA)

    private val assets = mutableListOf<AssetItem>()
    private val wishlist = mutableListOf<WishItem>()
    private val categories = mutableListOf<String>()

    private lateinit var searchEdit: EditText
    private lateinit var clearSearchButton: TextView

    private var selectedStatus = AssetStatus.ALL
    private var selectedCategory = "全部分类"
    private var selectedSort = SortMode.DEFAULT
    private var selectedRange = DateRange.ALL
    private var selectedRangeKey = ""
    private var currentTab = Tab.HOME
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AssetRepository(this)
        adapter = AssetAdapter()
        binding.assetRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.assetRecyclerView.adapter = adapter
        adapter.setOnItemClickListener { asset ->
            startActivity(Intent(this, AddAssetActivity::class.java).putExtra(AddAssetActivity.EXTRA_ASSET_ID, asset.id))
        }
        adapter.setOnItemLongClickListener { asset -> confirmDeleteAsset(asset) }

        setupListeners()
        loadData()
        switchTab(Tab.HOME)
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupListeners() {
        binding.searchButton.setOnClickListener {
            binding.searchPanel.isVisible = !binding.searchPanel.isVisible
            if (binding.searchPanel.isVisible) {
                searchEdit.requestFocus()
            }
        }
        binding.addAssetButton.setOnClickListener { startActivity(Intent(this, AddAssetActivity::class.java)) }
        binding.allButton.setOnClickListener { showStatusMenu() }
        binding.categoryButton.setOnClickListener { showCategoryMenu() }
        binding.sortButton.setOnClickListener { showSortMenu() }
        binding.manageCategoryText.setOnClickListener {
            startActivity(Intent(this, CategoryManagementActivity::class.java))
        }

        binding.weekChip.setOnClickListener { updateRange(DateRange.WEEK) }
        binding.monthChip.setOnClickListener { updateRange(DateRange.MONTH) }
        binding.yearChip.setOnClickListener { updateRange(DateRange.YEAR) }
        binding.allRangeChip.setOnClickListener { updateRange(DateRange.ALL) }

        binding.wishAddButton.setOnClickListener { showAddWishDialog() }
        binding.wishEmptyAdd.setOnClickListener { showAddWishDialog() }

        binding.bottomNav.homeNav.setOnClickListener { switchTab(Tab.HOME) }
        binding.bottomNav.statsNav.setOnClickListener { switchTab(Tab.STATS) }
        binding.bottomNav.wishNav.setOnClickListener { switchTab(Tab.WISHLIST) }
        binding.bottomNav.profileNav.setOnClickListener { switchTab(Tab.PROFILE) }

        searchEdit = binding.searchEdit
        clearSearchButton = binding.clearSearchButton
        clearSearchButton.setOnClickListener { searchEdit.setText("") }
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim().orEmpty()
                renderHome()
            }
        })
    }

    private fun loadData() {
        assets.clear()
        assets.addAll(repository.loadAssets())
        wishlist.clear()
        wishlist.addAll(repository.loadWishlist())
        categories.clear()
        categories.addAll(repository.loadCategories())
        if (!categories.contains(selectedCategory)) {
            selectedCategory = "全部分类"
        }
        render()
    }

    private fun render() {
        renderHome()
        renderStats()
        renderWishlist()
        renderProfile()
    }

    private fun renderHome() {
        val filtered = assets
            .filter { selectedStatus == AssetStatus.ALL || it.status == selectedStatus }
            .filter { selectedCategory == "全部分类" || it.category == selectedCategory }
            .filter {
                if (searchQuery.isBlank()) {
                    true
                } else {
                    val query = searchQuery.lowercase()
                    it.name.lowercase().contains(query) ||
                        it.category.lowercase().contains(query) ||
                        it.note.lowercase().contains(query)
                }
            }
            .sortedWith(
                when (selectedSort) {
                    SortMode.DEFAULT -> compareByDescending<AssetItem> { it.createdAt }
                    SortMode.AMOUNT -> compareByDescending { it.totalAmount() }
                    SortMode.DAYS -> compareByDescending { daysSince(it.purchaseDate) }
                    SortMode.COUNT -> compareByDescending { if (it.countByUsage) 1 else 0 }
                    SortMode.PURCHASE_DATE -> compareByDescending { it.purchaseDate }
                    SortMode.DAILY_COST -> compareByDescending { dailyCost(it) }
                }
            )

        val holdingAssets = assets.filter { it.status != AssetStatus.SOLD }
        adapter.submitList(filtered)
        binding.homeTitle.text = getString(R.string.assets_count_title, assets.size, assets.size)
        binding.totalAssetValue.text = getString(R.string.money_value, compactFormat.format(holdingAssets.sumOf { it.totalAmount() }))
        binding.avgDailyValue.text = getString(R.string.money_value_short, amountFormat.format(holdingAssets.sumOf { dailyCost(it) }))
        binding.allButton.text = labelForStatus(selectedStatus)
        binding.categoryButton.text = selectedCategory
        binding.sortButton.text = labelForSort(selectedSort)
        binding.homeEmptyState.isVisible = filtered.isEmpty()
        binding.assetRecyclerView.isVisible = filtered.isNotEmpty()
    }

    private fun renderStats() {
        val buckets = buildRangeBuckets(selectedRange)
        if (buckets.isEmpty()) {
            selectedRangeKey = ""
            binding.statsRangeContainer.removeAllViews()
            renderStatsForAssets(emptyList())
            highlightRange(selectedRange)
            return
        }

        if (selectedRangeKey.isBlank() || buckets.none { it.key == selectedRangeKey }) {
            selectedRangeKey = buckets.first().key
        }

        renderRangeBuckets(buckets)
        val selectedBucket = buckets.firstOrNull { it.key == selectedRangeKey } ?: buckets.first()
        renderStatsForAssets(selectedBucket.assets)
        highlightRange(selectedRange)
    }

    private fun renderStatsForAssets(bucketAssets: List<AssetItem>) {
        val purchasedAssets = bucketAssets.filter { it.status != AssetStatus.SOLD }
        val soldAssets = bucketAssets.filter { it.status == AssetStatus.SOLD }
        val holdingAmount = purchasedAssets.sumOf { it.totalAmount() }
        val dailyTotal = purchasedAssets.sumOf { dailyCost(it) }
        val periodDaily = if (bucketAssets.isEmpty()) 0.0 else bucketAssets.sumOf { dailyCost(it) }

        binding.statsContent.buyAmount.text = getString(
            R.string.money_value_with_decimals,
            amountFormat.format(purchasedAssets.sumOf { it.totalAmount() })
        )
        binding.statsContent.sellAmount.text = getString(
            R.string.money_value_with_decimals,
            amountFormat.format(soldAssets.sumOf { it.totalAmount() })
        )
        binding.statsContent.buyCount.text = purchasedAssets.size.toString()
        binding.statsContent.sellCount.text = soldAssets.size.toString()
        binding.statsContent.dailyTotal.text = getString(R.string.money_value_short, amountFormat.format(dailyTotal))
        binding.statsContent.periodDaily.text = getString(R.string.money_value_with_decimals, amountFormat.format(periodDaily))

        val grouped = purchasedAssets.groupBy { it.category.ifBlank { "未分类" } }
        val dominant = grouped.maxByOrNull { entry -> entry.value.sumOf { asset -> asset.totalAmount() } }
        val dominantAmount = dominant?.value?.sumOf { it.totalAmount() } ?: 0.0
        val ratio = if (holdingAmount <= 0.0) 1f else (dominantAmount / holdingAmount).toFloat()

        binding.statsContent.assetChart.setContent(
            ratio,
            getString(R.string.total_assets_short),
            getString(R.string.money_value_short, amountFormat.format(holdingAmount))
        )
        binding.statsContent.chartCategoryName.text = dominant?.key ?: getString(R.string.no_category)
        binding.statsContent.chartCategoryMeta.text = getString(
            R.string.category_distribution_meta,
            dominant?.value?.size ?: 0,
            amountFormat.format(dominantAmount)
        )
        binding.statsContent.chartCategoryPercent.text = getString(R.string.percent_value, (ratio * 100).toInt())
        binding.statsContent.categoryStatTitle.text = dominant?.key ?: getString(R.string.no_category)
        binding.statsContent.categoryStatAmount.text = getString(R.string.money_value_with_decimals, amountFormat.format(dominantAmount))
        binding.statsContent.categoryStatPercent.text = getString(R.string.percent_value, (ratio * 100).toInt())
    }

    private fun buildRangeBuckets(range: DateRange): List<RangeBucket> {
        if (assets.isEmpty()) {
            return if (range == DateRange.ALL) {
                listOf(
                    RangeBucket(
                        key = "all",
                        label = "全部",
                        startMillis = 0L,
                        endMillis = System.currentTimeMillis(),
                        assets = emptyList()
                    )
                )
            } else {
                emptyList()
            }
        }

        if (range == DateRange.ALL) {
            val sortedAssets = assets.sortedByDescending { it.purchaseDate }
            return listOf(
                RangeBucket(
                    key = "all",
                    label = "全部",
                    startMillis = sortedAssets.minOf { it.purchaseDate },
                    endMillis = sortedAssets.maxOf { it.purchaseDate },
                    assets = sortedAssets
                )
            )
        }

        val buckets = linkedMapOf<String, MutableList<AssetItem>>()
        val infos = linkedMapOf<String, RangeBucketMeta>()
        assets.sortedByDescending { it.purchaseDate }.forEach { asset ->
            val info = bucketMetaFor(asset.purchaseDate, range)
            buckets.getOrPut(info.key) { mutableListOf() }.add(asset)
            infos.putIfAbsent(info.key, info)
        }

        return infos.values
            .sortedByDescending { it.startMillis }
            .map { meta ->
                RangeBucket(
                    key = meta.key,
                    label = meta.label,
                    startMillis = meta.startMillis,
                    endMillis = meta.endMillis,
                    assets = buckets[meta.key].orEmpty().sortedByDescending { it.purchaseDate }
                )
            }
    }

    private fun bucketMetaFor(timeMillis: Long, range: DateRange): RangeBucketMeta {
        val start = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val end = Calendar.getInstance().apply { timeInMillis = timeMillis }
        return when (range) {
            DateRange.WEEK -> {
                start.firstDayOfWeek = Calendar.MONDAY
                start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                resetToDayStart(start)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.DAY_OF_MONTH, 6)
                end.set(Calendar.HOUR_OF_DAY, 23)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)
                RangeBucketMeta(
                    key = "week-${start.timeInMillis}",
                    label = "${dayLabelFormat.format(Date(start.timeInMillis))}-${dayLabelFormat.format(Date(end.timeInMillis))}",
                    startMillis = start.timeInMillis,
                    endMillis = end.timeInMillis
                )
            }
            DateRange.MONTH -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                resetToDayStart(start)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.MONTH, 1)
                end.add(Calendar.MILLISECOND, -1)
                RangeBucketMeta(
                    key = "month-${start.get(Calendar.YEAR)}-${start.get(Calendar.MONTH)}",
                    label = monthLabelFormat.format(Date(start.timeInMillis)),
                    startMillis = start.timeInMillis,
                    endMillis = end.timeInMillis
                )
            }
            DateRange.YEAR -> {
                start.set(Calendar.DAY_OF_YEAR, 1)
                resetToDayStart(start)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.YEAR, 1)
                end.add(Calendar.MILLISECOND, -1)
                RangeBucketMeta(
                    key = "year-${start.get(Calendar.YEAR)}",
                    label = yearLabelFormat.format(Date(start.timeInMillis)),
                    startMillis = start.timeInMillis,
                    endMillis = end.timeInMillis
                )
            }
            DateRange.ALL -> {
                RangeBucketMeta(
                    key = "all",
                    label = "全部",
                    startMillis = timeMillis,
                    endMillis = timeMillis
                )
            }
        }
    }

    private fun renderRangeBuckets(buckets: List<RangeBucket>) {
        binding.statsRangeContainer.removeAllViews()
        buckets.forEachIndexed { index, bucket ->
            val chip = TextView(this).apply {
                text = bucket.label
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(10), dp(14), dp(10))
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textPrimary))
                textSize = 15f
                background = ContextCompat.getDrawable(
                    this@MainActivity,
                    if (bucket.key == selectedRangeKey) R.drawable.bg_chip_selected else R.drawable.bg_filter_button
                )
                setOnClickListener {
                    selectedRangeKey = bucket.key
                    renderStats()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) {
                        marginStart = dp(8)
                    }
                }
            }
            binding.statsRangeContainer.addView(chip)
        }
    }

    private fun renderWishlist() {
        val fulfilled = wishlist.count { it.fulfilled }
        binding.wishTotalAmount.text = getString(R.string.money_value_with_decimals, amountFormat.format(wishlist.sumOf { it.targetAmount }))
        binding.wishTotalCount.text = getString(R.string.wish_count_value, wishlist.size)
        binding.wishFulfilledCount.text = getString(R.string.wish_count_value, fulfilled)
        binding.wishEmptyLayout.isVisible = wishlist.isEmpty()
        binding.wishListContainer.isVisible = wishlist.isNotEmpty()
        binding.wishListContainer.removeAllViews()
        wishlist.forEach { wish ->
            val itemBinding = ItemWishBinding.inflate(layoutInflater, binding.wishListContainer, false)
            itemBinding.wishName.text = wish.name
            itemBinding.wishAmount.text = getString(R.string.money_value_with_decimals, amountFormat.format(wish.targetAmount))
            itemBinding.wishStatus.text = getString(if (wish.fulfilled) R.string.wish_done else R.string.wish_pending)
            binding.wishListContainer.addView(itemBinding.root)
        }
    }

    private fun renderProfile() {
        binding.profileAmount.text = getString(R.string.money_value_with_decimals, amountFormat.format(assets.sumOf { it.totalAmount() }))
        binding.profileCount.text = assets.size.toString()
        binding.profileUpdated.text = getString(R.string.profile_updated, dateFormat.format(Calendar.getInstance().time))
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        binding.homePage.isVisible = tab == Tab.HOME
        binding.statsPage.isVisible = tab == Tab.STATS
        binding.wishlistPage.isVisible = tab == Tab.WISHLIST
        binding.profilePage.isVisible = tab == Tab.PROFILE
        updateNavState()
    }

    private fun updateNavState() {
        setNavSelected(binding.bottomNav.homeNav, binding.bottomNav.homeIndicator, currentTab == Tab.HOME)
        setNavSelected(binding.bottomNav.statsNav, binding.bottomNav.statsIndicator, currentTab == Tab.STATS)
        setNavSelected(binding.bottomNav.wishNav, binding.bottomNav.wishIndicator, currentTab == Tab.WISHLIST)
        setNavSelected(binding.bottomNav.profileNav, binding.bottomNav.profileIndicator, currentTab == Tab.PROFILE)
    }

    private fun setNavSelected(container: android.view.View, indicator: android.view.View, selected: Boolean) {
        container.background = if (selected) {
            ContextCompat.getDrawable(this, R.drawable.bg_nav_selected)
        } else {
            null
        }
        indicator.isVisible = selected
    }

    private fun showStatusMenu() {
        val options = listOf(AssetStatus.ALL, AssetStatus.IN_USE, AssetStatus.STORED, AssetStatus.RETIRED, AssetStatus.SOLD)
        showMenu(binding.allButton, options.map(::labelForStatus)) {
            selectedStatus = options[it]
            renderHome()
        }
    }

    private fun showCategoryMenu() {
        showMenu(binding.categoryButton, categories) {
            selectedCategory = categories[it]
            renderHome()
        }
    }

    private fun showSortMenu() {
        val options = listOf(SortMode.DEFAULT, SortMode.AMOUNT, SortMode.DAYS, SortMode.COUNT, SortMode.PURCHASE_DATE, SortMode.DAILY_COST)
        showMenu(binding.sortButton, options.map(::labelForSort)) {
            selectedSort = options[it]
            renderHome()
        }
    }

    private fun showMenu(anchor: android.view.View, labels: List<String>, onSelect: (Int) -> Unit) {
        val popup = PopupMenu(this, anchor)
        labels.forEachIndexed { index, label -> popup.menu.add(0, index, index, label) }
        popup.setOnMenuItemClickListener {
            onSelect(it.itemId)
            true
        }
        popup.show()
    }

    private fun updateRange(range: DateRange) {
        selectedRange = range
        selectedRangeKey = ""
        renderStats()
    }

    private fun highlightRange(range: DateRange) {
        binding.weekChip.setBackgroundResource(if (range == DateRange.WEEK) R.drawable.bg_chip_selected else R.drawable.bg_filter_button)
        binding.monthChip.setBackgroundResource(if (range == DateRange.MONTH) R.drawable.bg_chip_selected else R.drawable.bg_filter_button)
        binding.yearChip.setBackgroundResource(if (range == DateRange.YEAR) R.drawable.bg_chip_selected else R.drawable.bg_filter_button)
        binding.allRangeChip.setBackgroundResource(if (range == DateRange.ALL) R.drawable.bg_chip_selected else R.drawable.bg_filter_button)
    }

    private fun showAddWishDialog() {
        val dialogBinding = DialogAddWishBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_wish)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = dialogBinding.wishNameEdit.text?.toString()?.trim().orEmpty()
                val amount = dialogBinding.wishAmountEdit.text?.toString()?.trim().orEmpty().toDoubleOrNull() ?: 0.0
                if (name.isBlank()) {
                    Toast.makeText(this, R.string.error_name_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                wishlist.add(WishItem(name = name, targetAmount = amount, fulfilled = false))
                repository.saveWishlist(wishlist)
                renderWishlist()
            }
            .show()
    }

    private fun dailyCost(item: AssetItem): Double =
        item.totalAmount() / max(1L, daysSince(item.purchaseDate)).toDouble()

    private fun resetToDayStart(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    private fun confirmDeleteAsset(asset: AssetItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.action_delete))
            .setMessage(asset.name)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                repository.saveAssets(repository.loadAssets().filterNot { it.id == asset.id })
                loadData()
            }
            .show()
    }

    private fun daysSince(time: Long): Long = max(1L, ((System.currentTimeMillis() - time) / 86_400_000L) + 1L)

    private fun labelForStatus(status: AssetStatus): String = when (status) {
        AssetStatus.ALL -> getString(R.string.filter_all)
        AssetStatus.IN_USE -> getString(R.string.status_in_use)
        AssetStatus.STORED -> getString(R.string.status_stored)
        AssetStatus.RETIRED -> getString(R.string.status_retired)
        AssetStatus.SOLD -> getString(R.string.status_sold)
    }

    private fun labelForSort(sortMode: SortMode): String = when (sortMode) {
        SortMode.DEFAULT -> getString(R.string.sort_default)
        SortMode.AMOUNT -> getString(R.string.sort_amount)
        SortMode.DAYS -> getString(R.string.sort_days)
        SortMode.COUNT -> getString(R.string.sort_count)
        SortMode.PURCHASE_DATE -> getString(R.string.sort_purchase_date)
        SortMode.DAILY_COST -> getString(R.string.sort_daily_cost)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class Tab {
        HOME,
        STATS,
        WISHLIST,
        PROFILE
    }

    private data class RangeBucket(
        val key: String,
        val label: String,
        val startMillis: Long,
        val endMillis: Long,
        val assets: List<AssetItem>
    )

    private data class RangeBucketMeta(
        val key: String,
        val label: String,
        val startMillis: Long,
        val endMillis: Long
    )
}
