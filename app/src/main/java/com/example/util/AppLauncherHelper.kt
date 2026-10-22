package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore

object AppLauncherHelper {

    fun launchAppByName(context: Context, appQuery: String): Boolean {
        val cleanQuery = appQuery.trim().lowercase()
        if (cleanQuery.isEmpty()) return false

        // Common system fallbacks
        when {
            cleanQuery.contains("camera") || cleanQuery.contains("kamera") -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            }
            cleanQuery.contains("call") || cleanQuery.contains("dial") || cleanQuery.contains("phone") -> {
                val intent = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }
            cleanQuery.contains("browser") || cleanQuery.contains("chrome") -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }
        }

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(mainIntent, 0)

        // Exact match
        for (resolveInfo in apps) {
            val label = resolveInfo.loadLabel(pm).toString().lowercase()
            if (label == cleanQuery || cleanQuery.contains(label) || label.contains(cleanQuery)) {
                val launchIntent = pm.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }
        }

        return false
    }
}
