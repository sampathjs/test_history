package com.matthey.pmm.limits.reporting

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import com.matthey.pmm.limits.reporting.DealingLimitType.*
import org.slf4j.LoggerFactory
import kotlin.math.max

enum class DealingLimitType(val fullName: String) {
    OVERNIGHT("Overnight"), INTRADAY_DESK("Intraday Desk"), OVERNIGHT_DESK("Overnight Desk")
}

class DealingLimitChecker(private val connector: LimitsReportingConnector) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun check(type: DealingLimitType): List<RunResult> {
        logger.info("checking overnight limits")

        val fxRate = connector.getGbpUsdRate()
        logger.info("GBP/USD rate: $fxRate")
        val metalPrices = connector.metalPrices
        logger.info("metal prices: $metalPrices")
        val closingPositions = connector.closingPositions
        logger.info("closing positions: $closingPositions")

        when (type) {
            OVERNIGHT -> {
                val overnightLimit = connector.dealingLimits.single { it.limitType == "Overnight" }.limit
                logger.info("overnight limit $overnightLimit")
                val positions =
                    connector.closingPositions.columnMap().mapValues { (_, positions) -> positions.values.sum() }
                logger.info("closing positions by metal: $positions")
                val extraPositions = connector.unhedgedAndRefiningGainsPositions
                logger.info("unhedged and refining gains positions: $extraPositions")
                val position =
                    (sumForAllMetals(positions, metalPrices) + sumForAllMetals(extraPositions, metalPrices)) / fxRate
                val breach = position > overnightLimit

                return listOf(
                    RunResult(
                        runTime = connector.runDate,
                        runType = type.fullName,
                        positionLimit = overnightLimit,
                        breach = breach,
                        currentPosition = position,
                        breachGBP = max(position - overnightLimit, 0.0),
                        breachDates = RunResult.getBreachDates(connector, breach, type.fullName)
                    )
                )
            }
            INTRADAY_DESK -> {
                val intradayDeskLimits = connector.dealingLimits.filter { it.limitType == type.fullName }
                logger.info("intraday desk limits $intradayDeskLimits")
                return checkDeskLimits(intradayDeskLimits, fxRate, metalPrices, connector.closingPositions)
            }
            OVERNIGHT_DESK -> {
                val overnightDeskLimits = connector.dealingLimits.filter { it.limitType == type.fullName }
                logger.info("overnight desk limits $overnightDeskLimits")
                val positions = HashBasedTable.create(connector.closingPositions)
                val metals = positions.columnKeySet().toSet()
                metals.forEach {
                    positions.put(
                        "JM PMM One Book",
                        it,
                        (positions.get("JM PMM UK", it) ?: 0.0) +
                                (positions.get("JM PMM US", it) ?: 0.0) +
                                (positions.get("JM PMM HK", it) ?: 0.0)
                    )
                }
                logger.info("overnight positions $positions")
                return checkDeskLimits(overnightDeskLimits, fxRate, metalPrices, positions)
            }
        }
    }

    private fun checkDeskLimits(
        deskLimits: List<DealingLimit>,
        fxRate: Double,
        metalPrices: Map<String, Double>,
        positions: Table<String, String, Double>
    ): List<RunResult> {
        return deskLimits.filter { positions.contains(it.desk, it.metal) }.map {
            val position = positions.get(it.desk, it.metal)
            val breach = position ?: -1.0 > it.limit
            val breachTOz = max(position - it.limit, 0.0)
            RunResult(
                runTime = connector.runDate,
                runType = it.limitType,
                desk = it.desk,
                metal = it.metal,
                positionLimit = it.limit,
                breach = breach,
                currentPosition = position,
                breachTOz = breachTOz,
                breachGBP = breachTOz * metalPrices.getValue(it.metal) / fxRate,
                breachDates = RunResult.getBreachDates(
                    connector,
                    breach,
                    runType = it.limitType,
                    desk = it.desk,
                    metal = it.metal
                )
            )
        }
    }

    private fun sumForAllMetals(positions: Map<String, Double>, metalPrices: Map<String, Double>): Double {
        return positions.map { (metal, position) -> position * (metalPrices[metal] ?: error("no price for $metal")) }
            .sum()
    }
}