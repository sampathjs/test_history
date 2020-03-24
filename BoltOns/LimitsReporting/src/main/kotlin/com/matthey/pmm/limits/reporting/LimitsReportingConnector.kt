package com.matthey.pmm.limits.reporting

import com.google.common.collect.HashBasedTable
import com.google.common.collect.ImmutableTable
import com.google.common.collect.Table
import com.olf.embedded.application.Context
import com.olf.openjvs.ReportBuilder
import com.olf.openrisk.staticdata.Currency
import com.olf.openrisk.staticdata.EnumReferenceObject
import com.olf.openrisk.table.TableRow
import com.olf.openrisk.trading.EnumTranfField
import org.intellij.lang.annotations.Language
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import org.slf4j.LoggerFactory
import java.text.NumberFormat

typealias OCTable = com.olf.openrisk.table.Table
typealias JVSTable = com.olf.openjvs.Table

class LimitsReportingConnector(private val context: Context) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val leaseLimit: Int by lazy {
        context.ioFactory.getUserTable("USER_limits_reporting_lease").retrieveTable().getInt("value", 0)
    }

    val dealingLimits: List<DealingLimit> by lazy {
        context.ioFactory.getUserTable("USER_limits_reporting_dealing").retrieveTable().rows.map {
            DealingLimit(
                limitType = it.getString("limit_type"),
                desk = it.getString("desk"),
                metal = it.getString("metal"),
                limit = it.getInt("limit")
            )
        }
    }

    val liquidityLimits: List<LiquidityLimits> by lazy {
        context.ioFactory.getUserTable("USER_limits_reporting_liquidity").retrieveTable().rows.map {
            LiquidityLimits(
                metal = it.getString("metal"),
                lowerLimit = it.getInt("lower_limit"),
                upperLimit = it.getInt("upper_limit"),
                maxLiability = it.getInt("max_liability")
            )
        }
    }

    val runDate: LocalDateTime by lazy {
        LocalDateTime.fromDateFields(context.tradingDate)
    }

    fun getGbpUsdRate(date: LocalDateTime = runDate): Double {
        val staticDataFactory = context.staticDataFactory
        val gbp = staticDataFactory.getReferenceObject(EnumReferenceObject.Currency, "GBP") as Currency
        val usd = staticDataFactory.getReferenceObject(EnumReferenceObject.Currency, "USD") as Currency
        val market = context.market
        try {
            market.loadClose(date.toDate())
        } catch (_: Exception) {
            // if there is no closing dataset for the day, use the default dataset
        }
        return market.getFXSpotRate(gbp, usd, date.toDate())
    }

    val metalPrices: Map<String, Double> by lazy {
        @Language("TSQL") val sql = """
            SELECT substring(id.index_name, 1, 3) AS metal,
                   ihp.price
                FROM idx_historical_prices ihp
                         INNER JOIN (SELECT MAX(last_update) AS last_update, index_id
                                         FROM idx_historical_prices
                                         GROUP BY index_id, ref_source) AS latest_prices
                                    ON ihp.index_id = latest_prices.index_id AND ihp.last_update = latest_prices.last_update
                         JOIN ref_source rs
                              ON ihp.ref_source = rs.id_number
                         JOIN idx_def id
                              ON ihp.index_id = id.index_version_id
                WHERE (id.index_name = 'XAG.USD' AND rs.name = 'LBMA Silver')
                   OR (id.index_name = 'XAU.USD' AND rs.name = 'LBMA PM')
                   OR (id.index_name IN ('XOS.USD', 'XRU.USD', 'XPD.USD', 'XPT.USD', 'XIR.USD', 'XRH.USD') AND
                       rs.name = 'JM NY Opening')
            """.trimIndent()
        context.ioFactory.runSQL(sql).rows.associateBy({ it.getString("metal") }, { it.getDouble("price") })
    }

    val metalBalances: MetalBalances by lazy {
        val result: Table<String, String, String> = HashBasedTable.create()
        val rawTable = runReport(context, "Metals Balance Sheet - Combined", "Output_01")
        val tableFormatter = rawTable.formatter
        for (row in rawTable.rows) {
            val line = row.getCell(0).displayString
            for (idx in 0 until rawTable.columnCount) {
                result.put(line, tableFormatter.getColumnTitle(idx), row.getCell(idx).displayString)
            }
        }
        MetalBalances(result)
    }

    val balanceLines: List<BalanceLine> by lazy {
        context.ioFactory.getUserTable("USER_limits_reporting_balance")
            .retrieveTable().rows.map { BalanceLine(it.getString("balance_line"), it.getString("purpose")) }
    }

    val closingPositions: Table<String, String, Double> by lazy {
        val resultBuilder = ImmutableTable.builder<String, String, Double>()
        val rawTable = runReport(context, "PMM Closing Position by Metal and BU", "TableOutput")
        rawTable.rows.forEach {
            resultBuilder.put(
                it.getString("bunit"),
                it.getString("metal_ccy"),
                NumberFormat.getInstance().parse(it.getString("closing_volume")).toDouble()
            )
        }
        resultBuilder.build()
    }

    val unhedgedAndRefiningGainsPositions: Map<String, Double> by lazy {
        @Language("TSQL") val sql = """
            SELECT c.name AS metal, sum(settle_amount) AS position
                FROM ab_tran_event_settle abes
                         JOIN ab_tran_event abe
                              ON abes.event_num = abe.event_num
                         JOIN ab_tran ab
                              ON abe.tran_num = ab.tran_num
                         JOIN trans_status ts
                              ON ab.tran_status = ts.trans_status_id
                         JOIN account a
                              ON abes.int_account_id = a.account_id
                         JOIN USER_limits_reporting_account ra
                              ON a.account_name = ra.account_name
                         JOIN currency c
                              ON abes.currency_id = c.id_number
                WHERE ts.name IN ('Validated', 'Matured')
                GROUP BY c.name
        """.trimIndent()
        context.ioFactory.runSQL(sql).rows.associateBy({ it.getString("metal") }, { it.getDouble("position") })
    }

    val leaseDeals: List<LeaseDeal> by lazy {
        val tradingFactory = context.tradingFactory
        tradingFactory.retrieveTransactions(tradingFactory.queries.getQuery("Limits Reporting - Lease Deals")).map {
            LeaseDeal(
                tranNum = it.retrieveField(EnumTranfField.TranNum, 0).valueAsString,
                startDate = LocalDateTime.fromDateFields(it.retrieveField(EnumTranfField.StartDate, 0).valueAsDate),
                Notnl = it.retrieveField(EnumTranfField.Notnl, 0).valueAsDouble,
                currencyFxRate = it.retrieveField(EnumTranfField.CcyConvRate).valueAsDouble
            )
        }
    }

    fun getPreviousBreachDates(
        runType: String,
        runDate: LocalDateTime,
        liquidityBreachLimit: String,
        desk: String,
        metal: String
    ): String? {
        @Language("TSQL") val sql = """
            SELECT breach_dates
                FROM USER_limits_reporting_result r1
                         JOIN (SELECT max(update_time) AS latest_update_time, metal
                                   FROM USER_limits_reporting_result
                                   WHERE run_date < '${ISODateTimeFormat.date().print(runDate)}'
                                   GROUP BY run_type, metal) r2
                              ON r1.metal = r2.metal AND r1.update_time = r2.latest_update_time
                WHERE run_type = '$runType'
                  AND liquidity_breach_limit = '$liquidityBreachLimit'
                  AND desk = '$desk'
                  AND r1.metal = '$metal'
        """.trimIndent()
        logger.debug("getPreviousBreachDates SQL: $sql")
        val table = context.ioFactory.runSQL(sql)
        return if (table.rowCount > 0) table.getString(0, 0) else null
    }

    val intradayBreaches: List<RunResult> by lazy {
        @Language("TSQL") val sql = """
            SELECT *
                FROM USER_limits_reporting_result
                WHERE run_type = 'Intraday Desk'
                  AND breach = 1            
        """.trimIndent()
        context.ioFactory.runSQL(sql).rows.map { fromResultTableRow(it, true) }
    }

    val eodBreaches: List<RunResult> by lazy {
        @Language("TSQL") val sql = """
            SELECT r1.*
                FROM USER_limits_reporting_result r1
                         JOIN (SELECT max(update_time) AS latest_update_time,
                                      run_date,
                                      run_type,
                                      desk,
                                      metal
                                   FROM USER_limits_reporting_result
                                   WHERE breach = 1 
                                     AND run_type <> 'Intraday Desk'
                                   GROUP BY run_date, run_type, desk, metal) r2
                              ON r1.run_date = r2.run_date AND r1.run_type = r2.run_type AND r1.desk = r2.desk AND
                                 r1.metal = r2.metal AND r1.update_time = r2.latest_update_time
        """.trimIndent()
        logger.debug("breaches SQL: $sql")
        context.ioFactory.runSQL(sql).rows.map { fromResultTableRow(it, false) }
    }

    private fun fromResultTableRow(row: TableRow, needRunTime: Boolean): RunResult {
        return RunResult(
            runTime = if (needRunTime) LocalDateTime.parse(
                row.getString("update_time") + "Z",
                ISODateTimeFormat.dateTime()
            ) else LocalDateTime.fromDateFields(row.getDate("run_date")),
            runType = row.getString("run_type"),
            desk = row.getString("desk"),
            metal = row.getString("metal"),
            liquidityLowerLimit = row.getInt("liquidity_lower_limit"),
            liquidityUpperLimit = row.getInt("liquidity_upper_limit"),
            liquidityMaxLiability = row.getInt("liquidity_max_liability"),
            positionLimit = row.getInt("position_limit"),
            breach = row.getInt("breach") > 0,
            liquidityBreachLimit = row.getString("liquidity_breach_limit"),
            currentPosition = row.getDouble("current_position"),
            liquidityDiff = row.getInt("liquidity_diff"),
            breachTOz = row.getDouble("breach_toz"),
            breachGBP = row.getDouble("breach_gbp"),
            critical = row.getInt("critical") > 0,
            breachDates = row.getString("breach_dates").split(RunResult.DATE_SEPARATOR)
        )
    }

    fun saveRunResult(result: RunResult) {
        val userTable = context.ioFactory.getUserTable("USER_limits_reporting_result")
        val newRows = userTable.retrieveTable().cloneStructure()
        val newRow = newRows.addRow()
        newRow.getCell("run_date").date = runDate.toDate()
        newRow.getCell("run_type").string = result.runType
        newRow.getCell("desk").string = result.desk
        newRow.getCell("metal").string = result.metal
        newRow.getCell("liquidity_lower_limit").int = result.liquidityLowerLimit
        newRow.getCell("liquidity_upper_limit").int = result.liquidityUpperLimit
        newRow.getCell("liquidity_max_liability").int = result.liquidityMaxLiability
        newRow.getCell("position_limit").int = result.positionLimit
        newRow.getCell("breach").int = if (result.breach) 1 else 0
        newRow.getCell("liquidity_breach_limit").string = result.liquidityBreachLimit
        newRow.getCell("current_position").double = result.currentPosition
        newRow.getCell("liquidity_diff").int = result.liquidityDiff
        newRow.getCell("breach_toz").double = result.breachTOz
        newRow.getCell("breach_gbp").double = result.breachGBP
        newRow.getCell("critical").int = if (result.critical) 1 else 0
        newRow.getCell("breach_dates").string = result.breachDates.joinToString(RunResult.DATE_SEPARATOR)
        newRow.getCell("update_time").string = ISODateTimeFormat.dateTime().print(LocalDateTime.now())

        userTable.insertRows(newRows)
    }

    fun getEmails(runType: String): Set<String> {
        @Language("TSQL") val sql = """
                SELECT DISTINCT(p.email)
                    FROM personnel p
                             JOIN personnel_functional_group pf
                                  ON p.id_number = pf.personnel_id
                             JOIN functional_group f
                                  ON f.id_number = pf.func_group_id
                    WHERE f.name = 'EOD Limits Reporting - $runType'                    
        """.trimIndent()
        return context.ioFactory.runSQL(sql).rows.map { row -> row.getString("email") }.toSet()
    }

    private fun runReport(context: Context, name: String, reportOutput: String): OCTable {
        val output = JVSTable.tableNew()
        val reportBuilder = ReportBuilder.createNew(name)
        reportBuilder.setOutputTable(output)
        reportBuilder.runReportOutput(reportOutput)
        return context.tableFactory.fromOpenJvs(output)
    }
}