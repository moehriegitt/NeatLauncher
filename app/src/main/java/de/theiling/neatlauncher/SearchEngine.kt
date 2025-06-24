package de.theiling.neatlauncher

import android.content.Context

class SearchUrl(
    private val container: SearchEngine,
    var name: String,
    var url: String,
    isDefault: Boolean)
{
    var isDefault = isDefault
        set(v) {
            if (field != v) {
                field = v
                container.setDefault(this, v)
            }
        }

    override fun equals(other: Any?): Boolean {
        if (other !is SearchUrl) return false
        return this.url == other.url
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }
}

class SearchEngine(private val c: Context)
{
    var version = 0
    val them = mutableListOf<SearchUrl>()

    private var defaultMaybe: SearchUrl? = null
    val default get() = defaultMaybe ?: them[0]

    fun add(name: String, url: String, isDefault: Boolean = false) {
        val e = SearchUrl(this, name, url, false)
        them.add(e)
        if (isDefault) { e.isDefault = true }
    }

    fun delete(e: SearchUrl) {
        them.remove(e)
        if (defaultMaybe == e) {
            defaultMaybe = null
        }
        if (them.isEmpty()) {
            addPredefined()
        }
    }

    fun setDefault(su: SearchUrl, active: Boolean) {
        when {
            active -> {
                defaultMaybe?.let { it.isDefault = false }
                defaultMaybe = su
            }
            defaultMaybe == su -> {
                defaultMaybe = null
            }
        }
    }

    fun savePref() {
        val s = buildString {
            append("ver\n$version\n")
            for (a in them) {
                append("nam\n${a.name}\nurl\n${a.url}\n")
                if (a.isDefault) {
                    append("def\n\n")
                }
            }
        }
        setSearchEngine(c, s)
    }

    fun loadPref() {
        them.clear()
        defaultMaybe = null
        var k: String? = null
        var name = ""
        for (v in getSearchEngine(c).split("\n")) {
            if (k == null) { k = v }
            else {
                when (k) {
                    "nam" -> name = v
                    "url" -> add(name, v)
                    "def" -> if (them.any()) them[them.lastIndex].isDefault = true
                    "ver" -> version = v.toInt()
                }
                k = null
            }
        }
        // if (them.isEmpty()) {
        //    c.shortToast("Search Engine list is empty, reloading.")
        // }
        addPredefined()
    }

    private fun addPredefined() {
        val oldVersion = version
        var act = them.isEmpty()
        version = 1
        if (act) {
            add("Startpage",  "https://www.startpage.com/sp/search?pl=opensearch&query=%s")
            add("Duckduckgo", "https://duckduckgo.com/?q=%s")
            add("Spot",       "https://spot.murena.io/?q=%s")
            add("Qwant",      "https://www.qwant.com/?q=%s")
            add("Mojeek",     "https://www.mojeek.com/search?q=%s")
            add("Wiktionary", "https://en.m.wiktionary.org/w/index.php?search=%L")
        }
        act = act || (oldVersion < 1)
        if (act) {
            add("Geo",        "geo:0,0?q=%s")
            add("Wikipedia",  "https://en.m.wikipedia.org/w/index.php?search=%s")
        }
        act = act || (oldVersion != version)
        if (act) {
            savePref()
        }
    }
}
