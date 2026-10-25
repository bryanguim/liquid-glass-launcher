package com.bruce.liquidglass

import android.app.ActivityOptions
import android.app.Application
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bruce.liquidglass.data.AppInfo
import com.bruce.liquidglass.data.AppRepository
import com.bruce.liquidglass.data.Categories
import com.bruce.liquidglass.data.IconStyle
import com.bruce.liquidglass.data.LauncherPrefs
import com.bruce.liquidglass.data.WallpaperKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

const val PAGE0_CAPACITY = 16 // first page holds the 4x2 clock widget
const val PAGE_CAPACITY = 24  // 4 columns x 6 rows
const val DOCK_CAPACITY = 4

enum class SearchMode { SPOTLIGHT, LIBRARY }

data class MenuTarget(
    val app: AppInfo,
    val bounds: Rect,
    val inDock: Boolean,
    val onHome: Boolean,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = LauncherPrefs(application)
    private val repo = AppRepository(application)
    private var callback: LauncherApps.Callback? = null

    // ---- data ----
    var apps by mutableStateOf<List<AppInfo>>(emptyList()); private set
    var appsByKey by mutableStateOf<Map<String, AppInfo>>(emptyMap()); private set
    var homePages by mutableStateOf<List<List<String>>>(emptyList()); private set
    var dock by mutableStateOf<List<String>>(emptyList()); private set
    var recents by mutableStateOf(prefs.recents); private set
    var loaded by mutableStateOf(false); private set

    // ---- settings ----
    var onboarded by mutableStateOf(prefs.onboarded); private set
    var wallpaper by mutableStateOf(prefs.wallpaper); private set
    var iconStyle by mutableStateOf(prefs.iconStyle); private set
    var animateWallpaper by mutableStateOf(prefs.animateWallpaper); private set
    var showLabels by mutableStateOf(prefs.showLabels); private set
    var photo by mutableStateOf<ImageBitmap?>(null); private set

    // ---- transient UI state ----
    var editing by mutableStateOf(false)
    var selectedForMove by mutableStateOf<String?>(null)
    var menu by mutableStateOf<MenuTarget?>(null)
    var search by mutableStateOf<SearchMode?>(null)
    var openCategory by mutableStateOf<String?>(null)
    var settingsOpen by mutableStateOf(false)

    /** Emitted when the home button is pressed while the launcher is already in front. */
    val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emitted when the user comes back to the launcher from an app (for the zoom-back animation). */
    val returnedHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        loadPhoto()
        refresh()
        callback = repo.registerCallback { refresh() }
    }

    override fun onCleared() {
        callback?.let { repo.unregisterCallback(it) }
    }

    fun refresh() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.Default) { repo.loadApps(ICON_PX) }
            applyApps(list)
        }
    }

    private fun applyApps(list: List<AppInfo>) {
        val byKey = list.associateBy { it.key }
        apps = list
        appsByKey = byKey

        val storedPages = prefs.homePages
        val storedDock = prefs.dock
        if (storedPages == null || storedDock == null) {
            buildInitialLayout(list)
        } else {
            val known = prefs.knownApps
            var pages = storedPages.map { p -> p.filter { it in byKey } }.filter { it.isNotEmpty() }
            val newDock = storedDock.filter { it in byKey }
            val placed = pages.flatten().toSet() + newDock
            // Like iOS: newly installed apps land at the end of the home screen.
            val fresh = list.filter { it.key !in known && it.key !in placed }.map { it.key }
            if (fresh.isNotEmpty()) pages = appendToPages(pages, fresh)
            homePages = pages.ifEmpty { listOf(emptyList()) }
            dock = newDock
            persistLayout()
        }
        recents = recents.filter { it in byKey }
        prefs.knownApps = byKey.keys
        loaded = true
    }

    private fun buildInitialLayout(list: List<AppInfo>) {
        val dockPkgs = repo.defaultDockPackages()
        val newDock = dockPkgs.mapNotNull { pkg -> list.firstOrNull { it.packageName == pkg }?.key }
            .distinct().take(DOCK_CAPACITY)

        val used = newDock.toMutableSet()
        val ordered = mutableListOf<String>()
        fun add(app: AppInfo?) { if (app != null && used.add(app.key)) ordered += app.key }

        // 1. Our own apps first.
        list.filter { it.category == Categories.OURS }.forEach { add(it) }
        // 2. Essentials.
        val essentials = listOf("settings", "photos", "gallery", "camera", "deskclock", "clock", "calendar",
            "android.gm", "maps", "youtube", "vending", "notepad", "notes", "weather", "calculator", "filemanager", "files")
        essentials.forEach { k -> add(list.firstOrNull { it.packageName.contains(k) && it.key !in used }) }
        // 3. Apps the user installed.
        list.filter { !it.isSystem }.forEach { add(it) }

        val firstTwoPages = ordered.take(PAGE0_CAPACITY + PAGE_CAPACITY)
        homePages = appendToPages(listOf(), firstTwoPages).ifEmpty { listOf(emptyList()) }
        dock = newDock
        persistLayout()
    }

    private fun capacity(page: Int) = if (page == 0) PAGE0_CAPACITY else PAGE_CAPACITY

    private fun appendToPages(pages: List<List<String>>, keys: List<String>): List<List<String>> {
        val out = pages.map { it.toMutableList() }.toMutableList()
        if (out.isEmpty()) out += mutableListOf<String>()
        for (k in keys) {
            var last = out.lastIndex
            if (out[last].size >= capacity(last)) { out += mutableListOf<String>(); last++ }
            out[last] += k
        }
        return out
    }

    private fun persistLayout() {
        prefs.homePages = homePages.filter { it.isNotEmpty() }.ifEmpty { listOf(emptyList()) }
        prefs.dock = dock
    }

    // ---------------- actions ----------------

    fun launch(app: AppInfo, boundsInWindow: Rect?, view: View?) {
        val r = boundsInWindow?.let {
            android.graphics.Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt())
        }
        val opts = if (view != null && r != null) {
            ActivityOptions.makeScaleUpAnimation(view, r.left, r.top, r.width(), r.height()).toBundle()
        } else null
        runCatching { repo.launch(app, r, opts) }
        recents = (listOf(app.key) + recents.filter { it != app.key }).take(12)
        prefs.recents = recents
    }

    fun openAppInfo(app: AppInfo) = runCatching { repo.openAppInfo(app) }

    fun uninstall(app: AppInfo) {
        val ctx = getApplication<Application>()
        val i = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(i) }
    }

    fun isOnHome(key: String) = homePages.any { key in it } || key in dock

    fun removeFromHome(key: String) {
        homePages = homePages.map { p -> p.filter { it != key } }.let { pages ->
            // keep page 0 even if empty, drop other empty pages
            pages.filterIndexed { i, p -> i == 0 || p.isNotEmpty() }
        }
        dock = dock.filter { it != key }
        if (selectedForMove == key) selectedForMove = null
        persistLayout()
    }

    fun addToHome(key: String) {
        if (isOnHome(key)) return
        homePages = appendToPages(homePages, listOf(key))
        persistLayout()
    }

    fun addToDock(key: String): Boolean {
        if (key in dock || dock.size >= DOCK_CAPACITY) return false
        homePages = homePages.map { p -> p.filter { it != key } }
            .filterIndexed { i, p -> i == 0 || p.isNotEmpty() }
        dock = dock + key
        persistLayout()
        return true
    }

    fun removeFromDock(key: String) {
        if (key !in dock) return
        dock = dock.filter { it != key }
        homePages = appendToPages(homePages, listOf(key))
        persistLayout()
    }

    /** Edit mode: tap one icon, then another to swap them (works between pages and the dock). */
    fun onEditTap(key: String) {
        val sel = selectedForMove
        when {
            sel == null -> selectedForMove = key
            sel == key -> selectedForMove = null
            else -> { swap(sel, key); selectedForMove = null }
        }
    }

    private fun swap(a: String, b: String) {
        fun swapIn(list: List<String>): List<String> = list.map {
            when (it) { a -> b; b -> a; else -> it }
        }
        homePages = homePages.map { swapIn(it) }
        dock = swapIn(dock)
        persistLayout()
    }

    /** Edit mode: move the selected icon to the end of a page (pageIndex may be one past the last page). */
    fun moveSelectedToPage(pageIndex: Int) {
        val key = selectedForMove ?: return
        val pages = homePages.map { p -> p.filter { it != key }.toMutableList() }.toMutableList()
        val wasInDock = key in dock
        while (pages.size <= pageIndex) pages += mutableListOf<String>()
        if (pages[pageIndex].size >= capacity(pageIndex)) return
        pages[pageIndex] += key
        homePages = pages.filterIndexed { i, p -> i == 0 || p.isNotEmpty() }
        if (wasInDock) dock = dock.filter { it != key }
        selectedForMove = null
        persistLayout()
    }

    fun moveSelectedToDock() {
        val key = selectedForMove ?: return
        if (addToDock(key)) selectedForMove = null
    }

    fun resetLayout() {
        prefs.homePages = null
        prefs.dock = null
        buildInitialLayout(apps)
    }

    // ---------------- settings ----------------

    fun finishOnboarding() { onboarded = true; prefs.onboarded = true }
    fun replayOnboarding() { settingsOpen = false; onboarded = false; prefs.onboarded = false }

    fun setWallpaperKind(k: WallpaperKind) { wallpaper = k; prefs.wallpaper = k }
    fun setIconStyleValue(s: IconStyle) { iconStyle = s; prefs.iconStyle = s }
    fun setAnimateWallpaperValue(v: Boolean) { animateWallpaper = v; prefs.animateWallpaper = v }
    fun setShowLabelsValue(v: Boolean) { showLabels = v; prefs.showLabels = v }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { savePhoto(uri) }
            if (ok) { loadPhoto(); setWallpaperKind(WallpaperKind.PHOTO) }
        }
    }

    private fun photoFile() = File(getApplication<Application>().filesDir, "wallpaper.jpg")

    private fun savePhoto(uri: Uri): Boolean = runCatching {
        val cr = getApplication<Application>().contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (maxSide / sample > 2600) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return false
        FileOutputStream(photoFile()).use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
        true
    }.getOrDefault(false)

    private fun loadPhoto() {
        viewModelScope.launch {
            photo = withContext(Dispatchers.IO) {
                val f = photoFile()
                if (f.exists()) runCatching { BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() }.getOrNull() else null
            }
        }
    }

    fun closeOverlays(): Boolean {
        return when {
            menu != null -> { menu = null; true }
            openCategory != null -> { openCategory = null; true }
            search != null -> { search = null; true }
            settingsOpen -> { settingsOpen = false; true }
            editing -> { editing = false; selectedForMove = null; true }
            else -> false
        }
    }

    companion object { const val ICON_PX = 168 }
}
