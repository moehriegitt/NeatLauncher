package de.theiling.neatlauncher

import android.content.Context
import android.util.TypedValue

val Context.accentColor: Int get() {
    val tv = TypedValue()
    theme.resolveAttribute(android.R.attr.colorAccent, tv, true)
    return tv.data
}

val Context.mainForeground: Int get() = getColor(R.color.mainForeground)
val Context.veryDimColor: Int get() = getColor(R.color.veryDimColor)
