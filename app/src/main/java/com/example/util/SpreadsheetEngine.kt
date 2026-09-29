package com.example.util

import java.text.DecimalFormat

data class CellCoord(val row: Int, val col: Int) {
    fun toRef(): String {
        val colLetter = ('A' + col).toString()
        return "$colLetter${row + 1}"
    }

    companion object {
        fun fromRef(ref: String): CellCoord? {
            val trimmed = ref.trim().uppercase()
            if (trimmed.isEmpty()) return null
            val colChar = trimmed[0]
            if (colChar !in 'A'..'Z') return null
            val col = colChar - 'A'
            val rowStr = trimmed.substring(1)
            val row = (rowStr.toIntOrNull() ?: 1) - 1
            return CellCoord(row, col)
        }
    }
}

class SpreadsheetEngine {

    companion object {
        const val DEFAULT_ROWS = 25
        const val DEFAULT_COLS = 8

        fun columnHeader(col: Int): String {
            return ('A' + col).toString()
        }

        fun parseCsv(csv: String): Map<CellCoord, String> {
            val grid = mutableMapOf<CellCoord, String>()
            val lines = csv.split("\n")
            lines.forEachIndexed { rowIndex, line ->
                val cols = line.split(",")
                cols.forEachIndexed { colIndex, cellVal ->
                    val clean = cellVal.trim().removeSurrounding("\"")
                    if (clean.isNotEmpty()) {
                        grid[CellCoord(rowIndex, colIndex)] = clean
                    }
                }
            }
            return grid
        }

        fun toCsv(grid: Map<CellCoord, String>, maxRow: Int = 20, maxCol: Int = 8): String {
            val sb = StringBuilder()
            for (r in 0 until maxRow) {
                val rowCells = mutableListOf<String>()
                for (c in 0 until maxCol) {
                    val raw = grid[CellCoord(r, c)] ?: ""
                    // escape commas
                    if (raw.contains(",")) {
                        rowCells.add("\"$raw\"")
                    } else {
                        rowCells.add(raw)
                    }
                }
                sb.append(rowCells.joinToString(",")).append("\n")
            }
            return sb.toString().trimEnd()
        }

        fun evaluateGrid(grid: Map<CellCoord, String>): Map<CellCoord, String> {
            val evaluated = mutableMapOf<CellCoord, String>()
            grid.forEach { (coord, value) ->
                evaluated[coord] = evaluateCell(value, grid)
            }
            return evaluated
        }

        private val decimalFormat = DecimalFormat("#,##0.##")

        fun evaluateCell(value: String, grid: Map<CellCoord, String>): String {
            val trimmed = value.trim()
            if (!trimmed.startsWith("=")) return trimmed

            val formula = trimmed.substring(1).trim().uppercase()

            return try {
                when {
                    formula.startsWith("SUM(") && formula.endsWith(")") -> {
                        val rangeStr = formula.removePrefix("SUM(").removeSuffix(")")
                        val numbers = extractNumbersFromRange(rangeStr, grid)
                        val sum = numbers.sum()
                        decimalFormat.format(sum)
                    }
                    (formula.startsWith("AVERAGE(") && formula.endsWith(")")) ||
                    (formula.startsWith("AVG(") && formula.endsWith(")")) -> {
                        val rangeStr = if (formula.startsWith("AVERAGE(")) {
                            formula.removePrefix("AVERAGE(").removeSuffix(")")
                        } else {
                            formula.removePrefix("AVG(").removeSuffix(")")
                        }
                        val numbers = extractNumbersFromRange(rangeStr, grid)
                        if (numbers.isEmpty()) "0" else decimalFormat.format(numbers.average())
                    }
                    formula.startsWith("COUNT(") && formula.endsWith(")") -> {
                        val rangeStr = formula.removePrefix("COUNT(").removeSuffix(")")
                        val numbers = extractNumbersFromRange(rangeStr, grid)
                        numbers.size.toString()
                    }
                    formula.startsWith("MIN(") && formula.endsWith(")") -> {
                        val rangeStr = formula.removePrefix("MIN(").removeSuffix(")")
                        val numbers = extractNumbersFromRange(rangeStr, grid)
                        val min = numbers.minOrNull() ?: 0.0
                        decimalFormat.format(min)
                    }
                    formula.startsWith("MAX(") && formula.endsWith(")") -> {
                        val rangeStr = formula.removePrefix("MAX(").removeSuffix(")")
                        val numbers = extractNumbersFromRange(rangeStr, grid)
                        val max = numbers.maxOrNull() ?: 0.0
                        decimalFormat.format(max)
                    }
                    formula.contains("+") -> {
                        val parts = formula.split("+")
                        val sum = parts.sumOf { resolveValue(it.trim(), grid) }
                        decimalFormat.format(sum)
                    }
                    formula.contains("-") -> {
                        val parts = formula.split("-")
                        if (parts.size == 2) {
                            val diff = resolveValue(parts[0].trim(), grid) - resolveValue(parts[1].trim(), grid)
                            decimalFormat.format(diff)
                        } else {
                            formula
                        }
                    }
                    formula.contains("*") -> {
                        val parts = formula.split("*")
                        val prod = parts.fold(1.0) { acc, p -> acc * resolveValue(p.trim(), grid) }
                        decimalFormat.format(prod)
                    }
                    else -> {
                        val singleCoord = CellCoord.fromRef(formula)
                        if (singleCoord != null) {
                            grid[singleCoord] ?: ""
                        } else {
                            formula
                        }
                    }
                }
            } catch (_: Exception) {
                "#ERROR"
            }
        }

        private fun resolveValue(token: String, grid: Map<CellCoord, String>): Double {
            val coord = CellCoord.fromRef(token)
            if (coord != null) {
                val raw = grid[coord] ?: "0"
                val cleaned = raw.replace("$", "").replace(",", "").replace("%", "").trim()
                return cleaned.toDoubleOrNull() ?: 0.0
            }
            return token.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
        }

        private fun extractNumbersFromRange(rangeStr: String, grid: Map<CellCoord, String>): List<Double> {
            val numbers = mutableListOf<Double>()
            if (rangeStr.contains(":")) {
                val parts = rangeStr.split(":")
                val start = CellCoord.fromRef(parts[0])
                val end = CellCoord.fromRef(parts[1])
                if (start != null && end != null) {
                    val minRow = minOf(start.row, end.row)
                    val maxRow = maxOf(start.row, end.row)
                    val minCol = minOf(start.col, end.col)
                    val maxCol = maxOf(start.col, end.col)

                    for (r in minRow..maxRow) {
                        for (c in minCol..maxCol) {
                            val v = grid[CellCoord(r, c)] ?: continue
                            val clean = v.replace("$", "").replace(",", "").replace("%", "").trim()
                            clean.toDoubleOrNull()?.let { numbers.add(it) }
                        }
                    }
                }
            } else {
                // Comma separated e.g. A1,B2
                rangeStr.split(",").forEach {
                    val d = resolveValue(it.trim(), grid)
                    numbers.add(d)
                }
            }
            return numbers
        }
    }
}
