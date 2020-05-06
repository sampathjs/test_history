package com.matthey.pmm.limits.reporting

import com.google.common.collect.Table
import java.text.NumberFormat

class MetalBalances(private val rawData: Table<String, String, String>) {

    companion object {
        val metalNames = mapOf(
            "XPT" to "Platinum",
            "XPD" to "Palladium",
            "XRH" to "Rhodium",
            "XAU" to "Gold",
            "XAG" to "Silver",
            "XIR" to "Iridium",
            "XOS" to "Osmium",
            "XRU" to "Ruthenium"
        )
    }

    fun getBalance(lineTitle: String, metal: String): Int {
        val metalName = metalNames.getValue(metal)
        return NumberFormat.getInstance().parse(rawData.get(lineTitle, "$metalName\\nActual")).toInt()
    }
}