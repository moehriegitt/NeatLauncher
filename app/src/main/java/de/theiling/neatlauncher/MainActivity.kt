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
import androidx.appcompat.app.AppCompatDelegate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity:
    AppCompatActivity(),
    ItemAdapter.ClickListener
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

    private val homeAdapter = ItemAdapter(homeItems, R.layout.home_item, { true }, this)
    private val drawerAdapter = ItemAdapter(items, R.layout.drawer_item, { !it.hidden }, this)

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

    private var searchStr: String = ""
    private lateinit var z: MainActivityBinding
    private lateinit var searchEngine: SearchEngine
    private lateinit var dateChoice: EnumDate
    private lateinit var timeChoice: EnumTime
    private lateinit var backChoice: EnumBack
    private lateinit var colorChoice: EnumColor
    private lateinit var fontChoice: EnumFont

    // override on....()
    override fun onCreate(
        savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        searchEngine = SearchEngine(c)
        dateChoice = EnumDate(c) { onMinuteTick() }
        timeChoice = EnumTime(c) { onMinuteTick() }
        backChoice = EnumBack(c) { restart() }  // recreate() is not reset enough (bug?)
        colorChoice = EnumColor(c) { recreate() }
        fontChoice = EnumFont(c) { recreate() }

        setTheme(selectTheme(backChoice.x, fontChoice.x, colorChoice.x))
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 29) {
            // avoid translucent navibar in 3-button edge2edge.
            // Alternatively, but for fewer configs, we could:
            //    window.navigationBarColor = getColor(R.color.mainBackground)
            window.isNavigationBarContrastEnforced = false
        }

        z = MainActivityBinding.inflate(layoutInflater)
        setContentView(z.root)

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
            choiceDialog(getViewGroup(), timeChoice)
            true
        }

        z.mainDate.setOnClickListener {
            dateItem?.let { itemLaunch(it) } ?: startCalendar()
        }
        z.mainDate.setOnLongClickListener {
            choiceDialog(getViewGroup(), dateChoice)
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

    override fun onClickItem(view: View, item: Item) {
        if (allowClick) {
            itemLaunch(item)
        }
    }

    override fun onLongClickItem(view: View, item: Item) = itemDialog(view, item)

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

        val anlg = timeChoice.x == time_anlg
        z.mainClockAnalog.visibility = visibleIf(anlg)
        if (anlg) {
            z.mainClockAnalog.updateTime()
        }

        val word = timeChoice.x == time_word
        z.mainClockWord.visibility = visibleIf(word)
        if (word) {
            z.mainClockWord.updateTime()
        }

        val grid = timeChoice.x == time_grid
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
    private fun modePrimary() = if (homeItems.size > 0) Mode.HOME1 else Mode.DRAWER1
    private fun modeSecondary() = Mode.DRAWER2
    private fun drawerNotifyChange() = drawerAdapter.getFilter().filter(searchStr)
    private fun visibleIf(b: Boolean) = if (b) View.VISIBLE else View.GONE
    private fun <T>theSingle(v: List<T>) = if (v.isEmpty()) null else v[0]

    // functionality
    private fun clearSearch() {
        if (z.mainSearch.text.isNotEmpty()) {
            z.mainSearch.text.clear()
            drawerNotifyChange()
        }
    }

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
        when (dateChoice.x) {
            date_syst -> DateFormat.getDateFormat(c).format(now)
            date_yyyy -> simpleFormatDate(now, "yyyy-MM-dd")
            date_EEEy -> simpleFormatDate(now, "EEE yyyy-MM-dd")
            date_EEEE -> simpleFormatDate(now, "EEEE")
            else -> ""
        }

    private fun formatTime(now: Date) =
        when (timeChoice.x) {
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

    private fun itemInfoLaunch(pack: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.data = Uri.parse("package:$pack")
        startActivity(intent)
    }

    private fun restart() {
        val i = intent
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        finish()
        startActivity(intent)
    }

    private fun startUrl(uri: String) =
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))

    private fun searchWith(e: SearchUrl) =
        startUrl(e.url.
            replace("%s", searchStr).
            replace("%L", searchStr.lowercase(Locale.getDefault())))

    // dialogs
    private fun dialogInit(
        view: View, content: View?, title: String, onOK: (() -> Unit)? = null): AlertDialog.Builder
    {
        val b = AlertDialog.Builder(view.context)
        if (content != null) b.setView(content)
        b.setTitle(title)
        if (onOK != null) {
            b.setNegativeButton(getString(R.string.button_cancel)) { _, _ -> }
            b.setPositiveButton(getString(R.string.button_ok)) { _, _ -> onOK() }
        }
        return b
    }

    private fun choiceDialog(v: View, e: PrefEnum)
    {
        val b = dialogInit(v, null, getString(e.titleId))
        b.setSingleChoiceItems(e.nameArrId, e.x) { d, i ->
            d.dismiss()
            e.x = i
        }
        b.create().show()
    }

    private fun itemSetupButton(
        view: View, list: LinearLayout, d: AlertDialog, prefix: String, item: Item)
    {
        val v = LayoutInflater.from(view.context).inflate(R.layout.popup_item, list, false)
        list.addView(v)
        val t = v.findViewById<TextView>(R.id.item_name)
        t.text = item.label.removePrefix(prefix)
        t.setOnClickDismiss(d) { itemLaunch(item) }
        t.setOnLongClickDismiss(d) { itemDialog(view, item) }
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
        val d = dialogInit(view, z.root, item.label).create()

        // start
        z.itemStart.setOnClickDismiss(d) { itemLaunch(item) }

        // rename
        z.itemRename.setOnClickDismiss(d) { renameDialog(view, item) }

        // hide/show
        (if (item.hidden) z.itemHide else z.itemShow).visibility = View.GONE
        z.itemHide.setOnClickDismiss(d) {
            shortToast(getString(R.string.do_hide, item.label))
            item.hidden = true
            if (searchStr == "") drawerNotifyChange()
        }
        z.itemShow.setOnClickDismiss(d) {
            shortToast(getString(R.string.do_show, item.label))
            item.hidden = false
            if (searchStr == "") drawerNotifyChange()
        }

        // remove
        z.itemRemove.visibility = visibleIf(item.type == ITEM_TYPE_PIN)
        z.itemRemove.setOnClickDismiss(d) {
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
        z.itemPin.setOnClickDismiss(d) { pinDialog(view, item) }
        z.itemRid.setOnClickDismiss(d) {
            item.pinned = 0
            homeNotifyChange()
            shortToast(getString(R.string.do_unpin, item.label))
        }

        // info
        z.itemInfo.visibility = visibleIf(item.pack != "")
        z.itemInfo.setOnClickDismiss(d) { itemInfoLaunch(item.pack) }

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
        val d = dialogInit(view, z.root, getString(R.string.rename_title)) {
            item.label = z.editLabel.text.toString()
            item.order = z.editOrder.text.toString()
            itemsNotifyChange()
        }.create()

        z.editLabel.setText(item.label)
        z.editLabel.hint = item.origLabel
        z.editOrder.setText(item.pref.order)

        setOnDoneClickOk(z.editLabel, d)
        setOnDoneClickOk(z.editOrder, d)
        d.show()
    }

    private fun searchRenameDialog(view: View, e: SearchUrl)
    {
        val z = SearchRenameDialogBinding.inflate(LayoutInflater.from(view.context))
        val d = dialogInit(view, z.root, getString(R.string.search_opt_title)) {
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
        }.create()

        z.editName.setText(e.name)
        z.editUrl.setText(e.url)
        z.editDef.isChecked = e.isDefault
        z.editDef.visibility = visibleIf(!e.isDefault)

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
        val d = dialogInit(view, z.root, item.label) {
            item.pinned =
                (if (z.pinHome.isChecked)  ITEM_PIN_HOME else 0) +
                (if (z.pinLeft.isChecked)  pinUnset(leftItem,  ITEM_PIN_LEFT)  else 0) +
                (if (z.pinRight.isChecked) pinUnset(rightItem, ITEM_PIN_RIGHT) else 0) +
                (if (z.pinDown.isChecked)  pinUnset(downItem,  ITEM_PIN_DOWN)  else 0) +
                (if (z.pinTime.isChecked)  pinUnset(timeItem,  ITEM_PIN_TIME)  else 0) +
                (if (z.pinDate.isChecked)  pinUnset(dateItem,  ITEM_PIN_DATE)  else 0)
            homeNotifyChange()
        }.create()

        z.pinHome.isChecked = (item.pinned and ITEM_PIN_HOME) == 0
        z.pinLeft.isChecked = (item.pinned and ITEM_PIN_LEFT) != 0
        z.pinRight.isChecked = (item.pinned and ITEM_PIN_RIGHT) != 0
        z.pinDown.isChecked = (item.pinned and ITEM_PIN_DOWN) != 0
        z.pinTime.isChecked = (item.pinned and ITEM_PIN_TIME) != 0
        z.pinDate.isChecked = (item.pinned and ITEM_PIN_DATE) != 0

        d.show()
    }

    private fun searchSetupButton(
        view: View, list: LinearLayout, d: AlertDialog, e: SearchUrl)
    {
        val z = PopupItemBinding.inflate(LayoutInflater.from(view.context), list, false)
        list.addView(z.root)
        z.itemName.text = e.name
        z.itemName.setOnClickDismiss(d) { searchWith(e) }
        z.itemName.setOnLongClickDismiss(d) { searchRenameDialog(view, e) }
    }

    private fun searchOptDialog(view: View)
    {
        val z = SearchOptDialogBinding.inflate(LayoutInflater.from(view.context))
        val d = dialogInit(view, z.root, getString(R.string.search_opt_title)).create()
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
        val d = dialogInit(view, z.root, getString(R.string.main_opt_title)).create()
        z.readContactList.isChecked = getReadContacts(c)
        z.readContactList.setOnCheckedChangeListener { _, checked ->
            setReadContacts(c, checked)
            learnItems()
            if (checked && !getReadContacts(c)) {
                z.readContactList.isChecked = false
                itemInfoLaunch(c.packageName)
            }
        }
        z.backChoice. setOnClickDismiss(d) { choiceDialog(view, backChoice) }
        z.colorChoice.setOnClickDismiss(d) { choiceDialog(view, colorChoice) }
        z.fontChoice. setOnClickDismiss(d) { choiceDialog(view, fontChoice) }
        z.timeChoice. setOnClickDismiss(d) { choiceDialog(view, timeChoice) }
        z.dateChoice. setOnClickDismiss(d) { choiceDialog(view, dateChoice) }
        z.mainInfo.   setOnClickDismiss(d) { itemInfoLaunch(c.packageName) }
        z.mainAbout.  setOnClickDismiss(d) { aboutDialog(view) }
        d.show()
    }

    private fun aboutDialog(view: View)
    {
        val z = AboutDialogBinding.inflate(LayoutInflater.from(view.context))
        val d = dialogInit(view, z.root, getString(R.string.about_title)).create()
        val i = packageManager.getPackageInfo(packageName, 0)
        z.packageName.text = getString(R.string.package_name, packageName)
        z.version.text = getString(R.string.version_name, i.versionName)
        z.author.text = getString(R.string.author_name, getString(R.string.author))
        z.license.text = getString(R.string.license_name, getString(R.string.license))
        z.author.     setOnClickDismiss(d) { startUrl(getString(R.string.url_author)) }
        z.license.    setOnClickDismiss(d) { startUrl(getString(R.string.url_license)) }
        z.sourceLink. setOnClickDismiss(d) { startUrl(getString(R.string.url_source)) }
        z.packageLink.setOnClickDismiss(d) { startUrl(getString(R.string.url_package)) }
        d.show()
    }
}
