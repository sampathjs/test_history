package com.matthey.pmm.limits.reporting

import org.slf4j.LoggerFactory

class LiquidityLimitsChecker(private val connector: LimitsReportingConnector) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val runType = "Liquidity"

    fun check(): List<RunResult> {
        logger.info("checking liquidity limits")

        return connector.liquidityLimits.map {
            logger.info("liquidity limits: $it")
            val fxRate = connector.getGbpUsdRate()
            logger.info("GBP/USD rate: $fxRate")
            val metalPrices = connector.metalPrices
            logger.info("metal prices: $metalPrices")

            val metalBalances = connector.metalBalances
            val liquidity = metalBalances.getBalance(runType, it.metal)
            val breach = liquidity !in it.lowerLimit..it.upperLimit
            val breachLowerLimit = liquidity < it.lowerLimit
            val diff =
                if (!breach) 0 else if (breachLowerLimit) it.lowerLimit - liquidity else liquidity - it.upperLimit
            val breachTOz = getBreachBalancesInTOz(metalBalances, it.metal, diff)
            logger.info("breach in TOz: $breachTOz")
            val liability = breachTOz * metalPrices.getValue(it.metal) / fxRate
            logger.info("liability: $liability")
            val critical = !breachLowerLimit && liability > it.maxLiability
            val liquidityBreachLimit = if (breach) if (breachLowerLimit) "Lower Limit" else "Upper Limit" else ""

            RunResult(
                runTime = connector.runDate,
                runType = runType,
                metal = it.metal,
                liquidityLowerLimit = it.lowerLimit,
                liquidityUpperLimit = it.upperLimit,
                liquidityMaxLiability = it.maxLiability,
                breach = breach,
                liquidityBreachLimit = liquidityBreachLimit,
                currentPosition = liquidity.toDouble(),
                liquidityDiff = diff,
                breachTOz = breachTOz,
                breachGBP = liability,
                critical = critical,
                breachDates = RunResult.getBreachDates(
                    connector,
                    breach,
                    runType = runType,
                    liquidityBreachLimit = liquidityBreachLimit,
                    metal = it.metal
                )
            )
        }
    }

    private fun getBreachBalancesInTOz(metalBalances: MetalBalances, metal: String, diff: Int): Double {
        val balanceLines = connector.balanceLines.filter { it.purpose == runType }.map { it.lineTitle }
        logger.info("balance lines to be used $balanceLines")
        val totalBalances = balanceLines.sumBy { metalBalances.getBalance(it, metal) }
        logger.info("total balance for $metal in TOz: $totalBalances")
        return totalBalances * diff / 100.0
    }
}