package de.theiling.neatlauncher

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import java.util.Calendar
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt

abstract class NeatWordClockBase(
    context: Context,
    attrs: AttributeSet,
    griddy: Boolean = false):
    AppCompatTextView(context, attrs)
{
    private val priExcept: Int = if (griddy) R.array.time_grid_except else R.array.time_word_except
    private val secExcept: Int = if (griddy) R.array.time_word_except else R.array.time_grid_except
    private val priMin:    Int = if (griddy) R.array.time_grid_min    else R.array.time_word_min
    private val secMin:    Int = if (griddy) R.array.time_word_min    else R.array.time_grid_min
    private val priHour:   Int = if (griddy) R.array.time_grid_hour   else R.array.time_word_hour
    private val secHour:   Int = if (griddy) R.array.time_word_hour   else R.array.time_grid_hour

    private fun arr(id: Int): Array<CharSequence> = context.resources.getTextArray(id)

    private fun exceptList(): Array<CharSequence> {
        val pri = arr(priExcept)
        if (pri.isNotEmpty()) { return pri }
        return arr(secExcept)
    }

    private fun minStr(i: Int): CharSequence {
        val pri = arr(priMin)
        val sec = arr(secMin)
        val j = (i * max(pri.size, sec.size)) / 60
        if (j < pri.size) { return pri[j] }
        if (j < sec.size) { return sec[j] }
        return ""
    }

    private fun hourStr(i: Int): CharSequence {
        val j = i % 12
        val pri = arr(priHour)
        if (pri != null) {
            val s = pri.getOrNull(i) ?: pri.getOrNull(j) ?: ""
            if (s != "") return s;
        }
        val sec = arr(secHour)
        if (sec != null) {
            val s = sec.getOrNull(i) ?: sec.getOrNull(j) ?: ""
            if (s != "") return s;
        }
        return ""
    }

    private fun getWords(h24exact: Int, m60exact: Int): List<String> {
        val cnt = max(arr(priMin).size, arr(secMin).size)
        if (cnt == 0) {
            return mutableListOf()
        }
        val per = 60.0 / cnt

        val m60plus = (round(m60exact / per) * per).roundToInt()
        val m60 = m60plus % 60
        val h24 = h24exact + (m60plus / 60)

        val str =
            (if (h24 < 10) "0" else "") + "$h24" +
            (if (m60 < 10) "0" else "") + "$m60"

        // exception list first
        for (l in exceptList()) {
            val v = l.split(" ")
            val w = v.filter { it != str }
            if (w.size < v.size) return w
        }

        // word minute list
        val r = mutableListOf<String>()
        for (e in minStr(m60).split(" ")) {
            when (e) {
                "=" -> hourStr(h24).split(" ").forEach { r.add(it) }
                "+" -> hourStr((h24 + 1) % 24).split(" ").forEach { r.add(it) }
                "-" -> hourStr((h24 + 23) % 24).split(" ").forEach { r.add(it) }
                else -> r.add(e)
            }
        }
        return r
    }

    fun updateTime()
    {
        val c = Calendar.getInstance()
        showTime(getWords(c[Calendar.HOUR], c[Calendar.MINUTE]))
    }

    abstract fun showTime(words: List<String>)
}

open class NeatWordClock(
    context: Context,
    attrs: AttributeSet):
    NeatWordClockBase(context, attrs, false)
{
    override fun showTime(words: List<String>)
    {
        text = words.filter { it.toIntOrNull() == null }.joinToString(" ")
    }
}
