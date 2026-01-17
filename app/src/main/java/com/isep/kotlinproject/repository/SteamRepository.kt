package com.isep.kotlinproject.repository

import com.isep.kotlinproject.api.SteamApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SteamRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://store.steampowered.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(SteamApiService::class.java)

    suspend fun searchGames(query: String) = api.searchGames(query)
}
