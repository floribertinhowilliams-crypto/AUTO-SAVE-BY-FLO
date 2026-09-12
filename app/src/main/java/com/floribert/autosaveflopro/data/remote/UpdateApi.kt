package com.floribert.autosaveflopro.data.remote

import com.floribert.autosaveflopro.data.model.UpdateInfo
import retrofit2.http.GET
import retrofit2.http.Url

interface UpdateApi {
    @GET
    suspend fun getUpdateInfo(@Url url: String): UpdateInfo
}
