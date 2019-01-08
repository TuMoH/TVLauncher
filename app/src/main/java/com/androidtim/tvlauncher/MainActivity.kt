package com.androidtim.tvlauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


// todo clock
// todo remove, uninstall, move items
class MainActivity : Activity() {

    companion object {
        const val FAVORITES_KEY = "FAVORITES_KEY"
    }

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val favorites = findFavorites().toMutableList()
        recycler.layoutManager = GridLayoutManager(this, 4)
        recycler.adapter = Adapter(
            data = favorites,
            itemClickListener = { position ->
                try {
                    startActivity(
                        packageManager.getLaunchIntentForPackage(
                            favorites[position].packageName
                        )
                    )
                } catch (e: Exception) {
                }
            },
            showAddButton = true,
            addClickListener = {
                val allApps = findAllApplications()
                    .filter { appInfo -> favorites.find { it.packageName == appInfo.packageName } == null }

                val dialogView = LayoutInflater.from(this).inflate(R.layout.add_app_dialog, null)
                val dialogRecycler = dialogView.findViewById<RecyclerView>(R.id.recycler)
                dialogRecycler.layoutManager = GridLayoutManager(this, 4)

                var dialog: AlertDialog? = null

                dialogRecycler.adapter = Adapter(
                    data = allApps,
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
            result = packageManager.getApplicationBanner(packageName)
        } catch (e: Exception) {
        }

        if (result == null) {
            try {
                result = packageManager.getApplicationIcon(packageName)
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

    private fun SharedPreferences.getStringList(key: String): List<String> {
        return TextUtils.split(getString(key, ""), "‚‗‚").toList()
    }

    private fun SharedPreferences.putStringList(key: String, value: List<String>) {
        edit().putString(
            key,
            TextUtils.join("‚‗‚", value.toTypedArray())
        ).apply()
    }


}
















