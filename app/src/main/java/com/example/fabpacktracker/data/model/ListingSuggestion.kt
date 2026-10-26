package com.example.fabpacktracker.data.model

data class ListingSuggestion(
    val name : String,
    val description: String,
    val missing : List<String> = emptyList()
)