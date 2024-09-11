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

fun String.containsWords(needle: CharSequence): MatchWords? {
    val haystack = this
    val r = mutableListOf<Span>()
    val words = needle.split(" ").filter { it != "" }
    if (words.isEmpty()) return MatchWords(haystack)

    // first step: try to match at all
    var pos = 0
    for (word in words) {
        val nextPos = haystack.indexOf(word, pos, true)
        if (nextPos < 0) return null
        r.add(Span(nextPos, word.length))
        pos = nextPos + 1
    }

    // ok, matches.  Second step: find better match at starts of words:
    val s = mutableListOf<Span>()
    pos = 0
    for (word in words) {
        val match = Regex(
            // \b is not Unicode, so:
            // either no letter precedes, or we're at a lowercaseUppercase boundary.
            "((?<!\\p{L})|(?<=\\p{Ll})(?=\\p{Lu}))" + Regex.escape(word), RegexOption.IGNORE_CASE)
            .find(haystack, pos) ?: return MatchWords(haystack, false, r)
        val nextPos = match.range.first
        s.add(Span(nextPos, word.length))
        pos = nextPos + 1
    }

    return MatchWords(haystack, true, s)
}
