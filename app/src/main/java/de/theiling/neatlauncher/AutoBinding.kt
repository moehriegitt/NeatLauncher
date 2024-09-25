// Autogenerated by mkresource.pl, do not edit
@file:Suppress("MemberVisibilityCanBePrivate")

package de.theiling.neatlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class AboutDialogBinding(
    rootView: LinearLayout,
    val author: TextView,
    val authorBox: LinearLayout,
    val license: TextView,
    val licenseBox: LinearLayout,
    val oMeteoBox: LinearLayout,
    val packageBox: LinearLayout,
    val packageLink: TextView,
    val sourceBox: LinearLayout,
    val sourceLink: TextView,
    val ubuntuBox: LinearLayout,
    val versionBox: LinearLayout,
    val versionName: TextView):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = AboutDialogBinding(
            rootView as LinearLayout,
            rootView.findViewById(R.id.author)!!,
            rootView.findViewById(R.id.author_box)!!,
            rootView.findViewById(R.id.license)!!,
            rootView.findViewById(R.id.license_box)!!,
            rootView.findViewById(R.id.o_meteo_box)!!,
            rootView.findViewById(R.id.package_box)!!,
            rootView.findViewById(R.id.package_link)!!,
            rootView.findViewById(R.id.source_box)!!,
            rootView.findViewById(R.id.source_link)!!,
            rootView.findViewById(R.id.ubuntu_box)!!,
            rootView.findViewById(R.id.version_box)!!,
            rootView.findViewById(R.id.version_name)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): AboutDialogBinding
        {
            val root = inflater.inflate(R.layout.about_dialog, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class ItemActionsBinding(
    rootView: LinearLayout,
    val itemHide: TextView,
    val itemInfo: TextView,
    val itemPin: TextView,
    val itemRemove: TextView,
    val itemRename: TextView,
    val itemRid: TextView,
    val itemShortcuts: LinearLayout,
    val itemShow: TextView,
    val itemStart: TextView,
    val shortcutList: LinearLayout):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = ItemActionsBinding(
            rootView as LinearLayout,
            rootView.findViewById(R.id.item_hide)!!,
            rootView.findViewById(R.id.item_info)!!,
            rootView.findViewById(R.id.item_pin)!!,
            rootView.findViewById(R.id.item_remove)!!,
            rootView.findViewById(R.id.item_rename)!!,
            rootView.findViewById(R.id.item_rid)!!,
            rootView.findViewById(R.id.item_shortcuts)!!,
            rootView.findViewById(R.id.item_show)!!,
            rootView.findViewById(R.id.item_start)!!,
            rootView.findViewById(R.id.shortcut_list)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): ItemActionsBinding
        {
            val root = inflater.inflate(R.layout.item_actions, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class LocCurDialogBinding(
    rootView: FrameLayout,
    val editLabel: EditText,
    val editOrder: EditText,
    val geoLink: TextView):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = LocCurDialogBinding(
            rootView as FrameLayout,
            rootView.findViewById(R.id.edit_label)!!,
            rootView.findViewById(R.id.edit_order)!!,
            rootView.findViewById(R.id.geo_link)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): LocCurDialogBinding
        {
            val root = inflater.inflate(R.layout.loc_cur_dialog, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class LocDialogBinding(
    rootView: FrameLayout,
    val editDel: RadioButton,
    val editKeep: RadioButton,
    val editLabel: EditText,
    val editOrder: EditText,
    val geoLink: TextView):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = LocDialogBinding(
            rootView as FrameLayout,
            rootView.findViewById(R.id.edit_del)!!,
            rootView.findViewById(R.id.edit_keep)!!,
            rootView.findViewById(R.id.edit_label)!!,
            rootView.findViewById(R.id.edit_order)!!,
            rootView.findViewById(R.id.geo_link)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): LocDialogBinding
        {
            val root = inflater.inflate(R.layout.loc_dialog, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class MainActivityBinding(
    val content: ConstraintLayout,
    val clockAnalog: NeatAnalogClock,
    val clockBox: LinearLayout,
    val clockDigital: TextView,
    val clockGrid: NeatGridClock,
    val clockWord: NeatWordClock,
    val datetimeTopMargin: View,
    val drawerRecycler: RecyclerView,
    val gridB0: TextView,
    val gridB1: TextView,
    val gridB2: TextView,
    val gridB3: TextView,
    val gridB4: TextView,
    val gridB5: TextView,
    val gridB6: TextView,
    val gridBu: TextView,
    val gridC0: TextView,
    val gridC1: TextView,
    val gridC2: TextView,
    val gridC3: TextView,
    val gridC4: TextView,
    val gridC5: TextView,
    val gridC6: TextView,
    val gridCu: TextView,
    val gridE0: TextView,
    val gridE1: TextView,
    val gridE2: TextView,
    val gridE3: TextView,
    val gridE4: TextView,
    val gridE5: TextView,
    val gridE6: TextView,
    val gridEu: TextView,
    val gridS0: View,
    val gridS1: View,
    val gridS2: View,
    val gridS3: View,
    val gridS4: View,
    val gridS5: View,
    val gridS6: View,
    val homeRecycler: RecyclerView,
    val mainDate: TextView,
    val mainHead: LinearLayout,
    val mainSearch: EditText,
    val mainSearchOpt: TextView,
    val weatherBox: HorizontalScrollView,
    val weatherGrid: GridLayout,
    val weatherLoc: TextView,
    val widgetBox: LinearLayout):
    ViewBinding
{
    override val root = content
    val gridB = arrayOf(gridB0, gridB1, gridB2, gridB3, gridB4, gridB5, gridB6)
    val gridC = arrayOf(gridC0, gridC1, gridC2, gridC3, gridC4, gridC5, gridC6)
    val gridE = arrayOf(gridE0, gridE1, gridE2, gridE3, gridE4, gridE5, gridE6)
    val gridS = arrayOf(gridS0, gridS1, gridS2, gridS3, gridS4, gridS5, gridS6)
    companion object {
        fun bind(rootView: View) = MainActivityBinding(
            rootView as ConstraintLayout,
            rootView.findViewById(R.id.clock_analog)!!,
            rootView.findViewById(R.id.clock_box)!!,
            rootView.findViewById(R.id.clock_digital)!!,
            rootView.findViewById(R.id.clock_grid)!!,
            rootView.findViewById(R.id.clock_word)!!,
            rootView.findViewById(R.id.datetime_top_margin)!!,
            rootView.findViewById(R.id.drawer_recycler)!!,
            rootView.findViewById(R.id.gridB0)!!,
            rootView.findViewById(R.id.gridB1)!!,
            rootView.findViewById(R.id.gridB2)!!,
            rootView.findViewById(R.id.gridB3)!!,
            rootView.findViewById(R.id.gridB4)!!,
            rootView.findViewById(R.id.gridB5)!!,
            rootView.findViewById(R.id.gridB6)!!,
            rootView.findViewById(R.id.gridBu)!!,
            rootView.findViewById(R.id.gridC0)!!,
            rootView.findViewById(R.id.gridC1)!!,
            rootView.findViewById(R.id.gridC2)!!,
            rootView.findViewById(R.id.gridC3)!!,
            rootView.findViewById(R.id.gridC4)!!,
            rootView.findViewById(R.id.gridC5)!!,
            rootView.findViewById(R.id.gridC6)!!,
            rootView.findViewById(R.id.gridCu)!!,
            rootView.findViewById(R.id.gridE0)!!,
            rootView.findViewById(R.id.gridE1)!!,
            rootView.findViewById(R.id.gridE2)!!,
            rootView.findViewById(R.id.gridE3)!!,
            rootView.findViewById(R.id.gridE4)!!,
            rootView.findViewById(R.id.gridE5)!!,
            rootView.findViewById(R.id.gridE6)!!,
            rootView.findViewById(R.id.gridEu)!!,
            rootView.findViewById(R.id.gridS0)!!,
            rootView.findViewById(R.id.gridS1)!!,
            rootView.findViewById(R.id.gridS2)!!,
            rootView.findViewById(R.id.gridS3)!!,
            rootView.findViewById(R.id.gridS4)!!,
            rootView.findViewById(R.id.gridS5)!!,
            rootView.findViewById(R.id.gridS6)!!,
            rootView.findViewById(R.id.home_recycler)!!,
            rootView.findViewById(R.id.main_date)!!,
            rootView.findViewById(R.id.main_head)!!,
            rootView.findViewById(R.id.main_search)!!,
            rootView.findViewById(R.id.main_search_opt)!!,
            rootView.findViewById(R.id.weather_box)!!,
            rootView.findViewById(R.id.weather_grid)!!,
            rootView.findViewById(R.id.weather_loc)!!,
            rootView.findViewById(R.id.widget_box)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): MainActivityBinding
        {
            val root = inflater.inflate(R.layout.main_activity, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class MainOptDialogBinding(
    rootView: LinearLayout,
    val backChoice: TextView,
    val colorChoice: TextView,
    val contactChoice: TextView,
    val dateChoice: TextView,
    val fontChoice: TextView,
    val language: TextView,
    val mainAbout: TextView,
    val mainInfo: TextView,
    val timeChoice: TextView,
    val weatherMenu: TextView):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = MainOptDialogBinding(
            rootView as LinearLayout,
            rootView.findViewById(R.id.back_choice)!!,
            rootView.findViewById(R.id.color_choice)!!,
            rootView.findViewById(R.id.contact_choice)!!,
            rootView.findViewById(R.id.date_choice)!!,
            rootView.findViewById(R.id.font_choice)!!,
            rootView.findViewById(R.id.language)!!,
            rootView.findViewById(R.id.main_about)!!,
            rootView.findViewById(R.id.main_info)!!,
            rootView.findViewById(R.id.time_choice)!!,
            rootView.findViewById(R.id.weather_menu)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): MainOptDialogBinding
        {
            val root = inflater.inflate(R.layout.main_opt_dialog, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class PinDialogBinding(
    rootView: FrameLayout,
    val pinBgrd: CheckBox,
    val pinDate: CheckBox,
    val pinDown: CheckBox,
    val pinHome: CheckBox,
    val pinLeft: CheckBox,
    val pinRight: CheckBox,
    val pinTime: CheckBox,
    val pinUp: CheckBox,
    val pinWeath: CheckBox):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = PinDialogBinding(
            rootView as FrameLayout,
            rootView.findViewById(R.id.pin_bgrd)!!,
            rootView.findViewById(R.id.pin_date)!!,
            rootView.findViewById(R.id.pin_down)!!,
            rootView.findViewById(R.id.pin_home)!!,
            rootView.findViewById(R.id.pin_left)!!,
            rootView.findViewById(R.id.pin_right)!!,
            rootView.findViewById(R.id.pin_time)!!,
            rootView.findViewById(R.id.pin_up)!!,
            rootView.findViewById(R.id.pin_weath)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): PinDialogBinding
        {
            val root = inflater.inflate(R.layout.pin_dialog, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class PopupActionBinding(
    rootView: FrameLayout,
    val itemName: TextView):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = PopupActionBinding(
            rootView as FrameLayout,
            rootView.findViewById(R.id.item_name)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): PopupActionBinding
        {
            val root = inflater.inflate(R.layout.popup_action, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class PopupItemBinding(
    rootView: FrameLayout,
    val itemName: TextView):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = PopupItemBinding(
            rootView as FrameLayout,
            rootView.findViewById(R.id.item_name)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): PopupItemBinding
        {
            val root = inflater.inflate(R.layout.popup_item, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class RadioButtonBinding(
    rootView: RadioButton):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = RadioButtonBinding(
            rootView as RadioButton)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): RadioButtonBinding
        {
            val root = inflater.inflate(R.layout.radio_button, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class RenameDialogBinding(
    rootView: FrameLayout,
    val editLabel: EditText,
    val editOrder: EditText):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = RenameDialogBinding(
            rootView as FrameLayout,
            rootView.findViewById(R.id.edit_label)!!,
            rootView.findViewById(R.id.edit_order)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): RenameDialogBinding
        {
            val root = inflater.inflate(R.layout.rename_dialog, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class SearchLocationBinding(
    rootView: ConstraintLayout,
    val list: RecyclerView,
    val statusMsg: TextView):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = SearchLocationBinding(
            rootView as ConstraintLayout,
            rootView.findViewById(R.id.list)!!,
            rootView.findViewById(R.id.status_msg)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): SearchLocationBinding
        {
            val root = inflater.inflate(R.layout.search_location, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class SearchOptDialogBinding(
    rootView: FrameLayout,
    val dialogList: LinearLayout):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = SearchOptDialogBinding(
            rootView as FrameLayout,
            rootView.findViewById(R.id.dialog_list)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): SearchOptDialogBinding
        {
            val root = inflater.inflate(R.layout.search_opt_dialog, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class SearchRenameDialogBinding(
    rootView: FrameLayout,
    val editDef: CheckBox,
    val editDel: RadioButton,
    val editKeep: RadioButton,
    val editName: EditText,
    val editNew: RadioButton,
    val editUrl: EditText):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = SearchRenameDialogBinding(
            rootView as FrameLayout,
            rootView.findViewById(R.id.edit_def)!!,
            rootView.findViewById(R.id.edit_del)!!,
            rootView.findViewById(R.id.edit_keep)!!,
            rootView.findViewById(R.id.edit_name)!!,
            rootView.findViewById(R.id.edit_new)!!,
            rootView.findViewById(R.id.edit_url)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): SearchRenameDialogBinding
        {
            val root = inflater.inflate(R.layout.search_rename_dialog, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class TableDialogBinding(
    rootView: FrameLayout,
    val table: TableLayout):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = TableDialogBinding(
            rootView as FrameLayout,
            rootView.findViewById(R.id.table)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): TableDialogBinding
        {
            val root = inflater.inflate(R.layout.table_dialog, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class TableRowBinding(
    rootView: TableRow,
    val text1: TextView,
    val text2: TextView):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = TableRowBinding(
            rootView as TableRow,
            rootView.findViewById(R.id.text1)!!,
            rootView.findViewById(R.id.text2)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): TableRowBinding
        {
            val root = inflater.inflate(R.layout.table_row, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

class TextDialogBinding(
    rootView: FrameLayout,
    val text: TextView):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = TextDialogBinding(
            rootView as FrameLayout,
            rootView.findViewById(R.id.text)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): TextDialogBinding
        {
            val root = inflater.inflate(R.layout.text_dialog, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}

interface ViewBinding {
	val root: View
}

class WeatherDialogBinding(
    rootView: LinearLayout,
    val currentLocation: RadioButton,
    val locationChoice: RadioGroup,
    val newLocation: EditText,
    val noWeather: RadioButton,
    val oMeteoLink: TextView,
    val symbols: TextView,
    val tempChoice: TextView,
    val ttypeChoice: TextView,
    val weatherType: TextView,
    val weekStart: TextView):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = WeatherDialogBinding(
            rootView as LinearLayout,
            rootView.findViewById(R.id.current_location)!!,
            rootView.findViewById(R.id.location_choice)!!,
            rootView.findViewById(R.id.new_location)!!,
            rootView.findViewById(R.id.no_weather)!!,
            rootView.findViewById(R.id.o_meteo_link)!!,
            rootView.findViewById(R.id.symbols)!!,
            rootView.findViewById(R.id.temp_choice)!!,
            rootView.findViewById(R.id.ttype_choice)!!,
            rootView.findViewById(R.id.weather_type)!!,
            rootView.findViewById(R.id.week_start)!!)

        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean): WeatherDialogBinding
        {
            val root = inflater.inflate(R.layout.weather_dialog, parent, false)
            if (attachToParent) parent?.addView(root)
            return bind(root)
        }

        @Suppress("unused")
        fun inflate(inflater: LayoutInflater) = inflate(inflater, null, false)
    }
}
