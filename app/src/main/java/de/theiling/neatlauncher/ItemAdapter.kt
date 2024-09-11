package de.theiling.neatlauncher

import android.icu.lang.UCharacter.foldCase
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

data class ItemViewInfo(
    val item: Item,
    val match: MatchWords? = null)
{
    var initial: String = ""
    var preSep: Boolean = false

    fun displayCompareToMatch(other: ItemViewInfo): Int {
        val t = this.match?.rank ?: 9
        val o = other.match?.rank ?: 9
        val i = t.compareTo(o)
        if (i != 0) return i
        return this.item.displayCompareToMatch(other.item)
    }
}

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

        if (a.match != null) {
            val t = SpannableStringBuilder(a.match.haystack)
            for (s in a.match.spans) {
                t.setSpan(UnderlineSpan(), s.start, s.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            holder.nameView.text = t
        }
        else {
            holder.nameView.text = a.item.label
        }

        holder.nameView.setTextColor(a.item.color)
        holder.initial?.text = a.initial
    }

    override fun getItemCount(): Int = partial.size

    override fun getFilter(): Filter = object: Filter() {
        override fun performFiltering(hay: CharSequence): FilterResults
        {
            val f = mutableListOf<ItemViewInfo>()
            if (hay == "") {
                them.forEach { if (whenEmpty(it)) f.add(ItemViewInfo(it)) }
                f.sortWith { a, b -> a.item.displayCompareTo(b.item) }
            } else {
                them.forEach {
                    val m = it.label.containsWords(hay)
                    if (m != null) f.add(ItemViewInfo(it, m))
                }
                f.sortWith { a, b -> a.displayCompareToMatch(b) }
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
                clicky.onClickItem(view, partial[pos].item)
            }
        }

        override fun onLongClick(view: View): Boolean {
            val pos = adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                clicky.onLongClickItem(view, partial[pos].item)
            }
            return true
        }
    }

    interface ClickListener {
        fun onClickItem(view: View, item: Item)
        fun onLongClickItem(view: View, item: Item)
    }
}
