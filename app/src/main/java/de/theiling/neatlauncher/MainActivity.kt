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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var z: MainActivityBinding
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
        z = MainActivityBinding.inflate(layoutInflater)
        setContentView(z.root)

        window.navigationBarColor = getColor(R.color.mainBackground)

        z.homeRecycler.setAdapter(homeAdapter)
        z.drawerRecycler.setAdapter(drawerAdapter)

        z.mainSearch.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(c: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(e: Editable) {}
            override fun onTextChanged(c: CharSequence, i: Int, i1: Int, i2: Int) {
                searchStr = c.toString()
                drawerNotifyChange()
            }
        })
        z.mainSearch.setOnEditorActionListener { _, aid, _ ->
            val done = (aid == EditorInfo.IME_ACTION_DONE)
            if (done) {
                searchWith(searchEngine.default)
            }
            done
        }
        z.mainSearch.setOnFocusChangeListener { _, hasFocus ->
            when (hasFocus) {
                true -> setMode(modeSecondary())
                else -> resetView(false)
            }
        }

        z.mainSearchOpt.setOnClickListener {
            when (z.mainSearch.hasFocus() || z.mainSearch.text.isNotEmpty()) {
                true -> searchOptDialog(getViewGroup())
                else -> mainOptDialog(getViewGroup())
            }
        }

        if (Build.VERSION.SDK_INT >= 26) {
            bootSeq = packageManager.getChangedPackages(0)?.sequenceNumber ?: 0
        }

        z.mainClockBox.setOnClickListener {
            // SHOW_ALARMS requires special permissions, so we'll just start the item instead.
            timeItem?.let { itemLaunch(it) } ?:
                startActivity(packageIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS)))
        }
        z.mainClockBox.setOnLongClickListener {
            timeDialog(getViewGroup())
            true
        }

        z.mainDate.setOnClickListener {
            dateItem?.let { itemLaunch(it) } ?: startCalendar()
        }
        z.mainDate.setOnLongClickListener {
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
        resetView(!z.mainSearch.hasFocus())
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
            modeSecondary() -> if (!z.drawerRecycler.canScrollVertically(-1)) {
                resetView(false)
            }
            else -> Unit
        }
    }

    private fun onMinuteTick() {
        val now = Date()

        val fmt = formatTime(now)
        z.mainClockDigital.text = fmt
        z.mainClockDigital.visibility = visibleIf(fmt.isNotEmpty())

        val dat = formatDate(now)
        z.mainDate.text = dat
        z.mainDate.visibility = visibleIf(dat.isNotEmpty())

        val anlg = timeChoice == time_anlg
        z.mainClockAnalog.visibility = visibleIf(anlg)
        if (anlg) {
            z.mainClockAnalog.updateTime()
        }

        val word = timeChoice == time_word
        z.mainClockWord.visibility = visibleIf(word)
        if (word) {
            z.mainClockWord.updateTime()
        }

        val grid = timeChoice == time_grid
        z.mainClockGrid.visibility = visibleIf(grid)
        if (grid) {
            z.mainClockGrid.updateTime()
        }

        z.mainTopMargin.visibility = when {
            anlg -> View.GONE
            word -> View.VISIBLE
            grid -> View.GONE
            fmt.isNotEmpty() -> View.VISIBLE
            dat.isNotEmpty() -> View.VISIBLE
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
    private fun clearSearch() = z.mainSearch.text.clear()
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
        z.mainSearch.clearFocus()
        getInputMethodManager().hideSoftInputFromWindow(getViewGroup().windowToken, 0)
        z.drawerRecycler.scrollToPosition(0)
        setMode(modePrimary())
    }

    private fun getMode() =
        when {
            z.mainHead.visibility == View.GONE -> Mode.DRAWER2
            z.homeRecycler.visibility == View.GONE -> Mode.DRAWER1
            z.drawerRecycler.visibility == View.GONE -> Mode.HOME1
            else -> Mode.INIT
        }

    private fun setMode(want: Mode) {
        if (want == getMode()) {
            return
        }
        z.mainHead.visibility = visibleIf(want != Mode.DRAWER2)
        z.homeRecycler.visibility = visibleIf(want == Mode.HOME1)
        z.drawerRecycler.visibility = visibleIf(want != Mode.HOME1)
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

    private fun itemInfoLaunch(pack: String)
    {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.data = Uri.parse("package:$pack")
        startActivity(intent)
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

    private fun dialogInit(view: View, content: View): AlertDialog.Builder {
        val b = AlertDialog.Builder(view.context)
        b.setView(content)
        b.setNegativeButton(getString(R.string.button_cancel)) { _, _ -> }
        return b
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

    private fun itemDialog(view: View, item: Item)
    {
        val z = ItemActionsBinding.inflate(LayoutInflater.from(view.context))
        z.dialogTitle.text = item.label
        val d = dialogInit(view, z.root).create()

        // Info
        z.itemInfo.visibility = visibleIf(item.pack != "")
        z.itemInfo.setOnClickListener {
            d.dismiss()
            itemInfoLaunch(item.pack)
        }

        // rename
        z.itemRename.setOnClickListener {
            d.dismiss()
            renameDialog(view, item)
        }

        // hide/show
        (if (item.hidden) z.itemHide else z.itemShow).visibility = View.GONE
        z.itemHide.setOnClickListener {
            d.dismiss()
            shortToast(getString(R.string.do_hide, item.label))
            item.hidden = true
            if (searchStr == "") drawerNotifyChange()
        }
        z.itemShow.setOnClickListener {
            d.dismiss()
            shortToast(getString(R.string.do_show, item.label))
            item.hidden = false
            if (searchStr == "") drawerNotifyChange()
        }

        // remove
        z.itemRemove.visibility = visibleIf(item.type == ITEM_TYPE_PIN)
        z.itemRemove.setOnClickListener {
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
        (if (item.pinned != 0) z.itemPin else z.itemRid).visibility = View.GONE
        z.itemPin.setOnClickListener {
            d.dismiss()
            pinDialog(view, item)
        }
        z.itemRid.setOnClickListener {
            d.dismiss()
            item.pinned = 0
            homeNotifyChange()
            shortToast(getString(R.string.do_unpin, item.label))
        }

        // Shortcuts
        val prefix = item.label + ": "
        item.shortcuts.sortWith { x, y -> x.displayCompareToPrep(y) { it.removePrefix(prefix) } }
        for (sub in item.shortcuts) {
            itemSetupButton(view, z.shortcutList, d, prefix, sub)
        }
        z.itemShortcuts.visibility = visibleIf(item.shortcuts.size > 0)

        d.show()
    }

    private fun renameDialog(view: View, item: Item)
    {
        val z = RenameDialogBinding.inflate(LayoutInflater.from(view.context))
        val b = dialogInit(view, z.root)

        z.editLabel.setText(item.label)
        z.editLabel.hint = item.origLabel

        z.editOrder.setText(item.pref.order)

        b.setPositiveButton(getString(R.string.button_ok)) { _, _ ->
            item.label = z.editLabel.text.toString()
            item.order = z.editOrder.text.toString()
            itemsNotifyChange()
        }

        val d = b.create()
        setOnDoneClickOk(z.editLabel, d)
        setOnDoneClickOk(z.editOrder, d)
        d.show()
    }

    private fun searchRenameDialog(view: View, e: SearchUrl)
    {
        val z = SearchRenameDialogBinding.inflate(LayoutInflater.from(view.context))
        val b = dialogInit(view, z.root)

        z.editName.setText(e.name)
        z.editUrl.setText(e.url)
        z.editDef.isChecked = e.isDefault
        z.editDef.visibility = visibleIf(!e.isDefault)

        b.setPositiveButton(getString(R.string.button_ok)) { _, _ ->
            when {
                z.editNew.isChecked -> {
                    searchEngine.add(
                        z.editName.text.toString(),
                        z.editUrl.text.toString(),
                        z.editDef.isChecked)
                }
                z.editDel.isChecked -> {
                    searchEngine.delete(e)
                }
                else -> {
                    e.name = z.editName.text.toString()
                    e.url = z.editUrl.text.toString()
                    e.isDefault = z.editDef.isChecked
                }
            }
            searchEngine.savePref()
        }

        val d = b.create()
        setOnDoneClickOk(z.editName, d)
        setOnDoneClickOk(z.editUrl, d)
        d.show()
    }

    private fun pinUnset(item: Item?, bit: Int): Int {
        item?.let { it.pinned = it.pinned and bit.inv() }
        return bit
    }

    private fun pinDialog(view: View, item: Item)
    {
        val z = PinDialogBinding.inflate(LayoutInflater.from(view.context))
        val b = dialogInit(view, z.root)
        z.dialogTitle.text = item.label

        z.pinHome.isChecked = (item.pinned and ITEM_PIN_HOME) == 0
        z.pinLeft.isChecked = (item.pinned and ITEM_PIN_LEFT) != 0
        z.pinRight.isChecked = (item.pinned and ITEM_PIN_RIGHT) != 0
        z.pinDown.isChecked = (item.pinned and ITEM_PIN_DOWN) != 0
        z.pinTime.isChecked = (item.pinned and ITEM_PIN_TIME) != 0
        z.pinDate.isChecked = (item.pinned and ITEM_PIN_DATE) != 0

        b.setPositiveButton(getString(R.string.button_ok)) { _, _ ->
            item.pinned =
                (if (z.pinHome.isChecked)  ITEM_PIN_HOME else 0) +
                (if (z.pinLeft.isChecked)  pinUnset(leftItem,  ITEM_PIN_LEFT)  else 0) +
                (if (z.pinRight.isChecked) pinUnset(rightItem, ITEM_PIN_RIGHT) else 0) +
                (if (z.pinDown.isChecked)  pinUnset(downItem,  ITEM_PIN_DOWN)  else 0) +
                (if (z.pinTime.isChecked)  pinUnset(timeItem,  ITEM_PIN_TIME)  else 0) +
                (if (z.pinDate.isChecked)  pinUnset(dateItem,  ITEM_PIN_DATE)  else 0)
            homeNotifyChange()
        }

        b.create().show()
    }

    private fun searchSetupButton(
        view: View, list: LinearLayout, d: AlertDialog, e: SearchUrl)
    {
        val z = PopupItemBinding.inflate(LayoutInflater.from(view.context), list, false)
        list.addView(z.root)
        z.itemName.text = e.name
        z.itemName.setOnClickListener {
            d.dismiss()
            searchWith(e)
        }
        z.itemName.setOnLongClickListener {
            d.dismiss()
            searchRenameDialog(view, e)
            true
        }
    }

    private fun searchOptDialog(view: View)
    {
        val z = SearchOptDialogBinding.inflate(LayoutInflater.from(view.context))
        val d = dialogInit(view, z.root).create()
        val them = searchEngine.them
        them.sortWith { x, y -> x.name.compareTo(y.name) }
        for (e in them) {
            searchSetupButton(view, z.dialogList, d, e)
        }
        d.show()
    }

    private fun mainOptDialog(view: View)
    {
        val z = MainOptDialogBinding.inflate(LayoutInflater.from(view.context))
        val b = dialogInit(view, z.root)

        z.readContactList.isChecked = getReadContacts(c)
        z.readContactList.setOnCheckedChangeListener { _, checked ->
            setReadContacts(c, checked)
            learnItems()
            if (checked && !getReadContacts(c)) {
                z.readContactList.isChecked = false
                itemInfoLaunch(c.packageName)
            }
        }

        b.setPositiveButton(getString(R.string.button_ok)) { _, _ -> }

        val d = b.create()

        z.colorChoice.setOnClickListener {
            d.dismiss()
            colorDialog(view)
        }
        z.fontChoice.setOnClickListener {
            d.dismiss()
            fontDialog(view)
        }
        z.timeChoice.setOnClickListener {
            d.dismiss()
            timeDialog(view)
        }
        z.dateChoice.setOnClickListener {
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
