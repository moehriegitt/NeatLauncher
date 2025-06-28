package de.theiling.neatlauncher

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.Date
import java.util.Locale

fun pref(c: Context): SharedPreferences =
    c.getSharedPreferences("de.theiling.neatlauncher.PREF_MAIN", Context.MODE_PRIVATE)

fun state(c: Context): SharedPreferences =
    c.getSharedPreferences("de.theiling.neatlauncher.STATE_MAIN", Context.MODE_PRIVATE)

fun prefPutBool(p: SharedPreferences, key: String, v: Boolean, def: Boolean) =
    with (p.edit()) {
        if (v == def) {
            remove(key)
        } else {
            putBoolean(key, v)
        }
        apply()
    }

fun prefPutLong(p: SharedPreferences, key: String, v: Long, def: Long) =
    with (p.edit()) {
        if (v == def) {
            remove(key)
        } else {
            putLong(key, v)
        }
        apply()
    }

fun prefPutString(p: SharedPreferences, key: String, v: String, def: String) =
    with (p.edit()) {
        if (v == def) {
            remove(key)
        } else {
            putString(key, v)
        }
        apply()
    }

fun prefPutEnum(c: Context, p: SharedPreferences, arrId: Int, key: String, i: Int, def: Int) =
    prefPutString(p, key,
        (try {
            if (i == def) "" else c.resources.getTextArray(arrId)[i].toString()
        } catch (_: Exception) { "" }),
        "")

