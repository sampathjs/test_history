package com.matthey.pmm.limits.reporting

data class BreachGroup(
    val breachType: String,
    val breaches: List<RunResult>
)