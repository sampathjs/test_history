package com.matthey.pmm.limits.reporting

import org.joda.time.LocalDateTime

data class LeaseDeal(
    val tranNum: String,
    val startDate: LocalDateTime,
    val Notnl: Double,
    val currencyFxRate: Double
)