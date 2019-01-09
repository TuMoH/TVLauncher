package com.androidtim.tvlauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.text.SimpleDateFormat

// todo wifi
// todo settings
// todo backpressed
// todo widgets?
class MainActivity : Activity() {

    companion object {
        const val FAVORITES_KEY = "FAVORITES_KEY"
        val CLOCK_FORMAT = SimpleDateFormat("HH:mm", Locale.ENGLISH)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = ClockRunnable()

    private lateinit var favorites: MutableList<AppInfo>
    private var appInfoForCheckOnResume: AppInfo? = null

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        favorites = findFavorites().toMutableList()

        initRecycler()
    }

    override fun onResume() {
        super.onResume()
        startClock()

        val finalAppInfoForCheckOnResume = appInfoForCheckOnResume
        appInfoForCheckOnResume = null
        if (finalAppInfoForCheckOnResume != null) {
            if (findAllApplications().find { it.packageName == finalAppInfoForCheckOnResume.packageName } == null) {
                removeFromFavorites(finalAppInfoForCheckOnResume)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pauseClock()
    }

    private fun initRecycler() {
        recycler.layoutManager = GridLayoutManager(this, 5)
        recycler.adapter = Adapter(
            data = favorites,
            itemClickListener = { position ->
                val packageName = favorites[position].packageName
                var intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
                if (intent == null) {
                    intent = packageManager.getLaunchIntentForPackage(packageName)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("TVLauncher", "unable to start activity", e)
                }
            },
            itemLongClickListener = { position ->
                val appInfo = favorites[position]
                var menuDialog: AlertDialog? = null

                val dialogView = LayoutInflater.from(this).inflate(R.layout.item_menu_dialog, null)
                dialogView.findViewById<Button>(R.id.move).setOnClickListener {
                    // todo move items
                    menuDialog?.dismiss()
                }
                dialogView.findViewById<Button>(R.id.remove).setOnClickListener {
                    removeFromFavorites(appInfo)
                    menuDialog?.dismiss()
                }
                dialogView.findViewById<Button>(R.id.uninstall).setOnClickListener {
                    startActivity(
                        Intent(Intent.ACTION_DELETE, Uri.parse("package:${appInfo.packageName}"))
                    )
                    appInfoForCheckOnResume = appInfo
                    menuDialog?.dismiss()
                }

                menuDialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setTitle(appInfo.name)
                    .create()
                menuDialog.show()
            },
            showAddButton = true,
            cropIcons = true,
            addClickListener = {
                val allApps = findAllApplications()
                    .filter { appInfo -> favorites.find { it.packageName == appInfo.packageName } == null }

                val dialogView = LayoutInflater.from(this).inflate(R.layout.add_app_dialog, null)
                val dialogRecycler = dialogView.findViewById<RecyclerView>(R.id.recycler)
                dialogRecycler.layoutManager = GridLayoutManager(this, 3)

                var dialog: AlertDialog? = null

                dialogRecycler.adapter = Adapter(
                    data = allApps,
                    showNames = true,
                    itemClickListener = { position ->
                        val appInfo = allApps[position]
                        favorites.add(appInfo)
                        recycler.adapter?.notifyDataSetChanged()
                        PreferenceManager.getDefaultSharedPreferences(this)
                            .putStringList(FAVORITES_KEY, favorites.map { it.packageName })
                        dialog?.dismiss()
                    })

                dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setTitle(getString(R.string.choose_app))
                    .create()
                dialog.show()
            }
        )
    }

    private fun findFavorites(): List<AppInfo> {
        return PreferenceManager.getDefaultSharedPreferences(this)
            .getStringList(FAVORITES_KEY)
            .map(::getAppInfo)
    }

    private fun findAllApplications(): List<AppInfo> {
        val processedPackages = ArrayList<String>()
        val entries = ArrayList<AppInfo>()

        val leanbackIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        packageManager.queryIntentActivities(leanbackIntent, 0)
            .forEach { resolveInfo ->
                val appPackageName = resolveInfo.activityInfo.packageName
                if (packageName != appPackageName) {
                    processedPackages.add(appPackageName)

                    entries.add(getAppInfo(appPackageName))
                }
            }

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, 0)
            .forEach { resolveInfo ->
                val appPackageName = resolveInfo.activityInfo.packageName
                if (packageName != appPackageName && !processedPackages.contains(resolveInfo.activityInfo.packageName)) {
                    entries.add(getAppInfo(appPackageName))
                }
            }

        entries.sortBy { appInfo -> appInfo.name }
        return entries
    }

    private fun loadIcon(packageName: String): Drawable? {
        var result: Drawable? = null
        try {
            result = packageManager.getActivityBanner(packageManager.getLeanbackLaunchIntentForPackage(packageName))
        } catch (e: Exception) {
        }

        if (result == null) {
            try {
                result = packageManager.getActivityIcon(packageManager.getLaunchIntentForPackage(packageName))
            } catch (e: Exception) {
            }
        }
        return result
    }

    private fun loadName(packageName: String): String {
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun getAppInfo(packageName: String): AppInfo {
        return AppInfo(
            packageName = packageName,
            name = loadName(packageName),
            icon = loadIcon(packageName)
        )
    }

    private fun removeFromFavorites(appInfo: AppInfo) {
        favorites.remove(appInfo)
        recycler.adapter?.notifyDataSetChanged()
        PreferenceManager.getDefaultSharedPreferences(this)
            .putStringList(FAVORITES_KEY, favorites.map { favorite -> favorite.packageName })
    }

    private fun startClock() {
        clockRunnable.run()
    }

    private fun pauseClock() {
        handler.removeCallbacks(clockRunnable)
    }

    inner class ClockRunnable : Runnable {
        override fun run() {
            val date = Date()
            clock_view.text = CLOCK_FORMAT.format(date)

            val currentTime = date.time
            date.seconds = 0
            date.minutes = date.minutes + 1
            val delay = date.time - currentTime
            handler.postDelayed(this, if (delay >= 0) delay else 0)
        }

    }

}
