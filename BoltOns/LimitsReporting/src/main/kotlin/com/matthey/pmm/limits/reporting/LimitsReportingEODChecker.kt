package com.matthey.pmm.limits.reporting

import org.joda.time.LocalDate
import org.slf4j.LoggerFactory

class LimitsReportingEODChecker(private val connector: LimitsReportingConnector, emailSender: EmailSender) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val breachNotifier = BreachNotifier(connector, emailSender)
    private val liquidityLimitsChecker = LiquidityLimitsChecker(connector)
    private val dealingLimitChecker = DealingLimitChecker(connector)
    private val leaseLimitChecker = LeaseLimitChecker(connector)

    fun run() {
        try {
            logger.info("process started for ${connector.runDate}")

            val liquidityResults = liquidityLimitsChecker.check()
            logger.info("result for liquidity limits check $liquidityResults")
            liquidityResults.forEach { connector.saveRunResult(it) }
            breachNotifier.sendLiquidityAlert(liquidityResults)

            val overnightResult = dealingLimitChecker.check(DealingLimitType.OVERNIGHT).single()
            logger.info("result for overnight limit check $overnightResult")
            connector.saveRunResult(overnightResult)
            breachNotifier.sendOvernightAlert(overnightResult)

            val overnightDeskResults = dealingLimitChecker.check(DealingLimitType.OVERNIGHT_DESK)
            logger.info("result for overnight desk limit check $overnightDeskResults")
            overnightDeskResults.forEach { connector.saveRunResult(it) }
            breachNotifier.sendDeskAlert(overnightDeskResults)

            val intradayDeskResults = connector.intradayBreaches.filter { it.runTime.toLocalDate() == LocalDate.now() }
                .sortedBy { it.runTime }
            logger.info("result for intraday desk limit check $intradayDeskResults")
            breachNotifier.sendDeskAlert(intradayDeskResults)

            val leaseResult = leaseLimitChecker.check()
            logger.info("result for lease limit check $overnightResult")
            connector.saveRunResult(leaseResult)
            breachNotifier.sendLeaseAlert(leaseResult)

            logger.info("process ended")
        } catch (e: Exception) {
            logger.error("error occurred ${e.message}", e)
            throw e
        }
    }
}