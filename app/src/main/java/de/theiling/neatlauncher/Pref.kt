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

fun prefPutInt(c: Context, key: String, v: Int, def: Int) =
    with (pref(c).edit()) {
        if (v == def) {
            remove(key)
        } else {
            putInt(key, v)
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

fun prefPutEnum(c: Context, arrId: Int, key: String, i: Int) =
    prefPutString(c, key,
        (try { c.resources.getTextArray(arrId)[i].toString() } catch (e: Exception) { "" }),
        "")

fun prefGetEnum(c: Context, arrId: Int, key: String, def: Int): Int {
    try {
        val s = pref(c).getString(key, null)!!
        c.resources.getTextArray(arrId).forEachIndexed { i, v ->
            if (v.toString() == s) {
                return i
            }
        }
    } catch (e: Exception) { /* nothing */}
    return def
}

fun keyItemInfo(type: String, pack: String, klass: String) = "item/$type/$pack/$klass"

fun getItemInfo(c: Context, type: String, pack: String, klass: String): String? =
    pref(c).getString(keyItemInfo(type, pack, klass), null)

fun setItemInfo(c: Context, type: String, pack: String, klass: String, v: String, def: String) =
    prefPutString(c, keyItemInfo(type, pack, klass), v, def)

fun getSearchEngine(c: Context) = pref(c).getString("searchEngine", "")!!
fun setSearchEngine(c: Context, s: String) = prefPutString(c, "searchEngine", s, "")

fun getDateChoice(c: Context) = prefGetEnum(c, R.array.date_choice_key, "dateChoice", date_yyyy)
fun setDateChoice(c: Context, i: Int) = prefPutEnum(c, R.array.date_choice_key, "dateChoice", i)

fun getTimeChoice(c: Context) = prefGetEnum(c, R.array.time_choice_key, "timeChoice", time_Hmmx)
fun setTimeChoice(c: Context, i: Int) = prefPutEnum(c, R.array.time_choice_key, "timeChoice", i)

fun getBackChoice(c: Context) = prefGetEnum(c, R.array.back_choice_key, "backChoice", back_opaq)
fun setBackChoice(c: Context, i: Int) = prefPutEnum(c, R.array.back_choice_key, "backChoice", i)

fun getFontChoice(c: Context) = prefGetEnum(c, R.array.font_choice_key, "fontChoice", font_ubun)
fun setFontChoice(c: Context, i: Int) = prefPutEnum(c, R.array.font_choice_key, "fontChoice", i)

fun getColorChoice(c: Context) = prefGetEnum(c, R.array.color_choice_key, "colorChoice",color_ambr)
fun setColorChoice(c: Context, i: Int) = prefPutEnum(c, R.array.color_choice_key, "colorChoice", i)

fun getReadContacts(c: Context) = pref(c).getBoolean("readContacts", true)
fun setReadContacts(c: Context, i: Boolean) = prefPutBool(c, "readContacts", i, true)
