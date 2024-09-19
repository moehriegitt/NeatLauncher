package de.theiling.neatlauncher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.SimpleTimeZone
import java.util.TimeZone
import kotlin.math.roundToInt

const val CURRENT_LOC = ""

const val SEARCH_LOC_URL =
    "https://geocoding-api.open-meteo.com/v1/search?format=json&language=%s&count=%s&name=%s"

const val WEATHER_FORECAST_URL =
    "https://api.open-meteo.com/v1/forecast" +
    "?latitude=%s" +
    "&longitude=%s" +
    "&forecast_minutely_15=12" +
    "&minutely_15=temperature_2m,apparent_temperature,weather_code" +
    "&forecast_hours=12" +
    "&hourly=temperature_2m,apparent_temperature,weather_code" +
    "&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset" +
    "&timeformat=unixtime" +
    "&timezone=auto"

class WeatherLoc(
    private val container: WeatherEngine,
    name: String,
    val lat: Double,
    val lon: Double,
    isActive: Boolean)
{
    var name = name
        set(v) {
            if ((field != CURRENT_LOC) && (v != CURRENT_LOC) && (field != v)) {
                field = v
                container.touch(this)
            }
        }

    val isCurrent get() = (name == CURRENT_LOC)

    var isActive = isActive
        set(v) {
            if (field != v) {
                field = v
                container.setActive(this, v)
            }
        }

    var orderOrEmpty = ""
        private set
    val haveOrder get() = orderOrEmpty != ""
    var order get() = if (haveOrder) orderOrEmpty else name
        set(v) {
            val old = orderOrEmpty
            orderOrEmpty = if (v != name) v else ""
            if (old != orderOrEmpty) { container.touch(this) }
        }

    fun displayCompareTo(that: WeatherLoc) = this.order.compareTo(that.order)
}

class WeatherEngine(private val c: Context)
{
    val them = mutableListOf<WeatherLoc>()
    private var currentMaybe: WeatherLoc? = null
    val current get() = currentMaybe!!
    var active: WeatherLoc? = null; private set
    var modified = false; private set
    var activeModified = false; private set

    fun touch(which: WeatherLoc) {
        modified = true
        if (which == active) activeModified = true
    }

    fun add(name: String, lat: Double, lon: Double, isActive: Boolean = false) {
        val e = WeatherLoc(this, name, lat, lon, false)
        if (e.isCurrent) {
            if (currentMaybe != null) return
            currentMaybe = e
        }
        them.add(e)
        if (isActive) {
            e.isActive = true
        }
        modified = true
    }

    fun delete(e: WeatherLoc) {
        if (e.isCurrent) return
        them.remove(e)
        if (active == e) {
            active = null
            activeModified = true
        }
        if (currentMaybe == e) {
            currentMaybe = null
        }
        modified = true
    }

    fun setActive(su: WeatherLoc, isActive: Boolean) {
        when {
            isActive -> {
                if (active == su) return
                active?.let { it.isActive = false }
                active = su
            }
            active == su -> {
                active = null
            }
            else -> return
        }
        modified = true
        activeModified = true
    }

    fun savePref() {
        them.sortWith { a,b -> a.displayCompareTo(b) }
        val s = buildString {
            for (a in them) {
                append("lat\n${a.lat}\nlon\n${a.lon}\nnam\n${a.name}\n")
                if (a.haveOrder) {
                    append("ord\n${a.order}\n")
                }
                if (a.isActive) {
                    append("act\n\n")
                }
            }
        }
        setWeatherLoc(c, s)
        modified = false
        activeModified = false
    }

    fun loadPref() {
        them.clear()
        currentMaybe = null
        active = null
        var k: String? = null
        var lat = 0.0
        var lon = 0.0
        for (v in getWeatherLoc(c).split("\n")) {
            if (k == null) { k = v }
            else {
                when (k) {
                    "nam" -> add(v, lat, lon)
                    "lat" -> lat = v.toDouble()
                    "lon" -> lon = v.toDouble()
                    "act" -> if (them.any()) them[them.lastIndex].isActive = true
                    "ord" -> if (them.any()) them[them.lastIndex].order = v
                }
                k = null
            }
        }
        if (currentMaybe == null) {
            add(CURRENT_LOC, 0.0, 0.0)
        }
        currentMaybe!!
        modified = false
        activeModified = false
    }
}

/* Temperature is stored in 100th of a Kelvin */
data class Temp(val k100th: Int) {
    companion object {
        fun kelvinOf (x: Double) = Temp((x * 100.0).roundToInt())
        fun celsiusOf(x: Double) = kelvinOf(x + 273.15)
        fun fahrenOf (x: Double) = kelvinOf(((x + 459.67) / 9) * 5)
    }
    val raw get() = k100th
    val kelvin  get() = k100th / 100.0
    val celsius get() = kelvin - 273.15
    val fahren  get() = ((kelvin / 5) * 9) - 459.67

