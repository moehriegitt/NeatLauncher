// Autogenerated by mkresource.pl, do not edit
@file:Suppress("MemberVisibilityCanBePrivate")

package de.theiling.neatlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class AboutDialogBinding(
    rootView: LinearLayout,
    val author: TextView,
    val authorBox: LinearLayout,
    val license: TextView,
    val licenseBox: LinearLayout,
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

class MainActivityBinding(
    val content: ConstraintLayout,
    val drawerRecycler: RecyclerView,
    val homeRecycler: RecyclerView,
    val mainClockAnalog: NeatAnalogClock,
    val mainClockBox: LinearLayout,
    val mainClockDigital: TextView,
    val mainClockGrid: NeatGridClock,
    val mainClockWord: NeatWordClock,
    val mainDate: TextView,
    val mainHead: LinearLayout,
    val mainSearch: EditText,
    val mainSearchOpt: TextView,
    val mainTopMargin: View):
    ViewBinding
{
    override val root = content
    companion object {
        fun bind(rootView: View) = MainActivityBinding(
            rootView as ConstraintLayout,
            rootView.findViewById(R.id.drawer_recycler)!!,
            rootView.findViewById(R.id.home_recycler)!!,
            rootView.findViewById(R.id.main_clock_analog)!!,
            rootView.findViewById(R.id.main_clock_box)!!,
            rootView.findViewById(R.id.main_clock_digital)!!,
            rootView.findViewById(R.id.main_clock_grid)!!,
            rootView.findViewById(R.id.main_clock_word)!!,
            rootView.findViewById(R.id.main_date)!!,
            rootView.findViewById(R.id.main_head)!!,
            rootView.findViewById(R.id.main_search)!!,
            rootView.findViewById(R.id.main_search_opt)!!,
            rootView.findViewById(R.id.main_top_margin)!!)

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
    val dateChoice: TextView,
    val fontChoice: TextView,
    val mainAbout: TextView,
    val mainInfo: TextView,
    val readContactList: CheckBox,
    val timeChoice: TextView):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = MainOptDialogBinding(
            rootView as LinearLayout,
            rootView.findViewById(R.id.back_choice)!!,
            rootView.findViewById(R.id.color_choice)!!,
            rootView.findViewById(R.id.date_choice)!!,
            rootView.findViewById(R.id.font_choice)!!,
            rootView.findViewById(R.id.main_about)!!,
            rootView.findViewById(R.id.main_info)!!,
            rootView.findViewById(R.id.read_contact_list)!!,
            rootView.findViewById(R.id.time_choice)!!)

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
    val pinDate: CheckBox,
    val pinDown: CheckBox,
    val pinHome: CheckBox,
    val pinLeft: CheckBox,
    val pinRight: CheckBox,
    val pinTime: CheckBox):
    ViewBinding
{
    override val root = rootView
    companion object {
        fun bind(rootView: View) = PinDialogBinding(
            rootView as FrameLayout,
            rootView.findViewById(R.id.pin_date)!!,
            rootView.findViewById(R.id.pin_down)!!,
            rootView.findViewById(R.id.pin_home)!!,
            rootView.findViewById(R.id.pin_left)!!,
            rootView.findViewById(R.id.pin_right)!!,
            rootView.findViewById(R.id.pin_time)!!)

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

interface ViewBinding {
	val root: View
}
