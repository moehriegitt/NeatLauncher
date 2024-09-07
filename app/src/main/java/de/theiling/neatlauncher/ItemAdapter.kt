package de.theiling.neatlauncher

import android.icu.lang.UCharacter.foldCase
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

data class ItemViewInfo(val item: Item, var initial: String = "", var preSep: Boolean = false)

class ItemAdapter(
    val them: MutableSet<Item>,
    private val layoutId: Int,
    val whenEmpty: (Item) -> Boolean,
    val clicky: ClickListener):
    RecyclerView.Adapter<ItemAdapter.ViewHolder>(),
    Filterable
{
    var partial = mutableListOf<ItemViewInfo>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false))

    override fun onBindViewHolder(holder: ItemAdapter.ViewHolder, pos: Int) {
        val a = partial[pos]
        holder.preSep?.visibility = if (a.preSep) View.VISIBLE else View.GONE
        holder.nameView.text = a.item.label
        holder.nameView.setTextColor(a.item.color)
        holder.initial?.text = a.initial
    }

    override fun getItemCount(): Int = partial.size

    override fun getFilter(): Filter = object: Filter() {
        override fun performFiltering(hay_: CharSequence): FilterResults
        {
            val f = mutableListOf<ItemViewInfo>()
            if (hay_ == "") {
                them.forEach { if (whenEmpty(it)) f.add(ItemViewInfo(it)) }
                f.sortWith { a, b -> a.item.displayCompareTo(b.item) }
            } else {
                val hay = hay_.toString().trim()
                them.forEach { if (it.label.contains(hay, true)) f.add(ItemViewInfo(it)) }
                f.sortWith { a, b -> a.item.displayCompareToSearch(b.item, hay) }
            }
            var i = ""
            for (a in f) {
                val k = foldCase(a.item.order, true).substring(0,1).uppercase(Locale.getDefault())
                if (k != i) {
                    a.preSep = k < i
                    a.initial = k
                    i = k
                }
            }
            val r = FilterResults()
            r.count = f.size
            r.values = f
            return r
        }

        override fun publishResults(s: CharSequence, r: FilterResults)
        {
            if (r.values != null) {
                partial = r.values as MutableList<ItemViewInfo>
            }
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(view: View):
        RecyclerView.ViewHolder(view),
        View.OnClickListener,
        View.OnLongClickListener
    {
        var preSep: View? = view.findViewById(R.id.pre_sep)
        var nameView: TextView = view.findViewById(R.id.item_name)
        var initial: TextView? = view.findViewById(R.id.initial_letter)

        init {
            preSep?.visibility = View.GONE
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            val pos = adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                clicky.onClick(view, partial[pos].item)
            }
        }

        override fun onLongClick(view: View): Boolean {
            val pos = adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                clicky.onLongClick(view, partial[pos].item)
            }
            return true
        }
    }

    interface ClickListener {
        fun onClick(view: View, item: Item)
        fun onLongClick(view: View, item: Item)
    }
}