fun prefGetEnum(c: Context, p: SharedPreferences, arrId: Int, key: String, def: Int): Int {
    try {
        val s = p.getString(key, null)!!
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
    prefPutString(pref(c), keyItemInfo(type, pack, klass), v, def)

fun getSearchEngine(c: Context) = pref(c).getString("searchEngine", "")!!
fun setSearchEngine(c: Context, s: String) = prefPutString(pref(c), "searchEngine", s, "")

fun getWeatherLoc(c: Context) = pref(c).getString("weatherLoc", "")!!
fun setWeatherLoc(c: Context, s: String) = prefPutString(pref(c), "weatherLoc", s, "")

fun getWeatherData(c: Context) = state(c).getString("weatherData", "")!!
fun setWeatherData(c: Context, s: String) = prefPutString(state(c), "weatherData", s, "")

fun prefToJson(c: Context) = JSONObject().apply {
    put("pack", c.packageName)
    put("ver", c.packageManager.getPackageInfo(c.packageName, 0).versionCode)
    put("lang", Locale.getDefault().toString())
    put("pref", JSONObject().apply {
        for ((k,v) in pref(c).all) {
            put(k, v)
        }
    })
}

fun prefFromJson(c: Context, j: JSONObject) {
    // FIXME: Locale is currently not restored, because the Internet recommends against
    // messing with the App Locale system setting.
    // FIXME: The version code is currently not compared, but we probably should shown an
    // alert and ask when downgrading config.  Add this Alert as soon as it matters.
    with (pref(c).edit()) {
        if (j.optString("pack") != c.packageName) throw IllegalArgumentException("Not our prefs")
        val m = j.optJSONObject("pref") ?: throw IllegalArgumentException("No pref map")
        clear()
        for (k in m.keys()) {
            when (val v = m[k]) {
                is String  -> putString(k,v)
                is Boolean -> putBoolean(k,v)
                is Int     -> putInt(k,v)
                else -> throw IllegalArgumentException("Illegal pref type")
            }
        }
        commit()  // synchronous store
    }
}

abstract class PrefChoice(
    val c: Context,
    val titleId: Int,
    val names: () -> Array<String>)
{
    constructor(c: Context, titleId: Int, nameArrId: Int):
        this (c, titleId, { c.resources.getStringArray(nameArrId) })

    abstract var x: Int
    override fun toString(): String = names()[x]
}

abstract class PrefEnum(
    c: Context,
    titleId: Int,
    names: () -> Array<String>,
    private val keyArrId: Int,
    private val prefKey: String,
    private val defVal: Int,
    val onChange: (Int) -> Unit): PrefChoice(c, titleId, names)
{
    constructor(c: Context, titleId: Int, nameArrId: Int, keyArrId: Int,
        prefKey: String, defVal: Int, onChange: (Int) -> Unit): this (
        c, titleId, { c.resources.getStringArray(nameArrId) }, keyArrId, prefKey, defVal, onChange)

    override var x = prefGetEnum(c, pref(c), keyArrId, prefKey, defVal)
        set(new) {
            if (field != new) {
                field = new
                prefPutEnum(c, pref(c), keyArrId, prefKey, field, defVal)
                onChange(field)
            }
        }
}

abstract class PrefWeekday(
    c: Context,
    titleId: Int,
    prefKey: String,
    defVal: Int,
    onChange: (Int) -> Unit): PrefEnum(c, titleId, { getWeekdayNames() },
        R.array.weekday_choice_key, prefKey, defVal, onChange)

abstract class PrefBool(
    c: Context,
    titleId: Int,
    nameArrId: Int,
    private val prefKey: String,
    private val defVal: Boolean,
    val onChange: (Int) -> Unit = {}): PrefChoice(c, titleId, nameArrId)
{
    override var x = if (pref(c).getBoolean(prefKey, defVal)) 1 else 0
        set(new) {
            val newI = if (new > 0) 1 else 0
            if (field != newI) {
                field = newI
                prefPutBool(pref(c), prefKey, field > 0, defVal)
                onChange(new) // pass exact int value
            }
        }
}

abstract class PrefSimpleBool(
    val c: Context,
    private val prefKey: String,
    private val defVal: Boolean)
{
    var x = if (pref(c).getBoolean(prefKey, defVal)) 1 else 0
        set(new) {
            val newI = if (new > 0) 1 else 0
            if (field != newI) {
                field = newI
                prefPutBool(pref(c), prefKey, field > 0, defVal)
            }
        }
}

abstract class StateDate(
    private val c: Context,
    private val prefKey: String)
{
    var date = Date(state(c).getLong(prefKey, 0) ?: 0)
        set(new) {
            field = new
            prefPutLong(state(c), prefKey, new.time, 0)
        }
    var time: Long
        get() = date.time
        set(new) { date = Date(new) }
}

class EnumDate(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.date_choice_title,
    R.array.date_choice, R.array.date_choice_key, "dateChoice", date_yyyy, onChange)

class EnumTime(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.time_choice_title,
    R.array.time_choice, R.array.time_choice_key, "timeChoice", time_Hmmx, onChange)

class EnumBack(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.back_choice_title,
    R.array.back_choice, R.array.back_choice_key, "backChoice", back_opaq, onChange)

class EnumSwipe(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.swipe_choice_title,
    R.array.five_choice, R.array.five_choice_key, "swipeChoice", five_x2, onChange)

class EnumFont(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.font_choice_title,
    R.array.font_choice, R.array.font_choice_key, "fontChoice", font_ubun, onChange)

class EnumColor(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.color_choice_title,
    R.array.color_choice, R.array.color_choice_key, "colorChoice", color_ambr, onChange)

class EnumTemp(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.temp_choice_title,
    R.array.temp_choice, R.array.temp_choice_key, "tempChoice", temp_C, onChange)

class EnumTtype(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.ttype_choice_title,
    R.array.ttype_choice, R.array.ttype_choice_key, "tempTypeChoice", ttype_actu, onChange)

class EnumTweath(c: Context, onChange: (Int) -> Unit): PrefEnum(c, R.string.tweath_choice_title,
    R.array.tweath_choice, R.array.tweath_choice_key, "weathTypeChoice", tweath_all, onChange)

class BoolContact(c: Context, onChange: (Int) -> Unit): PrefBool(c,
    R.string.contact_choice_title, R.array.contact_choice, "readContacts", true, onChange)

class BoolNotif(c: Context): PrefSimpleBool(c, "readNotif", true)

class EnumWstart(c: Context, onChange: (Int) -> Unit): PrefWeekday(c,
    R.string.wstart_choice_title, "weekStart", weekday_mon, onChange)

class StateWeatherTryNext(c: Context): StateDate(c, "weathTryNext")
class StateWeatherTryLast(c: Context): StateDate(c, "weathTryLast")
