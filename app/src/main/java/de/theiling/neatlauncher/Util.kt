package de.theiling.neatlauncher

import android.content.Context
import android.content.DialogInterface
import android.util.TypedValue
import android.view.View
import android.widget.CompoundButton
import java.net.URLEncoder
import java.text.DateFormatSymbols
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

val Context.accentColor: Int get() {
    val tv = TypedValue()
    theme.resolveAttribute(android.R.attr.colorAccent, tv, true)
    return tv.data
}

val Context.mainForeground: Int get() = getColor(R.color.mainForeground)
val Context.dimBackground: Int get() = getColor(R.color.dimBackground)
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

fun CompoundButton.setOnClickDismiss(d: DialogInterface, andDo: ()->Unit) =
    setOnClickListener {
        d.dismiss()
        if (isChecked) andDo()   // theoretically, always isChecked(), but who knows
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

fun String.containsWords(words: List<String>, ignoreCase: Boolean): MatchWords? {
    val s = mutableListOf<Span>()
    var sumRank = 0
    var pos = 0
    for (word in words) {
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

fun String.containsOneWord(words: List<String>, ignoreCase: Boolean): Boolean {
    for (word in words) {
        if (word == "") continue
        if (indexOf(word, 0, ignoreCase) >= 0) return true
    }
    return false
}

fun Any.toUrl(): String = URLEncoder.encode(toString(), "UTF-8")

// For numbers smaller than 10, round to 1 sign. digits, otherwise round to Int
fun Double.ceilString(): String  = ceil(this).toInt().toString()
fun Double.floorString(): String = floor(this).toInt().toString()

// We use Mon=0 (i.e., ISO8601 minus 1), so the array can be indexed directly.
fun getWeekdayNames() =
    DateFormatSymbols.getInstance().weekdays.let {
        arrayOf(
            it[Calendar.MONDAY].toString(),
            it[Calendar.TUESDAY].toString(),
            it[Calendar.WEDNESDAY].toString(),
            it[Calendar.THURSDAY].toString(),
            it[Calendar.FRIDAY].toString(),
            it[Calendar.SATURDAY].toString(),
            it[Calendar.SUNDAY].toString())
   }

fun tzUtcId(mi: Int): String {
    val o = abs(mi) / 60_000
    if (o == 0) return "UTC"
    val s = if (mi < 0) "-" else "+"
    val h = o / 60
    val m = o % 60
    if (m == 0) return "UTC%s%d".format(s, h)
    return "UTC%s%02d%02d".format(s, h, m)
}
