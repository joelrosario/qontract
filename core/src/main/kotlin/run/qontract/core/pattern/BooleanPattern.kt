package run.qontract.core.pattern

import run.qontract.core.Resolver
import run.qontract.core.Result
import run.qontract.core.mismatchResult
import run.qontract.core.resultReport
import run.qontract.core.value.BooleanValue
import run.qontract.core.value.JSONArrayValue
import run.qontract.core.value.Value
import java.util.*

object BooleanPattern : Pattern, ScalarType {
    override fun matches(sampleData: Value?, resolver: Resolver): Result =
        when(sampleData) {
            is BooleanValue -> Result.Success()
            else -> mismatchResult("boolean", sampleData)
        }

    override fun generate(resolver: Resolver): Value =
        when(Random().nextInt(2)) {
            0 -> BooleanValue(false)
            else -> BooleanValue(true)
        }

    override fun newBasedOn(row: Row, resolver: Resolver): List<Pattern> = listOf(this)
    override fun parse(value: String, resolver: Resolver): Value = when {
        value !in (listOf("true", "false")) -> throw ContractException(resultReport(mismatchResult(BooleanPattern, value)))
        else -> BooleanValue(value.toBoolean())
    }
    override fun encompasses(otherPattern: Pattern, thisResolver: Resolver, otherResolver: Resolver): Result {
        return encompasses(this, otherPattern, thisResolver, otherResolver)
    }

    override fun listOf(valueList: List<Value>, resolver: Resolver): Value {
        return JSONArrayValue(valueList)
    }

    override val typeName: String = "boolean"
    override val pattern: Any = "(boolean)"
    override fun toString(): String = pattern.toString()
}
