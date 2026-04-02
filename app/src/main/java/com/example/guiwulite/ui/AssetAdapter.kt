package com.example.guiwulite.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.guiwulite.R
import com.example.guiwulite.data.AssetItem
import com.example.guiwulite.databinding.ItemAssetBinding
import java.text.DecimalFormat
import kotlin.math.max

class AssetAdapter : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    private val items = mutableListOf<AssetItem>()
    private val amountFormat = DecimalFormat("#,##0")
    private val dailyFormat = DecimalFormat("#,##0.00")
    private var onItemClick: ((AssetItem) -> Unit)? = null
    private var onItemLongClick: ((AssetItem) -> Unit)? = null

    fun setOnItemClickListener(listener: (AssetItem) -> Unit) {
        onItemClick = listener
    }

    fun setOnItemLongClickListener(listener: (AssetItem) -> Unit) {
        onItemLongClick = listener
    }

    fun submitList(data: List<AssetItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val binding = ItemAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AssetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class AssetViewHolder(
        private val binding: ItemAssetBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AssetItem) {
            val total = item.totalAmount()
            val days = max(1L, ((System.currentTimeMillis() - item.purchaseDate) / 86_400_000L) + 1L)
            val daily = total / days.toDouble()

            binding.iconImage.setImageResource(iconResFor(item.iconName))
            binding.assetName.text = item.name
            binding.assetPrice.text = binding.root.context.getString(
                R.string.asset_amount_compact,
                amountFormat.format(total)
            )
            binding.assetDaily.text = binding.root.context.getString(
                R.string.asset_daily_cost,
                dailyFormat.format(daily)
            )
            binding.assetDays.text = binding.root.context.getString(
                R.string.asset_days_count,
                days.toString()
            )
            binding.root.setOnClickListener { onItemClick?.invoke(item) }
            binding.root.setOnLongClickListener {
                onItemLongClick?.invoke(item)
                true
            }
        }
    }

    private fun iconResFor(iconName: String): Int = when (iconName) {
        "电脑" -> R.drawable.ic_asset_laptop
        "相机" -> R.drawable.ic_asset_camera
        "耳机" -> R.drawable.ic_asset_headset
        "游戏" -> R.drawable.ic_asset_game
        else -> R.drawable.ic_asset_phone
    }
}