    operator fun get(unit: EnumTemp) = when (unit.x) {
        temp_K -> kelvin
        temp_F -> fahren
        else   -> celsius
    }
}

// weather code WMO; restricted by open-meteo docs
data class WeatherCode(val wmo: Int) {
    override fun toString() = when (wmo) {
        0  -> "\u2600\ufe0e"  // clear sky
        1  -> "\u2600\ufe0e`" // mainly clear
        2  -> "\u26c5\ufe0e"  // partly cloudy
        3  -> "\u2601\ufe0e"  // overcast
        45 -> "="             // fog
        48 -> "\u2A6E"        // depositing rime fog
        51 -> "’"             // drizzle: light
        53 -> "”"             // drizzle: moderate
        55 -> "’”"            // drizzle: dense
        56 -> "’!"            // freezing drizzle: light
        57 -> "”!"            // freezing drizzle: dense
        61 -> "··"            // rain: slight
        63 -> "∴"             // rain: moderate
        65 -> "⁘"             // rain: heavy
        66 -> "!"             // freezing rain: light
        67 -> "‼"             // freezing rain: heavy
        71 -> "*"             // snow fall: slight
        73 -> "**"            // snow fall: moderate
        75 -> "⁂"            // snow fall: heavy
        77 -> "\u2744\uFE0E"  // snow grains
        80 -> "∶"             // rain showers: slight
        81 -> "∷"             // rain showers: moderate
        82 -> "⁝⁝"             // rain showers: violent
        85 -> "⁑"             // snow showers: slight
        86 -> "⁑⁑"            // snow showers: heavy
        95 -> "☈"             // thunderstorm: slight or moderate
        96 -> "☈."            // thunderstorm: with slight hail
        99 -> "☈!"            // thunderstorm: with heavy hail
        else -> "?"           // unknown weather code
    }
}

/* Units used for internal storage:
 *    temp   :: Int  @ 100th of a Kelvin
 *    code   :: Int  @ WMO weather code (0..99)
 *    dur    :: Int  @ seconds
 */
data class WeatherStep(
    val start: Date,
    val durSec: Int,
    val code:   WeatherCode,
    val tAbs:   Temp,
    val tApp:   Temp)
{
    val end  get() = Date(start.time + (durSec * 1000L))
    val last get() = Date(end.time - 1)
}

/* A weather day has additional min/max ranges,
 * dur is a constant 86400,
 *  and it has a sunrise/sunset */
data class WeatherDay(
    val start:   Date,
    val code:    WeatherCode,
    val tAbsMin: Temp,
    val tAbsMax: Temp,
    val tAppMin: Temp,
    val tAppMax: Temp,
    val sunRise: Date,
    val sunSet:  Date)
{
    val durSec get() = 86400
    val end  get() = Date(start.time + (durSec * 1000L))
    val last get() = Date(end.time - 1)

    fun tMax(unit: EnumTtype) = when (unit.x) {
        ttype_actu -> tAbsMax
        else       -> tAppMax
    }
    fun tMin(unit: EnumTtype) = when (unit.x) {
        ttype_actu -> tAbsMin
        else       -> tAppMin
    }
}

