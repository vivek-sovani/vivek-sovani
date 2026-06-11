package com.wp10.launcher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.wp10.launcher.model.AppInfo

object AppLoader {

    fun loadApps(context: Context, pinnedPackages: Set<String> = emptySet()): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        return resolveInfos
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                AppInfo(
                    packageName = ri.activityInfo.packageName,
                    activityName = ri.activityInfo.name,
                    label = ri.loadLabel(pm).toString(),
                    icon = ri.loadIcon(pm),
                    isPinned = ri.activityInfo.packageName in pinnedPackages
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
