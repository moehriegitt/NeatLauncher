package de.theiling.neatlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ListItem(
    val text: String,
    val onClick: () -> Unit)

class ListAdapter(
    val them: MutableList<ListItem>,
    private val layoutId: Int):
    RecyclerView.Adapter<ListAdapter.ViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false))

    override fun onBindViewHolder(holder: ListAdapter.ViewHolder, pos: Int) {
        holder.nameView.text = them[pos].text
    }

    override fun getItemCount(): Int = them.size

    inner class ViewHolder(view: View):
        RecyclerView.ViewHolder(view),
        View.OnClickListener,
        View.OnLongClickListener
    {
        var nameView: TextView = view.findViewById(R.id.item_name)
        init {
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)
        }
        override fun onClick(view: View) {
            val pos = adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                them[pos].onClick()
            }
        }
        override fun onLongClick(view: View): Boolean {
            return false
        }
    }
}
