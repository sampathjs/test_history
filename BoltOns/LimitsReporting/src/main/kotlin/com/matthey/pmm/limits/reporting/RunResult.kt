package com.matthey.pmm.limits.reporting

import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

data class RunResult(
    val runTime: LocalDateTime,
    val runType: String,
    val desk: String = "",
    val metal: String = "",
    val liquidityLowerLimit: Int = 0,
    val liquidityUpperLimit: Int = 0,
    val liquidityMaxLiability: Int = 0,
    val positionLimit: Int = 0,
    val breach: Boolean = false,
    val liquidityBreachLimit: String = "",
    val currentPosition: Double = 0.0,
    val liquidityDiff: Int = 0,
    val breachTOz: Double = 0.0,
    val breachGBP: Double = 0.0,
    val critical: Boolean = false,
    val breachDates: List<String> = listOf()
) {
    companion object {
        const val DATE_SEPARATOR = ", "
        const val DATE_PATTERN = "dd-MM-yy"
        val dateFormat = DateTimeFormat.forPattern(DATE_PATTERN) ?: error("can't get date time format for dd-MM-yy")

        fun getBreachDates(
            connector: LimitsReportingConnector,
            breach: Boolean,
            runType: String,
            liquidityBreachLimit: String = "",
            desk: String = "",
            metal: String = ""
        ): List<String> {
            return if (breach) {
                val previousBreachDates =
                    connector.getPreviousBreachDates(runType, connector.runDate, liquidityBreachLimit, desk, metal)
                        ?.split(DATE_SEPARATOR)
                (previousBreachDates ?: listOf()) + dateFormat.print(connector.runDate)
            } else {
                listOf()
            }
        }
    }

    // used by FreeMarker template
    @Suppress("unused")
    val criticalBreachDates: Boolean
        get() = (liquidityBreachLimit == "Upper Limit" && breachDates.size > 20) || (liquidityBreachLimit == "Lower Limit" && breachDates.size > 10)
}