class WeatherData(
    private val c: Context,
    val timeStamp: Date,
    val timeZone: SimpleTimeZone,
    val step: List<WeatherStep> = listOf(),
    val day: List<WeatherDay> = listOf())
{
    companion object {
        private fun simpleTz(x: Int = 0) = SimpleTimeZone(x, tzUtcId(x))

        private fun <T> meteoList(
            j: JSONObject, group: String, entry: String, make: (JSONArray, Int)->T): List<T> {
            val a = j.getJSONObject(group).getJSONArray(entry)
            val r = mutableListOf<T>()
            for (i in 0 until a.length()) { r.add(make(a,i)) }
            return r
        }

        private fun meteoTemp(j: JSONObject, group: String, entry: String): List<Temp> {
            val make = when (j.getJSONObject("${group}_units").getString(entry)) {
                "°F" -> Temp::fahrenOf
                else -> Temp::celsiusOf
            }
            return meteoList(j, group, entry) { a,i -> make(a.getDouble(i)) }
        }

        private fun meteoCode(j: JSONObject, group: String, entry: String) =
            meteoList(j, group, entry) { a, i -> WeatherCode(a.getInt(i)) }

        private fun meteoLong(j: JSONObject, group: String, entry: String) =
            meteoList(j, group, entry) { a, i -> a.getLong(i) }

        private fun meteoDate(j: JSONObject, group: String, entry: String) =
            meteoList(j, group, entry) { a, i -> Date(a.getLong(i) * 1000L) }

        fun fromOpenMeteo(context: Context, j: JSONObject): WeatherData {
            val tz = simpleTz(1000 * j.getInt("utc_offset_seconds"))
            val day = mutableListOf<WeatherDay>()
            with (day) {
                val start   = meteoDate(j, "daily", "time")
                val code    = meteoCode(j, "daily", "weather_code")
                val tAbsMin = meteoTemp(j, "daily", "temperature_2m_min")
                val tAbsMax = meteoTemp(j, "daily", "temperature_2m_max")
                val tAppMin = meteoTemp(j, "daily", "apparent_temperature_min")
                val tAppMax = meteoTemp(j, "daily", "apparent_temperature_max")
                val sunRise = meteoDate(j, "daily", "sunrise")
                val sunSet  = meteoDate(j, "daily", "sunset")
                for (i in code.indices) {
                    add(WeatherDay(start[i], code[i], tAbsMin[i], tAbsMax[i],
                        tAppMin[i], tAppMax[i], sunRise[i], sunSet[i]))
                }
            }
            day.sortWith { a,b -> a.start.compareTo(b.start) }

            val step = mutableListOf<WeatherStep>()
            with (step) {
                val start = meteoDate(j, "minutely_15", "time")
                val code  = meteoCode(j, "minutely_15", "weather_code")
                val tAbs  = meteoTemp(j, "minutely_15", "temperature_2m")
                val tApp  = meteoTemp(j, "minutely_15", "apparent_temperature")
                for (i in code.indices) {
                    add(WeatherStep(start[i], 15*60, code[i], tAbs[i], tApp[i]))
                }
            }
            step.sortWith { a,b -> a.start.compareTo(b.start) }

            with (step) {
                val start = meteoDate(j, "hourly", "time")
                val code  = meteoCode(j, "hourly", "weather_code")
                val tAbs  = meteoTemp(j, "hourly", "temperature_2m")
                val tApp  = meteoTemp(j, "hourly", "apparent_temperature")
                for (i in code.indices) {
                    if (step.isEmpty() || (start[i] >= step[step.lastIndex].end)) {
                        add(WeatherStep(start[i], 60*60, code[i], tAbs[i], tApp[i]))
                    }
                }
            }
            step.sortWith { a,b -> a.start.compareTo(b.start) }

            return WeatherData(context, Date(), tz, step, day)
        }

        fun loadPref(context: Context): WeatherData {
            val day = mutableListOf<WeatherDay>()
            val step = mutableListOf<WeatherStep>()
            var k: String? = null
            var c = WeatherCode(0)
            var update = Date()
            var s = Date(0)
            var sRise = Date(0)
            var sSet = Date(0)
            var tAbs = Temp(0)
            var tAbsMax = Temp(0)
            var tApp = Temp(0)
            var tAppMax = Temp(0)
            var tz = simpleTz()
            for (v in getWeatherData(context).split("\n")) {
                if (k == null) { k = v; continue }
                when (k) {
                    "D" -> day.add(WeatherDay(s,c, tAbs, tAbsMax, tApp, tAppMax, sRise, sSet))
                    "d" -> step.add(WeatherStep(s, v.toInt(), c, tAbs, tApp))
                    "s" -> s = Date(v.toLong())
                    "c" -> c = WeatherCode(v.toInt())
                    "u" -> update = Date(v.toLong())
                    "R" -> sRise = Date(v.toLong())
                    "S" -> sSet = Date(v.toLong())
                    "t" -> tAbs = Temp(v.toInt())
                    "T" -> tAbsMax = Temp(v.toInt())
                    "a" -> tApp = Temp(v.toInt())
                    "A" -> tAppMax = Temp(v.toInt())
                    "z" -> tz = simpleTz(v.toInt())
                }
                k = null
            }
            return WeatherData(context, update, tz, step, day)
        }
    }

    fun savePref() {
        val s = buildString {
            append("u\n${timeStamp.time}\n")
            append("z\n${timeZone.rawOffset}\n")
            for (i in day) {
                append("s\n${i.start.time}\n")
                append("c\n${i.code.wmo}\n")
                append("R\n${i.sunRise.time}\n")
                append("S\n${i.sunSet.time}\n")
                append("t\n${i.tAbsMin.raw}\n")
                append("T\n${i.tAbsMax.raw}\n")
                append("a\n${i.tAppMin.raw}\n")
                append("A\n${i.tAppMax.raw}\n")
                append("D\n\n")
            }
            for (i in step) {
                append("s\n${i.start.time}\n")
                append("c\n${i.code.wmo}\n")
                append("t\n${i.tAbs.raw}\n")
                append("a\n${i.tApp.raw}\n")
                append("d\n${i.durSec}\n")
            }
        }
        setWeatherData(c, s)
    }

    fun clearPref() {
        setWeatherData(c, "")
    }
}