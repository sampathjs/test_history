package com.matthey.pmm.limits.reporting

import org.slf4j.LoggerFactory

class LimitsReportingIntradayChecker(private val connector: LimitsReportingConnector) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val dealingLimitChecker = DealingLimitChecker(connector)

    fun run() {
        try {
            logger.info("process started for ${connector.runDate}")

            val deskResults = dealingLimitChecker.check(DealingLimitType.INTRADAY_DESK)
            logger.info("result for intraday desk limit check $deskResults")
            deskResults.forEach { connector.saveRunResult(it) }

            logger.info("process ended")
        } catch (e: Exception) {
            logger.error("error occurred ${e.message}", e)
            throw e
        }
    }
}