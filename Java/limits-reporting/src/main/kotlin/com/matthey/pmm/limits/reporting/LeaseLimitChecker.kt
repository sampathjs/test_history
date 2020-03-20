package com.matthey.pmm.limits.reporting

import org.slf4j.LoggerFactory
import kotlin.math.max

class LeaseLimitChecker(private val connector: LimitsReportingConnector) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun check(): RunResult {
        logger.info("checking lease limit")
        logger.info("lease limit: ${connector.leaseLimit}")
        logger.info("metal prices: ${connector.metalPrices}")
        val fxRate = connector.getGbpUsdRate()
        logger.info("FX rate: $fxRate")

        val runType = "Lease"
        val loanFacilityExposure =
            connector.balanceLines.filter { it.purpose == "Lease" }.map { sumLoanFacility(it.lineTitle) }
                .sum() / fxRate
        logger.info("exposure from loan facilities: $loanFacilityExposure")
        val dealExposure = connector.leaseDeals.map { calcExposure(it) }.sum()
        logger.info("exposure from deals: $dealExposure")
        val totalExposure = loanFacilityExposure + dealExposure
        val breach = totalExposure > connector.leaseLimit
        return RunResult(
            runTime = connector.runDate,
            runType = runType,
            breach = breach,
            positionLimit = connector.leaseLimit,
            currentPosition = totalExposure,
            breachGBP = max(totalExposure - connector.leaseLimit, 0.0),
            breachDates = RunResult.getBreachDates(connector, breach, runType)
        )
    }

    private fun sumLoanFacility(balanceLine: String): Double {
        val sum = MetalBalances.metalNames.keys.map {
            val balance = connector.metalBalances.getBalance(balanceLine, it)
            val exposure = (if (balance < 0) -balance else 0) * connector.metalPrices.getValue(it)
            logger.info("$balanceLine: metal -> $it; balance -> $balance; exposure -> $exposure")
            exposure
        }.sum()
        logger.info("$balanceLine: sum -> ${sum.toInt()}")
        return sum
    }

    private fun calcExposure(deal: LeaseDeal): Double {
        val gbpUsdRate = connector.getGbpUsdRate(deal.startDate)
        logger.info("deal: $deal; GBP/USD rate: $gbpUsdRate")
        return deal.Notnl * deal.currencyFxRate / gbpUsdRate
    }
}