package de.theiling.neatlauncher

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
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
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity:
    AppCompatActivity(),
    ItemAdapter.ClickListener,
    ActivityCompat.OnRequestPermissionsResultCallback
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
    private var weathItem : Item? = null
    private var dateItem : Item? = null
    private var bgrdItem : Item? = null

    private val homeAdapter = ItemAdapter(homeItems, R.layout.home_item, { true }, this)
    private val drawerAdapter = ItemAdapter(items, R.layout.drawer_item, { !it.hidden }, this)

    // Having both Fling events on the main view and Click events on an item view
    // seems to sometimes generate both a fling and a click event from one gesture.
    // The click always comes second, and despite onFling returning true.  To fix this,
    // we use another guard 'allowClick' to prevent the click if a Fling was seen.
    // It is set to true on Down and to false on confirmed Fling, preventing Click from
    // the same Down.  This is hacky -- dunno why it is needed.
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
    private var clockValid = false
    private var clockWeatherValid = false
    private var weatherData: WeatherData? = null
    private var weatherUpdateMillis = 15L * 60_000L  // currently non-configurable
    private var weatherTryLongMillis = 15L * 60_000L
    private var weatherTryShortMillis = 2L * 60_000L
    private var weatherTryMinMillis = 1_000L
    private var weatherTryLast = Date(0)
    private var weatherTryNext = Date(0)
    private lateinit var z: MainActivityBinding
    private lateinit var searchEngine: SearchEngine
    private lateinit var weather: WeatherEngine
    private lateinit var dateChoice: EnumDate
    private lateinit var timeChoice: EnumTime
    private lateinit var backChoice: EnumBack
    private lateinit var colorChoice: EnumColor
    private lateinit var fontChoice: EnumFont
    private lateinit var ttypeChoice: EnumTtype
    private lateinit var tempChoice: EnumTemp
    private lateinit var contactChoice: BoolContact
    private lateinit var weekStart: EnumWstart

    // override on....()
    override fun onCreate(
        savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        searchEngine = SearchEngine(c)
        weather = WeatherEngine(c)
        dateChoice = EnumDate(c) { clockRedraw(true) }
        timeChoice = EnumTime(c) { clockRedraw(true) }
        backChoice = EnumBack(c) { restart() }  // recreate() is not reset enough (bug?)
        colorChoice = EnumColor(c) { recreate() }
        fontChoice = EnumFont(c) { recreate() }
        tempChoice = EnumTemp(c) { weatherRedraw() }
        ttypeChoice = EnumTtype(c) { weatherRedraw() }
        weekStart = EnumWstart(c) { weatherRedraw() }

        contactChoice = BoolContact(c) {
            if (it >= 0) { // -1 resets but does not trigger itemsNotifyChange()
                if (it == 0 || checkRequestPerm(Manifest.permission.READ_CONTACTS, 1717)) {
                    itemsNotifyChange(2)
                }
            }
        }

        // Maybe we have no weather data, then this will fail:
        try {
            weatherData = WeatherData.loadPref(c)
        } catch (_: Exception) {}

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
                true -> searchOptDialog(viewGroup)
                else -> mainOptDialog(viewGroup)
            }
        }
        z.mainSearchOpt.setOnLongClickListener {
            mainOptDialog(viewGroup)
            true
        }
        z.content.setOnLongClickListener {
            bgrdItem?.let { itemLaunch(it) } ?: mainOptDialog(viewGroup)
            true
        }

        if (Build.VERSION.SDK_INT >= 26) {
            bootSeq = packageManager.getChangedPackages(0)?.sequenceNumber ?: 0
        }

        z.clockBox.setOnClickListener {
            // SHOW_ALARMS requires special permissions, so we'll just start the item instead.
            timeItem?.let { itemLaunch(it) } ?:
                startActivity(packageIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS)))
        }
        z.clockBox.setOnLongClickListener {
            choiceDialog(viewGroup, timeChoice)
            true
        }

        z.mainDate.setOnClickListener {
            dateItem?.let { itemLaunch(it) } ?: startCalendar()
        }
        z.mainDate.setOnLongClickListener {
            choiceDialog(viewGroup, dateChoice)
            true
        }

        z.weatherGrid.setOnClickListener {
            weathItem?.let { itemLaunch(it) } ?: startUrl(getString(R.string.open_meteo_url))
        }
        z.weatherGrid.setOnLongClickListener {
            weatherDialog(viewGroup)
            true
        }
    }

    override fun onCreateOptionsMenu(m: Menu): Boolean {
        m.add(getString(R.string.main_opt_title)).setOnMenuItemClickListener {
            // FIXME: dunno how to immediately launch it.  This is the creation callback,
            // not the click callback.
            mainOptDialog(viewGroup)
            true
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(minuteTick, IntentFilter(Intent.ACTION_TIME_TICK))
        onMinuteTick()
        searchEngine.loadPref()
        weather.loadPref()
        itemsNotifyChange(2)
    }

    override fun onStop() {
        unregisterReceiver(minuteTick)
        resetView(false)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        itemsNotifyChange(0)
        onMinuteTick()
    }

    // This makes the 'Home' button reset the view.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        resetView(true)
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val granted = grantResults.isNotEmpty() &&
            (grantResults[0] == PackageManager.PERMISSION_GRANTED)
        when (requestCode) {
           1717 -> if (granted) itemsNotifyChange(2) else {
               shortToast(getString(R.string.read_contacts_no_perm))
               contactChoice.x = -1  // don't try again
           }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() =
        resetView(!z.mainSearch.hasFocus())

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        gestures.onTouchEvent(e)
        return super.dispatchTouchEvent(e)
    }

    private fun onFlingRight() =
        rightItem?.let { itemLaunch(it) } ?: startPhone()

    private fun onFlingLeft() =
        leftItem?.let { itemLaunch(it) } ?:
            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))

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
        clockValid = false
        weatherUpdate(false)
        clockRedraw(false)
    }

    private fun onWeatherData() {
        clockWeatherValid = false
        weatherRedraw()
        clockRedraw(false)
    }

    // auxiliary oneliners
    private val c get() = applicationContext
    private fun shortToast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun longToast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    private val langCode get() = Locale.getDefault().language
    private val viewGroup: ViewGroup get() = findViewById(android.R.id.content)
    private val defaultDisplay get() = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
    private fun getUserHandle(uid: Long): UserHandle = userManager.getUserForSerialNumber(uid)
    private val userManager get() = c.getSystemService(USER_SERVICE) as UserManager
    private val displayManager get() = c.getSystemService(DISPLAY_SERVICE) as DisplayManager
    private val launcher get() = c.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val inputMethodManager get() =
        c.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

    private fun packageIntent(s: String) = packageManager.getLaunchIntentForPackage(s)
    private fun modePrimary() = if (homeItems.size > 0) Mode.HOME1 else Mode.DRAWER1
    private fun modeSecondary() = Mode.DRAWER2
    private fun drawerNotifyChange() = drawerAdapter.getFilter().filter(searchStr)
    private fun visibleIf(b: Boolean) = if (b) View.VISIBLE else View.GONE
    private fun <T>theSingle(v: List<T>) = if (v.isEmpty()) null else v[0]

    private fun urlText(url: String): String =
        (URL(url).openConnection() as HttpURLConnection).inputStream.bufferedReader().readText()

    // functionality
    private fun clockRedraw(force: Boolean) {
        val anlg = timeChoice.x == time_anlg
        val canWeather = anlg

        // guards
        if (force) clockValid = false
        if (canWeather && !clockWeatherValid) clockValid = false
        if (clockValid) return
        clockValid = true
        clockWeatherValid = true

        // doit
        val now = Date()

        // update clock and date
        val fmt = formatTime(now)
        z.clockDigital.text = fmt
        z.clockDigital.visibility = visibleIf(fmt.isNotEmpty())

        val dat = formatDate(now)
        z.mainDate.text = dat
        z.mainDate.visibility = visibleIf(dat.isNotEmpty())

        z.clockAnalog.visibility = visibleIf(anlg)
        z.clockAnalog.weatherData = weatherData
        if (anlg) {
            z.clockAnalog.updateTime()
        }

        val word = timeChoice.x == time_word
        z.clockWord.visibility = visibleIf(word)
        if (word) {
            z.clockWord.updateTime()
        }

        val grid = timeChoice.x == time_grid
        z.clockGrid.visibility = visibleIf(grid)
        if (grid) {
            z.clockGrid.updateTime()
        }

        z.datetimeTopMargin.visibility = when {
            anlg -> View.GONE
            word -> View.VISIBLE
            grid -> View.GONE
            fmt.isNotEmpty() -> View.VISIBLE
            dat.isNotEmpty() -> View.VISIBLE
            else -> View.GONE
        }
    }

    private fun clearSearch() {
        if (z.mainSearch.text.isNotEmpty()) {
            z.mainSearch.text.clear()
            drawerNotifyChange()
        }
    }

    private fun homeNotifyChange() {
        val pinItems = items.filter { (it.pinned != 0) }
        leftItem  = theSingle(pinItems.filter { (it.pinned and ITEM_PIN_LEFT) != 0 })
        rightItem = theSingle(pinItems.filter { (it.pinned and ITEM_PIN_RIGHT) != 0 })
        downItem  = theSingle(pinItems.filter { (it.pinned and ITEM_PIN_DOWN) != 0 })
        timeItem  = theSingle(pinItems.filter { (it.pinned and ITEM_PIN_TIME) != 0 })
        dateItem  = theSingle(pinItems.filter { (it.pinned and ITEM_PIN_DATE) != 0 })
        bgrdItem  = theSingle(pinItems.filter { (it.pinned and ITEM_PIN_BGRD) != 0 })
        weathItem = theSingle(pinItems.filter { (it.pinned and ITEM_PIN_WEATH) != 0 })
        homeItems.clear()
        pinItems.filterTo(homeItems) { (it.pinned and ITEM_PIN_HOME) != 0 }
        homeAdapter.getFilter().filter("")
        resetMode()
    }

    private fun checkRequestPerm(perm: String, code: Int): Boolean {
        if (ContextCompat.checkSelfPermission(c, perm) == PackageManager.PERMISSION_GRANTED)
            return true
        requestPermissions(arrayOf(perm), code)
        return false
    }

    private fun learnItems() {
        val c: Context = this
        val um = userManager
        val la = launcher
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
        if (contactChoice.x > 0) {
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
                contactChoice.x = -1   // don't try again
            }
        }
    }

    private fun refreshItems(forceRead: Boolean): Boolean {
        var doRead = forceRead
        if (Build.VERSION.SDK_INT >= 26) {
            packageManager.getChangedPackages(bootSeq)?.let {
                bootSeq = it.sequenceNumber
                if (it.packageNames.size > 0) {
                    doRead = true
                }
            }
        }
        if (doRead) {
            learnItems()
        }
        return doRead
    }

    private fun itemsNotifyChange(forceLevel: Int) {
        var doRedraw = forceLevel >= 1
        if (refreshItems(forceLevel >= 2)) {
            doRedraw = true
        }
        if (doRedraw) {
            homeNotifyChange()
            drawerNotifyChange()
            resetMode()
        }
    }

    private fun removeShortcut(sht: Item) = try {
        // Unfortunately, there is no 'unpinShortcut', only 'pinShortcuts' to set all
        // of them.  We need to set all of them except `sht`.
        if (Build.VERSION.SDK_INT >= 25) {
            val myUha = Process.myUserHandle()
            val la = launcher
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
        if (clear || (defaultDisplay.state != Display.STATE_ON)) {
            clearSearch()
        }
        z.mainSearch.clearFocus()
        inputMethodManager.hideSoftInputFromWindow(viewGroup.windowToken, 0)
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
                    val uha = getUserHandle(item.uid)
                    if (Build.VERSION.SDK_INT >= 25) {
                        launcher.startShortcut(item.pack, item.act, null, null, uha)
                    }
                }
                ITEM_TYPE_CONTACT -> openContact(item)
            }
        } catch (e: Exception) {
            shortToast(getString(R.string.error_starting, item.label))
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
            replace("%s", searchStr.toUrl()).
            replace("%L", searchStr.lowercase(Locale.getDefault()).toUrl()))

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

    private fun choiceDialog(v: View, e: PrefChoice)
    {
        val b = dialogInit(v, null, getString(e.titleId))
        b.setSingleChoiceItems(e.names(), e.x) { d, i ->
            d.dismiss()
            e.x = i
        }
        b.create().show()
    }

    private fun itemSetupButton(
        view: View, list: LinearLayout, d: AlertDialog, prefix: String, item: Item)
    {
        val v = LayoutInflater.from(view.context).inflate(R.layout.popup_action, list, false)
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

        z.itemStart.setOnClickDismiss(d) { itemLaunch(item) }

        z.itemRename.setOnClickDismiss(d) { renameDialog(view, item) }

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

        z.itemRemove.visibility = visibleIf(item.type == ITEM_TYPE_PIN)
        z.itemRemove.setOnClickDismiss(d) {
            val ok = removeShortcut(item)
            shortToast(
                getString(
                    (if (ok) R.string.ok_remove_shortcut else R.string.error_removing),
                    item.label))
            itemsNotifyChange(2)
        }

        (if (item.pinned != 0) z.itemPin else z.itemRid).visibility = View.GONE
        z.itemPin.setOnClickDismiss(d) { pinDialog(view, item) }
        z.itemRid.setOnClickDismiss(d) {
            item.pinned = 0
            homeNotifyChange()
            shortToast(getString(R.string.do_unpin, item.label))
        }

        z.itemInfo.visibility = visibleIf(item.pack != "")
        z.itemInfo.setOnClickDismiss(d) { itemInfoLaunch(item.pack) }

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
            itemsNotifyChange(1)
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
                (if (z.pinDate.isChecked)  pinUnset(dateItem,  ITEM_PIN_DATE)  else 0) +
                (if (z.pinBgrd.isChecked)  pinUnset(bgrdItem,  ITEM_PIN_BGRD)  else 0) +
                (if (z.pinWeath.isChecked) pinUnset(weathItem, ITEM_PIN_WEATH) else 0)
            homeNotifyChange()
        }.create()

        z.pinHome.isChecked  = (item.pinned and ITEM_PIN_HOME) == 0
        z.pinLeft.isChecked  = (item.pinned and ITEM_PIN_LEFT) != 0
        z.pinRight.isChecked = (item.pinned and ITEM_PIN_RIGHT) != 0
        z.pinDown.isChecked  = (item.pinned and ITEM_PIN_DOWN) != 0
        z.pinTime.isChecked  = (item.pinned and ITEM_PIN_TIME) != 0
        z.pinDate.isChecked  = (item.pinned and ITEM_PIN_DATE) != 0
        z.pinBgrd.isChecked  = (item.pinned and ITEM_PIN_BGRD) != 0
        z.pinWeath.isChecked = (item.pinned and ITEM_PIN_WEATH) != 0

        d.show()
    }

    private fun searchSetupButton(
        view: View, list: LinearLayout, d: AlertDialog, e: SearchUrl)
    {
        val z = PopupActionBinding.inflate(LayoutInflater.from(view.context), list, false)
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

    private fun aboutDialog(view: View)
    {
        val z = AboutDialogBinding.inflate(LayoutInflater.from(view.context))
        val d = dialogInit(view, z.root, getString(R.string.about_title)).create()
        val i = packageManager.getPackageInfo(packageName, 0)
        z.versionName.text = getString(R.string.package_version_code,
            packageName, i.versionName, i.versionCode)
        z.author.text = getString(R.string.author_name, getString(R.string.author))
        z.license.text = getString(R.string.license_name, getString(R.string.license))
        z.sourceLink.text = getString(R.string.source_link, getString(R.string.source_repo))
        z.packageLink.text = getString(R.string.package_link, getString(R.string.package_repo))
        z.authorBox. setOnClickDismiss(d) { startUrl(getString(R.string.author_url)) }
        z.licenseBox.setOnClickDismiss(d) { startUrl(getString(R.string.license_url)) }
        z.sourceBox. setOnClickDismiss(d) { startUrl(getString(R.string.source_url)) }
        z.packageBox.setOnClickDismiss(d) { startUrl(getString(R.string.package_url)) }
        z.ubuntuBox. setOnClickDismiss(d) { startUrl(getString(R.string.ubuntu_url)) }
        z.oMeteoBox. setOnClickDismiss(d) { startUrl(getString(R.string.lic_open_meteo_url)) }
        d.show()
    }

    private fun mainOptDialog(view: View)
    {
        val z = MainOptDialogBinding.inflate(LayoutInflater.from(view.context))
        val d = dialogInit(view, z.root, getString(R.string.main_opt_title)).create()
        z.backChoice.   setOnClickDismiss(d) { choiceDialog(view, backChoice) }
        z.colorChoice.  setOnClickDismiss(d) { choiceDialog(view, colorChoice) }
        z.fontChoice.   setOnClickDismiss(d) { choiceDialog(view, fontChoice) }
        z.timeChoice.   setOnClickDismiss(d) { choiceDialog(view, timeChoice) }
        z.dateChoice.   setOnClickDismiss(d) { choiceDialog(view, dateChoice) }
        z.contactChoice.setOnClickDismiss(d) { choiceDialog(view, contactChoice) }
        z.weatherMenu.  setOnClickDismiss(d) { weatherDialog(view) }
        z.mainInfo.     setOnClickDismiss(d) { itemInfoLaunch(c.packageName) }
        z.mainAbout.    setOnClickDismiss(d) { aboutDialog(view) }
        d.show()
    }

    // Weather
    private fun weatherRedraw() {
        z.weatherBox.visibility = visibleIf(weatherRender())
    }

    private fun weatherRender(): Boolean {
        val act = weather.active ?: return false
        val dat = weatherData ?: return false

        // process 'now'
        val cal = Calendar.getInstance()
        val day = dat.day.filter { cal.time <= it.last }
        if (day.isEmpty()) return false

        // title
        z.weatherLoc.text =
            (if (act.isCurrent) getString(R.string.current_loc_title) else act.name) +
            (if (cal.timeZone.getOffset(cal.time.time) != dat.timeZone.getOffset(cal.time.time))
                " ${dat.timeZone.id}" else "")

        // set time zone to get week days right in table (may be out of sync with date display!)
        cal.timeZone = dat.timeZone

        // rows min/max temp
        z.gridBu.text = "$tempChoice"
        z.gridCu.text = "$tempChoice"
        for (i in 0..6) {
            val ci = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
            val wi = (ci[Calendar.DAY_OF_WEEK] + 7 - Calendar.MONDAY) % 7  // day of week w/ 0=Mon
            val d = day.firstOrNull { (ci.time >= it.start) && (ci.time <= it.last) }
            z.gridS[i].visibility = visibleIf(wi == weekStart.x)
            z.gridB[i].text = if (d == null) "" else d.tMax(ttypeChoice)[tempChoice].ceilString()
            z.gridC[i].text = if (d == null) "" else d.tMin(ttypeChoice)[tempChoice].floorString()
            z.gridE[i].text = if (d == null) "" else d.code.toString()
        }

        return true
    }

    private fun weatherClear()
    {
        weatherData?.clearPref()
        weatherData = null
    }

    private fun weatherUpdate(forceLoad: Boolean)
    {
        var doLoad = forceLoad

        val now = Date()
        val wd = weatherData
        // update rate
        if ((wd == null) || ((now.time - wd.timeStamp.time) >= weatherUpdateMillis)) {
            doLoad = true
        }

        // error rate limit
        if (!forceLoad && (now.time < weatherTryNext.time)) {
            doLoad = false
        }
        // last resort rate limit
        if (now.time < (weatherTryLast.time + weatherTryMinMillis)) {
            doLoad = false
        }

        val loc = weather.active
        if (!doLoad || (loc == null)) {
            onWeatherData() // make sure weatherData is removed from display
            return
        }

        // if the location switched, remove weather data until we have fresh data
        if (forceLoad) {
            weatherClear()
            onWeatherData()
        }
        shortToast("Net: Weather?")
        weatherTryLast = now
        weatherTryNext = Date(now.time + weatherTryLongMillis)

        lifecycleScope.launch(Dispatchers.IO) {
            val url = String.format(WEATHER_FORECAST_URL, loc.lat.toUrl(), loc.lon.toUrl())
            try {
                val j = JSONObject(urlText(url))
                val w = WeatherData.fromOpenMeteo(c, j)
                weatherData = w
                w.savePref()
                runOnUiThread {
                    onWeatherData()
                }
            } catch (e: UnknownHostException) {
                // ignore: we have no internet access, but try again sooner
                weatherTryNext = Date(now.time + weatherTryShortMillis)
            } catch (e: Exception) {
                runOnUiThread {
                    longToast(getString(R.string.error_msg, "$e"))
                }
            }
        }
    }

    private fun weatherNotify()
    {
        if (weather.modified) {
            if (weather.activeModified) {
                weather.active.let { longToast(when (it) {
                    null -> getString(R.string.switched_off_weather)
                    weather.current -> getString(R.string.switched_to_current_location)
                    else -> getString(R.string.switched_to_location, it.name)
                })}
                weatherUpdate(true)
            }
            weather.savePref()
        }
    }

    private fun locDialog(view: View, e: WeatherLoc)
    {
        val z = LocDialogBinding.inflate(LayoutInflater.from(view.context))
        val d = dialogInit(view, z.root, getString(R.string.location_title)) {
            if (z.editDel.isChecked) {
                weather.delete(e)
            }
            else {
                e.name = z.editLabel.text.toString()
                e.order = z.editOrder.text.toString()
            }
            weatherNotify()
        }.create()

        z.geoLink.setOnClickDismiss(d) {
            startUrl("geo:${e.lat},${e.lon}")
        }

        z.editLabel.setText(e.name)
        z.editOrder.setText(e.orderOrEmpty)

        setOnDoneClickOk(z.editLabel, d)
        setOnDoneClickOk(z.editOrder, d)
        d.show()
    }

    private fun weatherDialog(view: View)
    {
        val z = WeatherDialogBinding.inflate(LayoutInflater.from(view.context))
        val b = dialogInit(view, z.root, getString(R.string.weather_title))
        b.setOnDismissListener { weatherNotify() }

        val p = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val l = mutableListOf<Pair<RadioButton, WeatherLoc>>()
        for (loc in weather.them) {
            if (loc.isCurrent) continue
            val r = RadioButtonBinding.inflate(LayoutInflater.from(view.context)).root
            r.layoutParams = p   // for some reasons, these are not taken from layout
            z.locationChoice.addView(r)
            r.text = loc.name
            l.add(Pair(r, loc))
        }

        val d = b.create()
        z.noWeather.isChecked = weather.active == null
        z.noWeather.setOnClickDismiss(d) { weather.active?.isActive = false }

        z.currentLocation.isChecked = (weather.current == weather.active)
        z.currentLocation.setOnClickDismiss(d) {
            weather.current.isActive = true
            weather.touch(weather.current) // reload loc even if active not changed
        }

        for (e in l) {
            val (r,loc) = e
            r.isChecked = loc.isActive
            r.setOnClickDismiss(d) { loc.isActive = true }
            r.setOnLongClickDismiss(d) { locDialog(view, loc) }
        }

        z.newLocation.setOnEditorActionListener { _, aid, _ ->
            val done = (aid == EditorInfo.IME_ACTION_DONE)
            val str = z.newLocation.text.toString()
            if (done && (str != "")) {
                d.dismiss()
                searchLocationDialog(view, str)
            }
            done
        }

        z.ttypeChoice. setOnClickDismiss(d) { choiceDialog(view, ttypeChoice) }
        z.weekStart.   setOnClickDismiss(d) { choiceDialog(view, weekStart) }
        z.tempChoice.  setOnClickDismiss(d) { choiceDialog(view, tempChoice) }
        z.oMeteoLink.  setOnClickDismiss(d) { startUrl(getString(R.string.open_meteo_url)) }
        d.show()
    }

    private fun searchLocationDialog(view: View, search: String)
    {
        val z = SearchLocationBinding.inflate(LayoutInflater.from(view.context))
        val d = dialogInit(view, z.root, getString(R.string.search_location_title)).create()
        val l = mutableListOf<ListItem>()
        val a = ListAdapter(l, R.layout.popup_item)
        z.list.setAdapter(a)
        d.show()
        lifecycleScope.launch(Dispatchers.IO) {
            val url = String.format(SEARCH_LOC_URL, langCode.toUrl(), 100.toUrl(), search.toUrl())
            try {
                val res = JSONObject(urlText(url)).optJSONArray("results") ?: JSONArray()
                for (i in 0 until res.length()) {
                    val o = res.getJSONObject(i)
                    val short = o.getString("name")
                    val name = StringBuilder().apply {
                        append(short)
                        for (key in listOf("admin4", "admin3", "admin2", "admin1", "country")) {
                            o.optString(key).let { if (it != "") append(", $it") }
                        }
                    }.toString()
                    val lat = o.getDouble("latitude")
                    val lon = o.getDouble("longitude")
                    val item = ListItem(name) {
                        d.dismiss()
                        weather.add(name, lat, lon, true)
                        weatherNotify()
                    }
                    l.add(item)
                }
                l.sortWith { a,b -> a.text.compareTo(b.text) }
                runOnUiThread {
                    z.statusMsg.text = getString(R.string.search_empty)
                    if (l.isNotEmpty()) {
                        a.notifyDataSetChanged()
                        z.statusMsg.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    z.statusMsg.text = getString(R.string.error_msg, "$e")
                }
            }
        }
    }
}
