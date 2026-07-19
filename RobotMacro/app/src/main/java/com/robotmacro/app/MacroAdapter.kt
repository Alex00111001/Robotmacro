package com.robotmacro.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.robotmacro.app.data.entity.MacroEntity

class MacroAdapter(
    private val onRun: (com.robotmacro.app.model.Macro) -> Unit,
    private val onEdit: (com.robotmacro.app.model.Macro) -> Unit,
    private val onDelete: (com.robotmacro.app.model.Macro) -> Unit
) : ListAdapter<MacroEntity, MacroAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_macro, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entity = getItem(position)
        val macro = com.robotmacro.app.model.Macro(
            id = entity.id,
            name = entity.name,
            actions = entity.actions,
            trigger = entity.trigger,
            loopConfig = entity.loopConfig
        )

        holder.tvName.text = entity.name
        holder.tvInfo.text = "${entity.actions.size} acciones • Loop: ${entity.loopConfig.maxIterations}x"
        holder.tvLastRun.text = entity.lastExecutionTime?.let { "Última ejecución: ${formatTime(it)}" } ?: "Sin ejecutar"

        holder.btnRun.setOnClickListener { onRun(macro) }
        holder.itemView.setOnClickListener { onEdit(macro) }
    }

    private fun formatTime(time: Long): String {
        val diff = System.currentTimeMillis() - time
        return when {
            diff < 60000 -> "hace ${diff / 1000}s"
            diff < 3600000 -> "hace ${diff / 60000}m"
            diff < 86400000 -> "hace ${diff / 3600000}h"
            else -> "hace ${diff / 86400000}d"
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvMacroName)
        val tvInfo: TextView = view.findViewById(R.id.tvMacroInfo)
        val tvLastRun: TextView = view.findViewById(R.id.tvLastRun)
        val btnRun: ImageButton = view.findViewById(R.id.btnRun)
    }

    class DiffCallback : DiffUtil.ItemCallback<MacroEntity>() {
        override fun areItemsTheSame(old: MacroEntity, new: MacroEntity) = old.id == new.id
        override fun areContentsTheSame(old: MacroEntity, new: MacroEntity) = old == new
    }
}
