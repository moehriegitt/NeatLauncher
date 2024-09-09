package de.theiling.neatlauncher

import android.content.Context
import android.content.SharedPreferences

const val PREF_MAIN = "de.theiling.neatlauncher.PREF_MAIN"

fun pref(c: Context): SharedPreferences =
    c.getSharedPreferences(PREF_MAIN, Context.MODE_PRIVATE)

fun prefPutBool(c: Context, key: String, v: Boolean, def: Boolean) =
    with (pref(c).edit()) {
        if (v == def) {
            remove(key)
        } else {
            putBoolean(key, v)
        }
        apply()
    }

fun prefPutString(c: Context, key: String, v: String, def: String) =
    with (pref(c).edit()) {
        if (v == def) {
            remove(key)
        } else {
            putString(key, v)
        }
        apply()
    }

fun prefPutEnum(c: Context, arrId: Int, key: String, i: Int, def: Int) =
    prefPutString(c, key,
        (try {
            if (i == def) "" else c.resources.getTextArray(arrId)[i].toString()
        } catch (_: Exception) { "" }),
        "")

fun prefGetEnum(c: Context, arrId: Int, key: String, def: Int): Int {
    try {
        val s = pref(c).getString(key, null)!!
        c.resources.getTextArray(arrId).forEachIndexed { i, v ->
            if (v.toString() == s) {
                return i
            }
        }
    } catch (_: Exception) { /* nothing */}
    return def
}

fun keyItemInfo(type: String, pack: String, klass: String) = "item/$type/$pack/$klass"

fun getItemInfo(c: Context, type: String, pack: String, klass: String): String? =
    pref(c).getString(keyItemInfo(type, pack, klass), null)

fun setItemInfo(c: Context, type: String, pack: String, klass: String, v: String, def: String) =
    prefPutString(c, keyItemInfo(type, pack, klass), v, def)

fun getSearchEngine(c: Context) = pref(c).getString("searchEngine", "")!!
fun setSearchEngine(c: Context, s: String) = prefPutString(c, "searchEngine", s, "")

fun getReadContacts(c: Context) = pref(c).getBoolean("readContacts", true)
fun setReadContacts(c: Context, i: Boolean) = prefPutBool(c, "readContacts", i, true)

interface PrefInt {
    var x: Int
}

abstract class PrefEnum(
    private val c: Context,
    val titleId: Int,
    val nameArrId: Int,
    private val keyArrId: Int,
    private val prefKey: String,
    private val defVal: Int,
    val onChange: (Int) -> Unit): PrefInt
{
    override var x = prefGetEnum(c, keyArrId, prefKey, defVal)
        set(new) {
            if (field != new) {
                field = new
                prefPutEnum(c, keyArrId, prefKey, field, defVal)
                onChange(field)
            }
        }
}

class EnumDate(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.date_choice_title,
    R.array.date_choice, R.array.date_choice_key, "dateChoice", date_yyyy, onChange)

class EnumTime(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.time_choice_title,
    R.array.time_choice, R.array.time_choice_key, "timeChoice", time_Hmmx, onChange)

class EnumBack(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.back_choice_title,
    R.array.back_choice, R.array.back_choice_key, "backChoice", back_opaq, onChange)

class EnumFont(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.font_choice_title,
    R.array.font_choice, R.array.font_choice_key, "fontChoice", font_ubun, onChange)

class EnumColor(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.color_choice_title,
    R.array.color_choice, R.array.color_choice_key, "colorChoice", color_ambr, onChange)
