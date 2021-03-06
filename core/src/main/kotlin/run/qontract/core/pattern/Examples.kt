package run.qontract.core.pattern

import io.cucumber.messages.Messages
import io.cucumber.messages.Messages.GherkinDocument.Feature.Scenario.Examples
import java.util.*

data class Examples(val columnNames: List<String> = emptyList(), val rows: List<Row> = listOf()) {
    val isEmpty: Boolean
        get() = rows.isEmpty()

    companion object {
        fun examplesFrom(examplesList: List<Examples>): List<run.qontract.core.pattern.Examples> = examplesList.map { examplesFrom(it) }

        fun examplesFrom(examples: Examples): run.qontract.core.pattern.Examples {
            val columns = getColumnNames(examples)
            val rows = examples.tableBodyList.map { Row(columns, getValues(it)) }

            return Examples(columns, rows)
        }

        private fun getColumnNames(examples: Examples) = getValues(examples.tableHeader)

        private fun getValues(row: Messages.GherkinDocument.Feature.TableRow): ArrayList<String> = ArrayList(row.cellsList.map { it.value })

        private fun getValues(line: String): List<String> {
            val values = " $line ".split("\\|".toRegex()).map { value -> value.trim()}
            return values.drop(1).dropLast(1)
        }
    }

}