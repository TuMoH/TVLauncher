package com.androidtim.tvlauncher

import android.os.AsyncTask
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import java.lang.ref.WeakReference

class Adapter(
    val data: List<AppInfo>,
    private val itemClickListener: (position: Int) -> Unit,
    private val showAddButton: Boolean = false,
    private val addClickListener: (() -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val FOCUSED_VIEW_Z = 12f
        const val FOCUSED_VIEW_SCALE = 1.2f
        const val DURATION_IN = 120L

        const val DEFAULT_VIEW_Z = 5f
        const val DEFAULT_VIEW_SCALE = 1f
        const val DURATION_OUT = 100L

        val INTERPOLATOR = DecelerateInterpolator()
    }

    enum class ViewType {
        TYPE_ITEM,
        TYPE_ADD
    }

    override fun getItemCount(): Int {
        return if (showAddButton) (data.size + 1) else data.size
    }

    override fun getItemViewType(position: Int): Int {
        val result = if (position < data.size) ViewType.TYPE_ITEM.ordinal
        else ViewType.TYPE_ADD.ordinal
        return result
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        val viewType = ViewType.values()[type]
        return when (viewType) {
            ViewType.TYPE_ITEM -> ItemViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
            )
            ViewType.TYPE_ADD -> AddViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.add_item, parent, false)
            )
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (viewHolder is ItemViewHolder) {
            val appInfo = data[position]

            viewHolder.nameView.text = appInfo.name
            viewHolder.iconView.setImageDrawable(appInfo.icon)
        }
    }

    open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnFocusChangeListener { view, hasFocus ->
                view.animate().cancel()
                if (hasFocus) {
                    view.z = FOCUSED_VIEW_Z
                    view.animate()
                        .scaleX(FOCUSED_VIEW_SCALE)
                        .scaleY(FOCUSED_VIEW_SCALE)
                        .setDuration(DURATION_IN)
                        .setInterpolator(INTERPOLATOR)
                        .start()
                } else {
                    view.z = DEFAULT_VIEW_Z
                    view.animate()
                        .scaleX(DEFAULT_VIEW_SCALE)
                        .scaleY(DEFAULT_VIEW_SCALE)
                        .setDuration(DURATION_OUT)
                        .setInterpolator(INTERPOLATOR)
                        .start()
                }
            }
        }
    }

    inner class ItemViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val nameView = itemView.findViewById<TextView>(R.id.name)
        val iconView = itemView.findViewById<ImageView>(R.id.icon)

        init {
            itemView.setOnClickListener { itemClickListener(adapterPosition) }
        }
    }

    inner class AddViewHolder(itemView: View) : BaseViewHolder(itemView) {
        init {
            itemView.setOnClickListener { addClickListener?.invoke() }
        }
    }

}
