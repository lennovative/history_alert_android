package com.example.historyalert

data class FactEntry(
    val date: String,
    val year: String,
    val type: String,
    val fact: String,
    val links: List<String>
)