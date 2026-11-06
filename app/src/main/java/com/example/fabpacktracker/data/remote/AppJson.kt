package com.example.fabpacktracker.data.remote

import kotlinx.serialization.json.Json

// !!! стоит все таки вынести json(тот самый, который используеться в RetrofitInstance)
// ведь, при юнит тестах к нему придеться обращаться
object AppJson {
    val json = Json {
        ignoreUnknownKeys = true
    }
}