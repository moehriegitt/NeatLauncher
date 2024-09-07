package de.theiling.neatlauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.Display
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity:
    AppCompatActivity()
{
    enum class Mode {
        INIT,
        HOME1,
        DRAWER1,
        DRAWER2
    }

    private val items = mutableSetOf<Item>()
    private val homeItems = mutableSetOf<Item>()
    private var leftItem : Item? = null
    private var rightItem : Item? = null
    private var downItem : Item? = null
    private var timeItem : Item? = null
    private var dateItem : Item? = null

    private val clicky = object: ItemAdapter.ClickListener {
        override fun onClick(view: View, item: Item) {
            if (allowClick) {
                itemLaunch(item)
            }
        }
        override fun onLongClick(view: View, item: Item) = itemDialog(view, item)
    }

    private val homeAdapter = ItemAdapter(homeItems, R.layout.home_item, { true }, clicky)
    private val drawerAdapter = ItemAdapter(items, R.layout.drawer_item, { !it.hidden }, clicky)

    private var searchStr: String = ""
    private var dateChoice = 0
    private var timeChoice = 0
    private var colorChoice = 0
    private var fontChoice = 0

    // Having both Fling events on the main view and Click events on an item view
    // seems to sometimes generate both a fling and a click event from one gesture.
    // The click always comes second, and despite onFling returning true.  To fix this,
    // we use another guard 'allowClick' to prevent the click if a Fling was seen.
    // It is set to true on Down and to false on confirmed Fling, preventing Click from
    // the same Down.
    private var allowClick = false

    private val minuteTick = object: BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            onMinuteTick()
        }
    }

    private val gestures = GestureDetector(
        baseContext,
        object: GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            allowClick = true
            return false
        }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (e1 != null) {
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                if (abs(dx) > abs(dy)) {
                    if (abs(vx) > 100) {
                        allowClick = false
                        if (dx > 0) { onFlingRight() } else { onFlingLeft() }
                        return true
                    }
                } else {
                    if (abs(vy) > 100) {
                        allowClick = false
                        if (dy > 0) { onFlingDown() } else { onFlingUp() }
                        return true
                    }
                }
            }
            return false
        }
    })

    // for getting added/removed packages, we need the sequence counter:
    private var bootSeq = 0

    private lateinit var mainHead: LinearLayout
    private lateinit var homeRecycler: RecyclerView
    private lateinit var drawerRecycler: RecyclerView
    private lateinit var mainTopMargin: View
    private lateinit var mainClockBox: LinearLayout
    private lateinit var mainClockDigital: TextView
    private lateinit var mainClockAnalog: NeatAnalogClock
    private lateinit var mainClockWord: NeatWordClock
    private lateinit var mainClockGrid: NeatGridClock
    private lateinit var mainDate: TextView
    private lateinit var mainSearch: EditText
    private lateinit var mainSearchOpt: TextView
    private lateinit var searchEngine: SearchEngine

    // override on....()
    override fun onCreate(
        savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        searchEngine = SearchEngine(c)
        dateChoice = getDateChoice(c)
        timeChoice = getTimeChoice(c)
        colorChoice = getColorChoice(c)
        fontChoice = getFontChoice(c)

        setTheme(selectTheme(fontChoice, colorChoice))

        enableEdgeToEdge()
        setContentView(R.layout.main_activity)

        mainHead = findViewById(R.id.main_head)!!
        homeRecycler = findViewById(R.id.home_recycler)!!
        drawerRecycler = findViewById(R.id.drawer_recycler)!!
        mainTopMargin = findViewById(R.id.main_top_margin)!!
        mainClockBox = findViewById(R.id.main_clock_box)!!
        mainClockDigital = findViewById(R.id.main_clock_digital)!!
        mainClockAnalog = findViewById(R.id.main_clock_analog)!!
        mainClockWord = findViewById(R.id.main_clock_word)!!
        mainClockGrid = findViewById(R.id.main_clock_grid)!!
        mainDate = findViewById(R.id.main_date)!!
        mainSearch = findViewById(R.id.main_search)!!
        mainSearchOpt = findViewById(R.id.main_search_opt)!!

        window.navigationBarColor = getColor(R.color.mainBackground)

        homeRecycler.setAdapter(homeAdapter)
        drawerRecycler.setAdapter(drawerAdapter)

        mainSearch.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(c: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(e: Editable) {}
            override fun onTextChanged(c: CharSequence, i: Int, i1: Int, i2: Int) {
                searchStr = c.toString()
                drawerNotifyChange()
            }
        })
        mainSearch.setOnEditorActionListener { _, aid, _ ->
            val done = (aid == EditorInfo.IME_ACTION_DONE)
            if (done) {
                searchWith(searchEngine.default)
            }
            done
        }
        mainSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                resetView(false)
            }
            setMode(modeSecondary())
        }

        mainSearchOpt.setOnClickListener {
            when (mainSearch.hasFocus() || mainSearch.text.isNotEmpty()) {
                true -> searchOptDialog(getViewGroup())
                else -> mainOptDialog(getViewGroup())
            }
        }

        if (Build.VERSION.SDK_INT >= 26) {
            bootSeq = packageManager.getChangedPackages(0)?.sequenceNumber ?: 0
        }

        mainClockBox.setOnClickListener {
            // SHOW_ALARMS requires special permissions, so we'll just start the item instead.
            timeItem?.let { itemLaunch(it) } ?:
                startActivity(packageIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS)))
        }
        mainClockBox.setOnLongClickListener {
            timeDialog(getViewGroup())
            true
        }

        mainDate.setOnClickListener {
            dateItem?.let { itemLaunch(it) } ?: startCalendar()
        }
        mainDate.setOnLongClickListener {
            dateDialog(getViewGroup())
            true
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(minuteTick, IntentFilter(Intent.ACTION_TIME_TICK))
        onMinuteTick()
        searchEngine.loadPref()
        learnItems()
        itemsNotifyChange()
    }

    override fun onStop() {
        unregisterReceiver(minuteTick)
        resetView(false)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        itemsNotifyChange()
        onMinuteTick()
    }

    // This makes the 'Home' button reset the view.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        resetView(true)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        resetView(!mainSearch.hasFocus())
    }

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        gestures.onTouchEvent(e)
        return super.dispatchTouchEvent(e)
    }

    private fun onFlingRight() {
        rightItem?.let { itemLaunch(it) } ?: startPhone()
    }

    private fun onFlingLeft() {
        leftItem?.let { itemLaunch(it) } ?:
            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
    }

    private fun onFlingUp() {
        if (getMode() == modePrimary()) {
            clearSearch()
            setMode(modeSecondary())
        }
    }

    private fun onFlingDown() {
        when (getMode()) {
            modePrimary() -> {
                downItem?.let { itemLaunch(it) } ?:
                    startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            modeSecondary() -> if (!drawerRecycler.canScrollVertically(-1)) {
                resetView(false)
            }
            else -> Unit
        }
    }

    private fun onMinuteTick() {
        val now = Date()

        val fmt = formatTime(now)
        mainClockDigital.text = fmt
        mainClockDigital.visibility = if (fmt.isEmpty()) View.GONE else View.VISIBLE

        val dat = formatDate(now)
        mainDate.text = dat
        mainDate.visibility = if (dat.isEmpty()) View.GONE else View.VISIBLE

        val anlg = timeChoice == time_anlg
        mainClockAnalog.visibility = if (!anlg) View.GONE else View.VISIBLE
        if (anlg) {
            mainClockAnalog.updateTime()
        }

        val word = timeChoice == time_word
        mainClockWord.visibility = if (!word) View.GONE else View.VISIBLE
        if (word) {
            mainClockWord.updateTime()
        }

        val grid = timeChoice == time_grid
        mainClockGrid.visibility = if (!grid) View.GONE else View.VISIBLE
        if (grid) {
            mainClockGrid.updateTime()
        }

        mainTopMargin.visibility = when {
            fmt.isNotEmpty() -> View.VISIBLE
            dat.isNotEmpty() -> View.VISIBLE
            word -> View.VISIBLE
            grid -> View.GONE
            anlg -> View.GONE
            else -> View.GONE
        }
    }

    // auxiliary oneliners
    private val c get() = applicationContext

    private fun shortToast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun getViewGroup(): ViewGroup = findViewById(android.R.id.content)
    private fun getUserManager() = c.getSystemService(USER_SERVICE) as UserManager
    private fun getDisplayManager() = c.getSystemService(DISPLAY_SERVICE) as DisplayManager
    private fun getDefaultDisplay() = getDisplayManager().getDisplay(Display.DEFAULT_DISPLAY)
    private fun getLauncher() = c.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private fun getInputMethodManager() =
        c.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

    private fun getUserHandle(uid: Long): UserHandle =
        getUserManager().getUserForSerialNumber(uid)

    private fun packageIntent(s: String) = packageManager.getLaunchIntentForPackage(s)
    private fun clearSearch() = mainSearch.text.clear()
    private fun modePrimary() = if (homeItems.size > 0) Mode.HOME1 else Mode.DRAWER1
    private fun modeSecondary() = Mode.DRAWER2
    private fun drawerNotifyChange() = drawerAdapter.getFilter().filter(searchStr)
    private fun visibleIf(b: Boolean) = if (b) View.VISIBLE else View.GONE
    private fun <T>theSingle(v: List<T>) = if (v.isEmpty()) null else v[0]

    // functionality
    private fun homeNotifyChange() {
        val pinitems = items.filter { (it.pinned != 0) }
        leftItem  = theSingle(pinitems.filter { (it.pinned and ITEM_PIN_LEFT) != 0 })
        rightItem = theSingle(pinitems.filter { (it.pinned and ITEM_PIN_RIGHT) != 0 })
        downItem  = theSingle(pinitems.filter { (it.pinned and ITEM_PIN_DOWN) != 0 })
        timeItem  = theSingle(pinitems.filter { (it.pinned and ITEM_PIN_TIME) != 0 })
        dateItem  = theSingle(pinitems.filter { (it.pinned and ITEM_PIN_DATE) != 0 })
        homeItems.clear()
        pinitems.filterTo(homeItems) { (it.pinned and ITEM_PIN_HOME) != 0 }
        homeAdapter.getFilter().filter("")
        resetMode()
    }

    private fun learnItems() {
        val c: Context = this
        val um = getUserManager()
        val la = getLauncher()
        val myUha = Process.myUserHandle()
        val myUid = um.getSerialNumberForUser(myUha)
        items.clear()

        // Standard Actions
        items.add(Item(c, ITEM_TYPE_INT, null, getString(R.string.item_nothing),
            "", "", myUid))
        items.add(Item(c, ITEM_TYPE_INT, null, getString(R.string.item_camera_photo),
            "", MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA, myUid))
        items.add(Item(c, ITEM_TYPE_INT, null, getString(R.string.item_camera_video),
            "", MediaStore.INTENT_ACTION_VIDEO_CAMERA, myUid))
        items.add(Item(c, ITEM_TYPE_INT, null, getString(R.string.item_settings_home),
            "", Settings.ACTION_HOME_SETTINGS, myUid))
        items.add(Item(c, ITEM_TYPE_INT, null, getString(R.string.item_phone_dial),
            "", Intent.ACTION_DIAL, myUid))

        // items
        for (profile in um.userProfiles) {
            val uid = um.getSerialNumberForUser(profile)
            for (acti in la.getActivityList(null, profile)) {
                val api = acti.applicationInfo
                val pack = api.packageName
                val app = Item(c, ITEM_TYPE_APP, null, acti.label.toString(),
                    pack, acti.name, uid)
                items.add(app)

                if (Build.VERSION.SDK_INT >= 25) {
                    // Shortcuts
                    val q = LauncherApps.ShortcutQuery().setPackage(pack).setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                    try {
                        la.getShortcuts(q, myUha)?.map { s ->
                            val sht = Item(c,
                                (if (s.isPinned) ITEM_TYPE_PIN else ITEM_TYPE_SHORT),
                                app, s.shortLabel.toString(),
                                s.`package`, s.id, uid)
                            app.shortcuts.add(sht)
                            items.add(sht)
                        }
                    } catch (e: SecurityException) {
                        /* nothing */
                    }
                }
            }
        }

        // Contact list:
        if (getReadContacts(c)) {
            try {
                contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME),
                    null, null, null)?.use { cu ->
                    while (cu.moveToNext()) {
                        val uri = cu.getString(0)
                        val name = cu.getString(1)
                        items.add(Item(c, ITEM_TYPE_CONTACT, null, name, "", uri, 0))
                    }
                }
            } catch (e: SecurityException) {
                shortToast(getString(R.string.read_contacts_no_perm))
                setReadContacts(c, false)
            }
        }
    }

    private fun refreshitems() {
        if (Build.VERSION.SDK_INT >= 26) {
            val cp = packageManager.getChangedPackages(bootSeq) ?: return
            bootSeq = cp.sequenceNumber
            if (cp.packageNames.size > 0) {
                learnItems()
            }
        }
    }

    private fun itemsNotifyChange() {
        refreshitems()
        homeNotifyChange()
        drawerNotifyChange()
        resetMode()
    }

    private fun removeShortcut(sht: Item) = try {
        // Unfortunately, there is no 'unpinShortcut', only 'pinShortcuts' to set all
        // of them.  We need to set all of them except `sht`.
        if (Build.VERSION.SDK_INT >= 25) {
            val myUha = Process.myUserHandle()
            val la = getLauncher()
            val q = LauncherApps.ShortcutQuery().setPackage(sht.pack).setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            la.pinShortcuts(sht.pack,
                la.getShortcuts(q, myUha)!!.map { it.id }.filter { it != sht.act },
                myUha)
        }
        true
    } catch (e: SecurityException) {
        false
    }

    private fun resetView(clear: Boolean) {
        if (clear || (getDefaultDisplay().state != Display.STATE_ON)) {
            clearSearch()
        }
        mainSearch.clearFocus()
        getInputMethodManager().hideSoftInputFromWindow(getViewGroup().windowToken, 0)
        drawerRecycler.scrollToPosition(0)
        setMode(modePrimary())
    }

    private fun getMode() =
        when {
            mainHead.visibility == View.GONE -> Mode.DRAWER2
            homeRecycler.visibility == View.GONE -> Mode.DRAWER1
            drawerRecycler.visibility == View.GONE -> Mode.HOME1
            else -> Mode.INIT
        }

    private fun setMode(want: Mode) {
        if (want == getMode()) {
            return
        }
        mainHead.visibility = visibleIf(want != Mode.DRAWER2)
        homeRecycler.visibility = visibleIf(want == Mode.HOME1)
        drawerRecycler.visibility = visibleIf(want != Mode.HOME1)
    }

    private fun resetMode() = setMode (
        when (getMode()) {
            Mode.DRAWER2 -> modeSecondary()
            else -> modePrimary()
        })

    private fun formatDate(now: Date) =
        when (dateChoice) {
            date_syst -> DateFormat.getDateFormat(c).format(now)
            date_yyyy -> simpleFormatDate(now, "yyyy-MM-dd")
            date_EEEy -> simpleFormatDate(now, "EEE yyyy-MM-dd")
            date_EEEE -> simpleFormatDate(now, "EEEE")
            else -> ""
        }

    private fun formatTime(now: Date) =
        when (timeChoice) {
            time_syst -> DateFormat.getTimeFormat(c).format(now)
            time_Hmmx -> simpleFormatDate(now, "H:mm")
            time_HHmm -> simpleFormatDate(now, "HH:mm")
            time_hmma -> simpleFormatDate(now, "h:mm aa")
            time_hhma -> simpleFormatDate(now, "hh:mm aa")
            else -> ""
        }

    private fun simpleFormatDate(now: Date, s: String) =
        SimpleDateFormat(s, Locale.getDefault()).format(now)

    private fun packageIntent(i: Intent) =
        try {
            val p = packageManager
            p.getLaunchIntentForPackage(i.resolveActivity(p).packageName)!!
        } catch (e: Exception) {
            i
        }

    // Instead of directly starting ACTION_DIAL, we will find the package
    // and start its main activity, because ACTION_DIAL opens the number pad,
    // but we usually do not want that, we want to search the contacts.
    private fun startPhone() =
        startActivity(packageIntent(Intent(Intent.ACTION_DIAL)))

    private fun startCalendar()
    {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
        startActivity(intent)
    }

    private fun openContact(con: Item)
    {
        val uri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_URI,
            con.act)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.resolveActivity(packageManager)
        startActivity(intent)
    }

    private fun itemLaunch(item: Item)
    {
        resetView(true)
        try {
            when(item.type) {
                ITEM_TYPE_APP -> packageIntent(item.pack)?.let { startActivity(it) }
                ITEM_TYPE_INT -> if (item.act != "") startActivity(Intent(item.act))
                ITEM_TYPE_SHORT,
                ITEM_TYPE_PIN -> {
                    val la = getLauncher()
                    val uha = getUserHandle(item.uid)
                    if (Build.VERSION.SDK_INT >= 25) {
                        la.startShortcut(item.pack, item.act, null, null, uha)
                    }
                }
                ITEM_TYPE_CONTACT -> openContact(item)
            }
        } catch (e: Exception) {
            shortToast(getString(R.string.error_starting).replace("%s", item.label))
        }
    }

    private fun searchWith(e: SearchUrl) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(e.url.
            replace("%s", searchStr).
            replace("%L", searchStr.lowercase(Locale.getDefault())))))
    }

    // dialogs
    private fun itemSetupButton(
        view: View, list: LinearLayout, d: AlertDialog, prefix: String, item: Item)
    {
        val v = LayoutInflater.from(view.context).inflate(R.layout.popup_item, list, false)
        list.addView(v)
        val t = v.findViewById<TextView>(R.id.item_name)
        t.text = item.label.removePrefix(prefix)
        t.setOnClickListener {
            d.dismiss()
            itemLaunch(item)
        }
        t.setOnLongClickListener {
            d.dismiss()
            itemDialog(view, item)
            true
        }
    }

    private fun dialogInit(view: View, id: Int):
        Pair<View, AlertDialog.Builder>
    {
        val c = view.context
        val b = AlertDialog.Builder(c)
        val v = LayoutInflater.from(c).inflate(id, null)
        b.setView(v)
        b.setNegativeButton(getString(R.string.button_cancel)) { _, _ -> }
        return Pair(v, b)
    }

    private fun itemInfoLaunch(pack: String)
    {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.data = Uri.parse("package:$pack")
        startActivity(intent)
    }

    private fun itemDialog(view: View, item: Item)
    {
        val (v, b) = dialogInit(view, R.layout.item_actions)
        v.findViewById<TextView>(R.id.dialog_title).text = item.label

        val d = b.create()

        // Info
        val itemInfo = v.findViewById<TextView>(R.id.item_info)!!
        if (item.pack == "") itemInfo.visibility = View.GONE
        itemInfo.setOnClickListener {
            d.dismiss()
            itemInfoLaunch(item.pack)
        }

        // rename
        val itemRename = v.findViewById<TextView>(R.id.item_rename)!!
        itemRename.setOnClickListener {
            d.dismiss()
            renameDialog(view, item)
        }

        // hide/show
        val itemHide = v.findViewById<TextView>(R.id.item_hide)!!
        val itemShow = v.findViewById<TextView>(R.id.item_show)!!
        (if (item.hidden) itemHide else itemShow).visibility = View.GONE
        itemHide.setOnClickListener {
            d.dismiss()
            shortToast(getString(R.string.do_hide, item.label))
            item.hidden = true
            if (searchStr == "") {
                drawerNotifyChange()
            }
        }
        itemShow.setOnClickListener {
            d.dismiss()
            shortToast(getString(R.string.do_show, item.label))
            item.hidden = false
            if (searchStr == "") {
                drawerNotifyChange()
            }
        }

        // remove
        val itemRemove = v.findViewById<TextView>(R.id.item_remove)!!
        if (item.type != ITEM_TYPE_PIN) {
            itemRemove.visibility = View.GONE
        }
        itemRemove.setOnClickListener {
            d.dismiss()
            val ok = removeShortcut(item)
            shortToast(
                getString(
                    (if (ok) R.string.ok_remove_shortcut else R.string.error_removing),
                    item.label))
            learnItems()
            itemsNotifyChange()
        }

        // pin/rid
        val itemPin = v.findViewById<TextView>(R.id.item_pin)!!
        val itemRid = v.findViewById<TextView>(R.id.item_rid)!!
        (if (item.pinned != 0) itemPin else itemRid).visibility = View.GONE
        itemPin.setOnClickListener {
            d.dismiss()
            pinDialog(view, item)
        }
        itemRid.setOnClickListener {
            d.dismiss()
            item.pinned = 0
            homeNotifyChange()
            shortToast(getString(R.string.do_unpin, item.label))
        }

        val prefix = item.label + ": "

        // Shortcuts
        val shortcutList = v.findViewById<LinearLayout>(R.id.shortcut_list)!!
        item.shortcuts.sortWith { x, y -> x.displayCompareToPrep(y) { it.removePrefix(prefix) } }
        for (sub in item.shortcuts) {
            itemSetupButton(view, shortcutList, d, prefix, sub)
        }
        if (item.shortcuts.size == 0) {
            v.findViewById<LinearLayout>(R.id.item_shortcuts).visibility = View.GONE
        }

        d.show()
    }

    private fun setOnDoneClickOk(edit: EditText, d: AlertDialog)
    {
        edit.setOnEditorActionListener { _, aid, _ ->
            val done = (aid == EditorInfo.IME_ACTION_DONE)
            if (done) {
                d.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            }
            done
        }
    }

    private fun renameDialog(view: View, item: Item)
    {
        val (v, b) = dialogInit(view, R.layout.rename_dialog)

        val editLabel = v.findViewById<EditText>(R.id.edit_label)!!
        editLabel.setText(item.label)
        editLabel.hint = item.origLabel

        val editOrder = v.findViewById<EditText>(R.id.edit_order)!!
        editOrder.setText(item.pref.order)

        b.setPositiveButton(getString(R.string.button_ok)) { _, _ ->
            item.label = editLabel.text.toString()
            item.order = editOrder.text.toString()
            itemsNotifyChange()
        }

        val d = b.create()
        setOnDoneClickOk(editLabel, d)
        setOnDoneClickOk(editOrder, d)
        d.show()
    }

    private fun searchRenameDialog(view: View, e: SearchUrl)
    {
        val (v, b) = dialogInit(view, R.layout.search_rename_dialog)

        val editName = v.findViewById<EditText>(R.id.edit_name)!!
        editName.setText(e.name)

        val editUrl = v.findViewById<EditText>(R.id.edit_url)!!
        editUrl.setText(e.url)

        val editDef = v.findViewById<CheckBox>(R.id.edit_default)!!
        editDef.isChecked = e.isDefault
        if (e.isDefault) editDef.visibility = View.GONE

        val editNew = v.findViewById<RadioButton>(R.id.edit_copy)!!
        val editDel = v.findViewById<RadioButton>(R.id.edit_delete)!!

        b.setPositiveButton(getString(R.string.button_ok)) { _, _ ->
            when {
                editNew.isChecked -> {
                    searchEngine.add(
                        editName.text.toString(),
                        editUrl.text.toString(),
                        editDef.isChecked)
                }
                editDel.isChecked -> {
                    searchEngine.delete(e)
                }
                else -> {
                    e.name = editName.text.toString()
                    e.url = editUrl.text.toString()
                    e.isDefault = editDef.isChecked
                }
            }
            searchEngine.savePref()
        }

        val d = b.create()
        setOnDoneClickOk(editName, d)
        setOnDoneClickOk(editUrl, d)
        d.show()
    }

    private fun pinUnset(item: Item?, bit: Int): Int {
        item?.let { it.pinned = it.pinned and bit.inv() }
        return bit
    }

    private fun pinDialog(view: View, item: Item)
    {
        val (v, b) = dialogInit(view, R.layout.pin_dialog)
        v.findViewById<TextView>(R.id.dialog_title).text = item.label

        val pinHome = v.findViewById<CheckBox>(R.id.pin_home)!!
        val pinLeft = v.findViewById<CheckBox>(R.id.pin_left)!!
        val pinRight = v.findViewById<CheckBox>(R.id.pin_right)!!
        val pinDown = v.findViewById<CheckBox>(R.id.pin_down)!!
        val pinTime = v.findViewById<CheckBox>(R.id.pin_time)!!
        val pinDate = v.findViewById<CheckBox>(R.id.pin_date)!!

        pinHome.isChecked = (item.pinned and ITEM_PIN_HOME) == 0
        pinLeft.isChecked = (item.pinned and ITEM_PIN_LEFT) != 0
        pinRight.isChecked = (item.pinned and ITEM_PIN_RIGHT) != 0
        pinDown.isChecked = (item.pinned and ITEM_PIN_DOWN) != 0
        pinTime.isChecked = (item.pinned and ITEM_PIN_TIME) != 0
        pinDate.isChecked = (item.pinned and ITEM_PIN_DATE) != 0

        b.setPositiveButton(getString(R.string.button_ok)) { _, _ ->
            item.pinned =
                (if (pinHome.isChecked)  ITEM_PIN_HOME else 0) +
                (if (pinLeft.isChecked)  pinUnset(leftItem,  ITEM_PIN_LEFT)  else 0) +
                (if (pinRight.isChecked) pinUnset(rightItem, ITEM_PIN_RIGHT) else 0) +
                (if (pinDown.isChecked)  pinUnset(downItem,  ITEM_PIN_DOWN)  else 0) +
                (if (pinTime.isChecked)  pinUnset(timeItem,  ITEM_PIN_TIME)  else 0) +
                (if (pinDate.isChecked)  pinUnset(dateItem,  ITEM_PIN_DATE)  else 0)
            homeNotifyChange()
        }

        b.create().show()
    }

    private fun searchSetupButton(
        view: View, list: LinearLayout, d: AlertDialog, e: SearchUrl)
    {
        val v = LayoutInflater.from(view.context).inflate(R.layout.popup_item, list, false)
        list.addView(v)
        val t = v.findViewById<TextView>(R.id.item_name)
        t.text = e.name
        t.setOnClickListener {
            d.dismiss()
            searchWith(e)
        }
        t.setOnLongClickListener {
            d.dismiss()
            searchRenameDialog(view, e)
            true
        }
    }

    private fun searchOptDialog(view: View)
    {
        val (v, b) = dialogInit(view, R.layout.search_opt_dialog)
        val d = b.create()
        val list = v.findViewById<LinearLayout>(R.id.dialog_list)!!
        val them = searchEngine.them
        them.sortWith { x, y -> x.name.compareTo(y.name) }
        for (e in them) {
            searchSetupButton(view, list, d, e)
        }
        d.show()
    }

    private fun mainOptDialog(view: View)
    {
        val (v, b) = dialogInit(view, R.layout.main_opt_dialog)

        val rcl = v.findViewById<CheckBox>(R.id.read_contact_list)!!
        rcl.isChecked = getReadContacts(c)
        rcl.setOnCheckedChangeListener { _, checked ->
            setReadContacts(c, checked)
            learnItems()
            if (checked && !getReadContacts(c)) {
                rcl.isChecked = false
                itemInfoLaunch(c.packageName)
            }
        }

        b.setPositiveButton(getString(R.string.button_ok)) { _, _ -> }

        val d = b.create()

        v.findViewById<TextView>(R.id.color_choice)!!.setOnClickListener {
            d.dismiss()
            colorDialog(view)
        }
        v.findViewById<TextView>(R.id.font_choice)!!.setOnClickListener {
            d.dismiss()
            fontDialog(view)
        }
        v.findViewById<TextView>(R.id.time_choice)!!.setOnClickListener {
            d.dismiss()
            timeDialog(view)
        }
        v.findViewById<TextView>(R.id.date_choice)!!.setOnClickListener {
            d.dismiss()
            dateDialog(view)
        }

        d.show()
    }

    private fun dateDialog(view: View)
    {
        val b = AlertDialog.Builder(view.context)
        b.setSingleChoiceItems(R.array.date_choice, dateChoice) { d, which ->
            d.dismiss()
            dateChoice = which
            setDateChoice(c, dateChoice)
            onMinuteTick()
        }
        b.create().show()
    }

    private fun timeDialog(view: View)
    {
        val b = AlertDialog.Builder(view.context)
        b.setSingleChoiceItems(R.array.time_choice, timeChoice) { d, which ->
            d.dismiss()
            timeChoice = which
            setTimeChoice(c, timeChoice)
            onMinuteTick()
        }
        b.create().show()
    }

    private fun colorDialog(view: View)
    {
        val b = AlertDialog.Builder(view.context)
        b.setSingleChoiceItems(R.array.color_choice, colorChoice) { d, which ->
            d.dismiss()
            colorChoice = which
            setColorChoice(c, colorChoice)
            recreate()
        }
        b.create().show()
    }

    private fun fontDialog(view: View)
    {
        val b = AlertDialog.Builder(view.context)
        b.setSingleChoiceItems(R.array.font_choice, fontChoice) { d, which ->
            d.dismiss()
            fontChoice = which
            setFontChoice(c, fontChoice)
            recreate()
        }
        b.create().show()
    }
}
