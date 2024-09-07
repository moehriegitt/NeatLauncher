package de.theiling.neatlauncher

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet

class NeatGridClock(
    c: Context,
    attrs: AttributeSet):
    NeatWordClockBase(c, attrs, true)
{
    private val line = c.getString(R.string.time_grid_base).trim(' ', '\n').split(" ").toTypedArray()
    private val offs = mutableListOf<Int>()
    private val grid = line.joinToString("\n")
    private val initPos = mutableMapOf<Char,MutableList<Pair<Int,Int>>>()

    private fun coordsOf(c: Char): MutableList<Pair<Int,Int>> {
        initPos[c]?.let { return it }
        initPos[c] = mutableListOf()
        return initPos[c]!!
    }

    init {
        var len = 0
        var y = 0
        for (l in line) {
            offs.add(len)
            len += l.length + 1
            var x = 0
            for (ch in l) {
                coordsOf(ch).add(Pair(x,y))
                x++
            }
            y++
        }
    }

    private fun isMatchHoriz(x: Int, y: Int, w: String) =
        try { line[y].substring(x, x + w.length) == w } catch (e: Exception) { false }

    private fun isMatchVert(x: Int, y: Int, w: String): Boolean {
        try {
            for (i in 1..w.lastIndex) {
                if (line[y+i][x] != w[i]) return false
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun markHoriz(t: SpannableStringBuilder, x: Int, y: Int, len: Int)
    {
        val i = offs[y] + x
        t.setSpan(
            ForegroundColorSpan(context.accentColor), i, i + len,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun markVert(t: SpannableStringBuilder, x: Int, y0: Int, len: Int)
    {
        for (y in y0..<y0+len) {
            val i = offs[y] + x
            t.setSpan(
                ForegroundColorSpan(context.accentColor), i, i + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun markWord(t: SpannableStringBuilder, no: Int, w: String)
    {
        var no0 = no
        for (pos in coordsOf(w[0])) {
            if (isMatchHoriz(pos.first, pos.second, w)) {
                if (--no0 == 0) {
                    markHoriz(t, pos.first, pos.second, w.length)
                }
            }
            if (isMatchVert(pos.first, pos.second, w)) {
                if (--no0 == 0) {
                    markVert(t, pos.first, pos.second, w.length)
                }
            }
        }
    }

    override fun showTime(words: List<String>)
    {
        val t = SpannableStringBuilder(grid)
        t.setSpan(
            ForegroundColorSpan(context.veryDimColor), 0, t.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        var no = 1
        for (w in words) {
            val no0 = w.toIntOrNull()
            if (no0 != null) {
                no = no0
                continue
            }
            markWord(t, no, w)
            no = 1
        }
        text = t
    }
}
