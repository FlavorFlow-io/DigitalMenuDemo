package dev.lssoftware.digitalmenu.presentation.ui

import java.util.Locale

private val BRL = Locale.forLanguageTag("pt-BR")

/** Formats a numeric price as Brazilian currency, e.g. `R$ 12,00`. */
fun formatPrice(value: Double): String = "R$ %,.2f".format(BRL, value)
