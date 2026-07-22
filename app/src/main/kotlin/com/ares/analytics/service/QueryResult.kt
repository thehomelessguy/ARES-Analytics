package com.ares.analytics.service

data class QueryResult(
    val columns: List<String>,
    val rows: List<List<String>>
)
