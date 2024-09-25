package de.theiling.neatlauncher

import android.content.Context

const val ITEM_TYPE_APP = "app"
const val ITEM_TYPE_INT = "int"
const val ITEM_TYPE_PIN = "pin"
const val ITEM_TYPE_SHORT = "sht"
const val ITEM_TYPE_CONTACT = "con"

const val ITEM_PIN_HOME = 1
const val ITEM_PIN_DOWN = 2
const val ITEM_PIN_LEFT = 4
const val ITEM_PIN_RIGHT = 8
const val ITEM_PIN_TIME = 16
const val ITEM_PIN_DATE = 32
const val ITEM_PIN_WEATH = 64
const val ITEM_PIN_BGRD = 128
const val ITEM_PIN_UP = 256

fun itemDefaultHidden(type: String) =
    (type == ITEM_TYPE_SHORT) ||
    (type == ITEM_TYPE_CONTACT)

fun typeRank(type: String): Int
{
    if (type == ITEM_TYPE_CONTACT) return 1
    return 0
}

class ItemInfo(private val defaultHidden: Boolean, confStr: String)
{
    var label = ""
    var order = ""
    var hidden = defaultHidden
    var pinned = 0

    init {
        var k : String? = null
        for (v in confStr.split("\n")) {
            if (k == null) {
                when (v) {
                    "hid0" -> hidden = false
                    "hid1" -> hidden = true
                    "pin1" -> pinned = ITEM_PIN_HOME
                    else -> k = v
                }
                continue
            }
            when (k) {
                "labl" -> label = v
                "ordr" -> order = v
                "pind" -> pinned = v.toInt()
            }
            k = null
        }
    }

    override fun toString(): String =
        (if (hidden == defaultHidden) "" else if (hidden) "hid1\n" else "hid0\n") +
        (if (label != "") "labl\n$label\n" else "") +
        (if (order != "") "ordr\n$order\n" else "") +
        (if (pinned != 0)  "pind\n$pinned\n" else "")
}

class Item(
    private val c: Context,
    type: String,
    parent: Item?,
    origLabelSuffixRaw: String,
    pack: String,
    act: String,
    val uid: Long)
{
    data class Data(val type: String, val pack: String, val act: String)
    private val data = Data(type, pack, act)

    val type get() = data.type
    val pack get() = data.pack
    val act get() = data.act

    override fun hashCode() = data.hashCode()
    override fun equals(other: Any?) = data == (other as? Item)?.data

    private val parentLabelPrefix: String = parent?.let { it.label + ": " } ?: ""
    private val parentOrderPrefix: String = parent?.let { it.order + ": " } ?: ""

    private val origLabelSuffix = if (origLabelSuffixRaw == "") pack else origLabelSuffixRaw

    val origLabel: String = parentLabelPrefix + origLabelSuffix
    private val origOrder: String = parentOrderPrefix + origLabelSuffix

    val pref =
        ItemInfo(itemDefaultHidden(type), getItemInfo(c, type, pack, act) ?: "")

    var label: String
        get() = (if (pref.label != "") pref.label else origLabel)
        set(v) {
            pref.label = (if (v == origLabel) "" else v)
            prefSet()
        }

    private val baseLabel get() = label.removePrefix(parentLabelPrefix)

    var order: String
        get() = (if (pref.order != "") pref.order else defaultOrder())
        set(v) {
            pref.order = (if (v == defaultOrder()) "" else v)
            prefSet()
        }

    var hidden: Boolean
        get() = pref.hidden
        set(v) {
            pref.hidden = v
            prefSet()
        }

    var pinned: Int
        get() = pref.pinned
        set(v) {
            pref.pinned = v
            prefSet()
        }

    private fun defaultOrder() =
        if ((parentLabelPrefix != "") && label.startsWith(parentLabelPrefix))
            origOrder
        else
            label

    private fun prefSet() =
        setItemInfo(c, type, pack, act, pref.toString(), "")

    val shortcuts = mutableListOf<Item>()

    val color get() = when (type) {
        ITEM_TYPE_CONTACT -> c.mainForeground
        else -> c.accentColor
    }

    fun displayCompareTo(other: Item): Int
    {
        var i = this.order.compareTo(other.order, true)
        if (i != 0) return i

        i = this.order.compareTo(other.order)
        if (i != 0) return i

        i = this.act.compareTo(other.act)
        if (i != 0) return i

        i = this.uid.compareTo(other.uid)
        if (i != 0) return i

        return 0
    }

    fun displayCompareToPrep(other: Item, prep: (String) -> String): Int
    {
        val t = prep(this.order)
        val o = prep(other.order)

        var i = t.compareTo(o, true)
        if (i != 0) return i

        i = t.compareTo(o)
        if (i != 0) return i

        return this.displayCompareTo(other)
    }

    fun displayCompareToMatch(other: Item): Int
    {
        val u = typeRank(this.type)
        val v = typeRank(other.type)
        val j = u.compareTo(v)
        if (j != 0) return j

        return this.displayCompareTo(other)
    }

    fun containsWords(needle: CharSequence, ignoreCase: Boolean): MatchWords? {
        val words = needle.split(" ")
        val r = label.containsWords(words, ignoreCase) ?: return null
        if ((parentLabelPrefix != "") &&
            words.isNotEmpty() &&
            !baseLabel.containsOneWord(words, ignoreCase)) return null
        return r
    }
}
