package de.theiling.neatlauncher

import android.content.Context
import android.content.DialogInterface
import android.util.TypedValue
import android.view.View

val Context.accentColor: Int get() {
    val tv = TypedValue()
    theme.resolveAttribute(android.R.attr.colorAccent, tv, true)
    return tv.data
}

val Context.mainForeground: Int get() = getColor(R.color.mainForeground)
val Context.veryDimColor: Int get() = getColor(R.color.veryDimColor)

fun View.setOnClickDismiss(d: DialogInterface, andDo: ()->Unit) =
    setOnClickListener {
        d.dismiss()
        andDo()
    }

fun View.setOnLongClickDismiss(d: DialogInterface, andDo: ()->Unit) =
    setOnLongClickListener {
        d.dismiss()
        andDo()
        true
    }

data class Span(
    val start: Int,
    val size: Int)
{
    val end get() = start + size
}

data class MatchWords(
    val haystack: String,
    val rank: Int,
    val spans: List<Span>)

fun CharSequence.isWordStartBoundary(pos: Int): Boolean {
    if (pos == 0) return true
    if (pos == length) return false
    val a = this[pos-1]
    if (!a.isLetter()) return true
    val b = this[pos]
    if (!b.isLetter()) return false
    return (a.lowercase() == a.toString()) && (b.uppercase() == b.toString())
}

fun String.containsWords(needle: CharSequence, ignoreCase: Boolean): MatchWords? {
    val s = mutableListOf<Span>()
    var sumRank = 0
    var pos = 0
    for (word in needle.split(" ")) {
        if (word == "") continue
        var idx = -1
        var rank = -1
        var p = 0
        while (true) {
            val i = indexOf(word, p, ignoreCase)
            if (i < 0) break
            val r = (if (isWordStartBoundary(i)) 100 else 0) + (if (i >= pos) 10 else 0)
            if (r > rank) {
                idx = i
                rank = r
            }
            p = i + 1
        }
        if (idx < 0) return null
        s.add(Span(idx, word.length))
        sumRank += rank
        pos = idx + word.length
    }
    return MatchWords(this, sumRank, s)
}
