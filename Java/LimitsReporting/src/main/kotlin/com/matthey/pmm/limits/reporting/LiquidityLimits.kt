package com.matthey.pmm.limits.reporting

data class LiquidityLimits(
    val metal: String,
    val lowerLimit: Int,
    val upperLimit: Int,
    val maxLiability: Int
)