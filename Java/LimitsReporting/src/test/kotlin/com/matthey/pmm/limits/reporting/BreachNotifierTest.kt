package com.matthey.pmm.limits.reporting

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class BreachNotifierTest {

    @RelaxedMockK
    lateinit var connector: LimitsReportingConnector

    @RelaxedMockK
    lateinit var emailSender: EmailSender

    private fun `do not send alert if result is not breach`(method: (sut: BreachNotifier) -> Unit) {
        every { connector.getEmails(any()) } returns setOf("tester@test.com")
        val sut = BreachNotifier(connector, emailSender)
        method(sut)
        verify(exactly = 0) { emailSender.send(any(), any(), any()) }
    }

    @Test
    fun `do not send liquidity alert if result is not breach`() {
        `do not send alert if result is not breach` {
            it.sendLiquidityAlert(
                listOf(
                    RunResult(runType = "Liquidity", runTime = LocalDateTime.now()),
                    RunResult(runType = "Liquidity", runTime = LocalDateTime.now())
                )
            )
        }
    }

    @Test
    fun `do not send overnight alert if result is not breach`() {
        `do not send alert if result is not breach` {
            it.sendOvernightAlert(
                RunResult(
                    runType = "Overnight",
                    runTime = LocalDateTime.now()
                )
            )
        }
    }

    @Test
    fun `do not send lease alert if result is not breach`() {
        `do not send alert if result is not breach` {
            it.sendLeaseAlert(
                RunResult(
                    runType = "Lease",
                    runTime = LocalDateTime.now()
                )
            )
        }
    }

    private fun `do not send alert if no email address available`(method: (sut: BreachNotifier) -> Unit) {
        every { connector.getEmails(any()) } returns setOf()
        val sut = BreachNotifier(connector, emailSender)
        method(sut)
        verify(exactly = 0) { emailSender.send(any(), any(), any()) }
    }

    @Test
    fun `do not send liquidity alert if no email address available`() {
        `do not send alert if no email address available` {
            it.sendLiquidityAlert(
                listOf(
                    RunResult(runType = "Liquidity", breach = true, runTime = LocalDateTime.now()),
                    RunResult(runType = "Liquidity", breach = true, runTime = LocalDateTime.now())
                )
            )
        }
    }

    @Test
    fun `do not send overnight alert if no email address available`() {
        `do not send alert if no email address available` {
            it.sendOvernightAlert(
                RunResult(
                    runType = "Overnight",
                    breach = true,
                    runTime = LocalDateTime.now()
                )
            )
        }
    }

    @Test
    fun `do not send lease alert if no email address available`() {
        `do not send alert if no email address available` {
            it.sendLeaseAlert(
                RunResult(
                    runType = "Lease",
                    breach = true,
                    runTime = LocalDateTime.now()
                )
            )
        }
    }

    private fun `throw exception if wrong type of result is passed for alert`(method: (sut: BreachNotifier) -> Unit) {
        assertThatThrownBy {
            method(
                BreachNotifier(
                    connector,
                    emailSender
                )
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throw exception if wrong type of result is passed for liquidity alert`() {
        `throw exception if wrong type of result is passed for alert` {
            it.sendLiquidityAlert(
                listOf(
                    RunResult(
                        runType = "Invalid",
                        runTime = LocalDateTime.now()
                    )
                )
            )
        }
    }

    @Test
    fun `throw exception if wrong type of result is passed for overnight alert`() {
        `throw exception if wrong type of result is passed for alert` {
            it.sendOvernightAlert(
                RunResult(
                    runType = "Invalid",
                    runTime = LocalDateTime.now()
                )
            )
        }
    }

    @Test
    fun `throw exception if wrong type of result is passed for lease alert`() {
        `throw exception if wrong type of result is passed for alert` {
            it.sendLeaseAlert(
                RunResult(
                    runType = "Invalid",
                    runTime = LocalDateTime.now()
                )
            )
        }
    }

    private fun `email is sent with correct parameters`(
        expectedTitle: String,
        expectedContentName: String,
        method: (sut: BreachNotifier) -> Unit
    ) {
        val title = slot<String>()
        val content = slot<String>()
        val emails = slot<Set<String>>()

        every { connector.runDate } returns LocalDateTime(2020, 3, 8, 0, 0, 0)
        every { connector.getEmails(any()) } returns setOf("tester1@test.com", "tester2@test.com")
        every { emailSender.send(capture(title), capture(content), capture(emails)) } answers {}
        DateTimeUtils.setCurrentMillisFixed(LocalDateTime(2020, 3, 8, 11, 11, 11).toDateTime(DateTimeZone.UTC).millis)

        method(BreachNotifier(connector, emailSender))

        assertThat(title.captured).isEqualTo(expectedTitle)
        assertThat(emails.captured).containsExactly("tester1@test.com", "tester2@test.com")
        assertThat(content.captured).isEqualToNormalizingWhitespace(getExpectedContent("$expectedContentName.html"))
    }

    private fun getExpectedContent(name: String): String? {
        return javaClass.classLoader.getResource(name)?.readText()
    }

    @Test
    fun `correct html is generated for lease alert`() {
        `email is sent with correct parameters`("Lease Breach 08-03-20", "lease-breach") {
            it.sendLeaseAlert(
                RunResult(
                    runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                    runType = "Lease",
                    breach = true,
                    breachGBP = 1234567.891111,
                    breachDates = listOf("06-03-20", "07-03-20", "08-03-20")
                )
            )
        }
    }

    @Test
    fun `correct html is generated for overnight alert`() {
        `email is sent with correct parameters`("Overnight Breach 08-03-20", "overnight-breach") {
            it.sendOvernightAlert(
                RunResult(
                    runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                    runType = "Overnight",
                    breach = true,
                    breachGBP = 1234567.891111,
                    breachDates = listOf("06-03-20", "07-03-20", "08-03-20")
                )
            )
        }
    }

    @Test
    fun `correct html is generated for overnight desk alert`() {
        `email is sent with correct parameters`("Overnight Desk Breaches 08-03-20", "overnight-desk-breach") {
            it.sendDeskAlert(
                listOf(
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Overnight Desk",
                        desk = "JM PMM US",
                        metal = "XPD",
                        breach = true,
                        breachTOz = 111111.0,
                        breachDates = listOf("06-03-20", "07-03-20", "08-03-20")
                    ),
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Overnight Desk",
                        desk = "JM PMM UK",
                        metal = "XPD",
                        breach = true,
                        breachTOz = 111111.0,
                        breachDates = listOf("06-03-20", "07-03-20", "08-03-20")
                    ),
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Overnight Desk",
                        desk = "JM PMM UK",
                        metal = "XPT",
                        breach = true,
                        breachTOz = 111111.0,
                        breachDates = listOf("06-03-20", "07-03-20", "08-03-20")
                    )
                )
            )
        }
    }

    @Test
    fun `correct html is generated for intraday desk alert`() {
        `email is sent with correct parameters`("Intraday Desk Breaches 08-03-20", "intraday-desk-breach") {
            it.sendDeskAlert(
                listOf(
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 1, 1, 1),
                        runType = "Intraday Desk",
                        desk = "JM PMM US",
                        metal = "XPD",
                        breach = true,
                        breachTOz = 111111.0,
                        breachDates = listOf("06-03-20", "07-03-20", "08-03-20")
                    ),
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 2, 2, 2),
                        runType = "Intraday Desk",
                        desk = "JM PMM UK",
                        metal = "XPD",
                        breach = true,
                        breachTOz = 111111.0,
                        breachDates = listOf("06-03-20", "07-03-20", "08-03-20")
                    ),
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Intraday Desk",
                        desk = "JM PMM UK",
                        metal = "XPT",
                        breach = true,
                        breachTOz = 111111.0,
                        breachDates = listOf("06-03-20", "07-03-20", "08-03-20")
                    )
                )
            )
        }
    }

    @Test
    fun `correct html is generated for liquidity alert without highlights`() {
        `email is sent with correct parameters`(
            "Liquidity Breaches 08-03-20",
            "liquidity-breach-without-highlights"
        ) {
            it.sendLiquidityAlert(
                listOf(
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Liquidity",
                        breach = true,
                        liquidityBreachLimit = "Upper Limit",
                        breachDates = listOf("06-03-20", "07-03-20", "08-03-20"),
                        critical = false,
                        metal = "XPD",
                        breachGBP = 100000000.00,
                        breachTOz = 2000000.00,
                        liquidityDiff = 300,
                        currentPosition = 200.00
                    ),
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Liquidity",
                        breach = true,
                        liquidityBreachLimit = "Upper Limit",
                        breachDates = listOf("08-03-20"),
                        critical = false,
                        metal = "XPT",
                        breachGBP = 100000000.00,
                        breachTOz = 2000000.0,
                        liquidityDiff = 300,
                        currentPosition = 200.0
                    )
                )
            )
        }
    }

    @Test
    fun `correct html is generated for liquidity alert that contains both breach and non-breach`() {
        `email is sent with correct parameters`(
            "Liquidity Breaches 08-03-20",
            "liquidity-breach-partial"
        ) {
            it.sendLiquidityAlert(
                listOf(
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Liquidity",
                        breach = false,
                        liquidityBreachLimit = "Upper Limit",
                        breachDates = listOf("06-03-20", "07-03-20", "08-03-20"),
                        critical = false,
                        metal = "XPD",
                        breachGBP = 100000000.00,
                        breachTOz = 2000000.0,
                        liquidityDiff = 300,
                        currentPosition = 200.0
                    ),
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Liquidity",
                        breach = true,
                        liquidityBreachLimit = "Upper Limit",
                        breachDates = listOf("08-03-20"),
                        critical = false,
                        metal = "XPT",
                        breachGBP = 100000000.00,
                        breachTOz = 2000000.0,
                        liquidityDiff = 300,
                        currentPosition = 200.0
                    )
                )
            )
        }
    }

    @Test
    fun `correct html is generated for liquidity alert with highlights`() {
        `email is sent with correct parameters`("Liquidity Breaches 08-03-20", "liquidity-breach-with-highlights") {
            it.sendLiquidityAlert(
                listOf(
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Liquidity",
                        breach = true,
                        liquidityBreachLimit = "Upper Limit",
                        breachDates = listOf(
                            "27-02-20",
                            "28-02-20",
                            "29-02-20",
                            "01-03-20",
                            "02-03-20",
                            "03-03-20",
                            "04-03-20",
                            "05-03-20",
                            "06-03-20",
                            "07-03-20",
                            "08-03-20"
                        ),
                        critical = false,
                        metal = "XPD",
                        breachGBP = 100000000.00,
                        breachTOz = 2000000.0,
                        liquidityDiff = 300,
                        currentPosition = 200.0
                    ),
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Liquidity",
                        breach = true,
                        liquidityBreachLimit = "Upper Limit",
                        breachDates = listOf("08-03-20"),
                        critical = true,
                        metal = "XPT",
                        breachGBP = 100000000.00,
                        breachTOz = 2000000.0,
                        liquidityDiff = 300,
                        currentPosition = 200.0
                    )
                )
            )
        }
    }

    @Test
    fun `correct html is generated for breach weekly summary`() {
        `email is sent with correct parameters`(
            "Breach Weekly Summary 08-03-20",
            "breach-weekly-summary"
        ) {
            it.sendAlertSummary(
                listOf(
                    RunResult(
                        runType = "Liquidity",
                        liquidityBreachLimit = "Upper Limit",
                        metal = "XPD",
                        runTime = LocalDateTime(2020, 3, 3, 0, 0, 0)
                    ),
                    RunResult(
                        runType = "Liquidity",
                        liquidityBreachLimit = "Upper Limit",
                        metal = "XPT",
                        runTime = LocalDateTime(2020, 3, 3, 0, 0, 0)
                    ),
                    RunResult(
                        liquidityBreachLimit = "Lease",
                        metal = "",
                        runTime = LocalDateTime(2020, 3, 1, 0, 0, 0),
                        runType = "Lease"
                    ),
                    RunResult(
                        liquidityBreachLimit = "Overnight",
                        metal = "",
                        runTime = LocalDateTime(2020, 3, 1, 0, 0, 0),
                        runType = "Overnight"
                    ),
                    RunResult(
                        liquidityBreachLimit = "Overnight",
                        metal = "",
                        runTime = LocalDateTime(2020, 3, 2, 0, 0, 0),
                        runType = "Overnight"
                    ),
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Overnight Desk",
                        desk = "JM PMM US",
                        metal = "XPD",
                        breach = true,
                        breachTOz = 111111.0,
                        breachDates = listOf("06-03-20", "07-03-20", "08-03-20")
                    ),
                    RunResult(
                        runTime = LocalDateTime(2020, 3, 8, 0, 0, 0),
                        runType = "Intraday Desk",
                        desk = "JM PMM UK",
                        metal = "XPD",
                        breach = true,
                        breachTOz = 111111.0,
                        breachDates = listOf("06-03-20", "07-03-20", "08-03-20")
                    )
                )
            )
        }
    }
}