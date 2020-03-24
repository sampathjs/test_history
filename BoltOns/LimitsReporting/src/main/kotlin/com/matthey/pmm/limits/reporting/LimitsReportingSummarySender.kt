package com.matthey.pmm.limits.reporting

import org.slf4j.LoggerFactory

class LimitsReportingSummarySender(private val connector: LimitsReportingConnector, emailSender: EmailSender) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val breachNotifier = BreachNotifier(connector, emailSender)

    fun run() {
        try {
            logger.info("process started for ${connector.runDate}")

            val breaches = (connector.eodBreaches + connector.intradayBreaches).filter {
                it.runTime.isAfter(
                    connector.runDate.minusDays(7)
                )
            }
            logger.info("breaches: $breaches")
            breachNotifier.sendAlertSummary(breaches)

            logger.info("process ended")
        } catch (e: Exception) {
            logger.error("error occurred ${e.message}", e)
            throw e
        }
    }
}
