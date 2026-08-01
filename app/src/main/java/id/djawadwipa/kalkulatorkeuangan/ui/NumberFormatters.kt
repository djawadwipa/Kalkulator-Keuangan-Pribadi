package id.djawadwipa.kalkulatorkeuangan.ui

import java.text.NumberFormat
import java.util.Locale

internal fun reportDecimal(value: Double): String = NumberFormat.getNumberInstance(
    Locale("id", "ID"),
).apply {
    maximumFractionDigits = 1
    minimumFractionDigits = 0
}.format(value)
