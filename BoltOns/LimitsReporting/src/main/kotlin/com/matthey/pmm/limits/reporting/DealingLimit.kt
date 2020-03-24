package com.matthey.pmm.limits.reporting

data class DealingLimit(
    val limitType: String,
    val desk: String,
    val metal: String,
    val limit: Int
)