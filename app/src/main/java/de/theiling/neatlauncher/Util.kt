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
    val wordMatch: Boolean = false,
    val spans: List<Span> = listOf())
{
    val rank get() = when {
        spans.isEmpty() -> 0
        !wordMatch -> 2
        spans[0].start > 0 -> 1
        else -> 0
    }
}

fun CharSequence.isWordStartBoundary(pos: Int): Boolean {
    if (pos == 0) return true
    if (pos == length) return false
    val a = this[pos-1]
    if (!a.isLetter()) return true
    val b = this[pos]
    if (!b.isLetter()) return false
    return (a.lowercase() == a.toString()) && (b.uppercase() == b.toString())
}

fun CharSequence.indexOfWord(
    needle: String, start: Int, ignoreCase: Boolean): Pair<Int,Boolean>
{
    val a = indexOf(needle, start, ignoreCase)
    if (a < 0) return Pair(-1,false)
    var b = a
    while (true) {
        if (isWordStartBoundary(b)) return Pair(b, true)
        b = indexOf(needle, b + 1, ignoreCase)
        if (b < 0) return Pair(a, false)
    }
}

fun String.containsWords(needle: CharSequence): MatchWords? {
    val haystack = this
    val r = mutableListOf<Span>()
    val words = needle.split(" ").filter { it != "" }
    if (words.isEmpty()) return MatchWords(haystack)

    var wordMatch = true
    for (word in words) {
        val (idx, beginWord) = haystack.indexOfWord(word, 0, true)
        if (idx < 0) return null
        if (!beginWord) wordMatch = false
        r.add(Span(idx, word.length))
    }

    return MatchWords(haystack, wordMatch, r)
}
