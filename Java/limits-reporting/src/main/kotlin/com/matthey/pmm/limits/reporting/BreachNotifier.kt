package com.matthey.pmm.limits.reporting

import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import org.joda.time.LocalDateTime
import org.slf4j.LoggerFactory
import java.io.StringWriter


class BreachNotifier(private val connector: LimitsReportingConnector, private val emailSender: EmailSender) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val runDate = RunResult.dateFormat.print(connector.runDate)
    private val freemarkerConfig = Configuration(Configuration.VERSION_2_3_30)

    init {
        freemarkerConfig.setClassForTemplateLoading(javaClass, "/email-templates")
        freemarkerConfig.defaultEncoding = "UTF-8"
        freemarkerConfig.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        freemarkerConfig.logTemplateExceptions = false
        freemarkerConfig.wrapUncheckedExceptions = true
        freemarkerConfig.fallbackOnNullLoopVariable = false
    }

    fun sendOvernightAlert(result: RunResult) {
        require(result.runType == "Overnight") { "the run type of the result should be overnight" }
        if (!result.breach) return
        send(
            functionalGroup = "Dealing",
            title = "Overnight Breach",
            parameters = mapOf("runDate" to runDate, "result" to result)
        )
    }

    fun sendDeskAlert(results: List<RunResult>) {
        if (!results.any { it.breach }) return
        val runType = results.map { it.runType }.distinct().single()
        require(runType.endsWith("Desk")) { "the run type of the result should end with Desk" }
        send(
            functionalGroup = "Dealing",
            title = "$runType Breaches",
            parameters = mapOf("runDate" to runDate, "results" to results)
        )
    }

    fun sendLeaseAlert(result: RunResult) {
        require(result.runType == "Lease") { "the run type of the result should be lease" }
        if (!result.breach) return
        send(
            functionalGroup = "Lease",
            title = "Lease Breach",
            parameters = mapOf("runDate" to runDate, "result" to result)
        )
    }

    fun sendLiquidityAlert(results: List<RunResult>) {
        require(results.all { it.runType == "Liquidity" }) { "the run type of the result should be liquidity" }
        if (!results.any { it.breach }) return
        send(
            functionalGroup = "Liquidity",
            title = "Liquidity Breaches",
            parameters = mapOf("runDate" to runDate, "results" to results)
        )
    }

    fun sendAlertSummary(allBreaches: List<RunResult>) {
        if (allBreaches.isEmpty()) return
        send(
            functionalGroup = "Summary",
            title = "Breach Weekly Summary",
            parameters = mapOf("breachGroups" to groupBreaches(allBreaches))
        )
    }

    private fun groupBreaches(allBreaches: List<RunResult>): List<BreachGroup> {
        return allBreaches.groupBy {
            getBreachType(it)
        }.map { (group, breaches) ->
            BreachGroup(
                breachType = group,
                breaches = breaches.sortedBy { it.runTime }
            )
        }.sortedBy { it.breachType }
    }

    private fun getBreachType(result: RunResult): String {
        val parts = mutableListOf(result.runType)
        if (result.runType == "Liquidity") {
            parts.add(result.liquidityBreachLimit)
        }
        if (result.runType.endsWith("Desk")) {
            parts.add(result.desk)
        }
        if (result.metal.isNotBlank()) {
            parts.add(result.metal)
        }
        return parts.joinToString(" ")
    }

    private fun send(functionalGroup: String, title: String, parameters: Map<String, Any>) {
        val emails = connector.getEmails(functionalGroup)
        logger.info("email addresses to be sent for ${functionalGroup}: $emails")
        if (emails.isEmpty()) return

        val template = freemarkerConfig.getTemplate("${title.toLowerCase().replace(' ', '-')}.ftl")
        val content = StringWriter()
        template.process(
            parameters + ("currentDate" to LocalDateTime.now()) + ("dateSeparator" to RunResult.DATE_SEPARATOR) + ("datePattern" to RunResult.DATE_PATTERN),
            content
        )

        emailSender.send("$title $runDate", content.toString(), emails)
    }
}