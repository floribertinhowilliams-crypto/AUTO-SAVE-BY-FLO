package com.floribert.autosaveflopro.data.model

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changeLog: String
)
