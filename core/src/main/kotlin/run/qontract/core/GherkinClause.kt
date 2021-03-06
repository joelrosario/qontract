package run.qontract.core

import run.qontract.core.GherkinSection.*
import run.qontract.core.pattern.ContractException
import run.qontract.core.pattern.Pattern
import run.qontract.core.pattern.TabularPattern
import run.qontract.core.pattern.withoutPatternDelimiters
import run.qontract.core.value.*

data class GherkinClause(val content: String, val section: GherkinSection)

enum class GherkinSection(val prefix: String) {
    Given("Given"), When("When"), Then("Then"), `*`("*")
}

fun responseBodyToGherkinClauses(typeName: String, body: Value?, types: Map<String, Pattern>): Triple<List<GherkinClause>, Map<String, Pattern>, ExampleDeclarations>? {
    if(body == EmptyString)
        return Triple(emptyList(), types, DiscardExampleDeclarations())

    return body?.typeDeclarationWithKey(typeName, types, DiscardExampleDeclarations())?.let { (typeDeclaration, _) ->
        val bodyClause = GherkinClause("response-body ${typeDeclaration.typeValue}", Then)
        Triple(listOf(bodyClause), typeDeclaration.types, DiscardExampleDeclarations())
    }
}

fun requestBodyToGherkinClauses(body: Value?, types: Map<String, Pattern>, exampleDeclarationsStore: ExampleDeclarations): Triple<List<GherkinClause>, Map<String, Pattern>, ExampleDeclarations> {
    if(body == EmptyString)
        return Triple(emptyList(), types, exampleDeclarationsStore)

    val declarations = body?.typeDeclarationWithoutKey("RequestBody", types, exampleDeclarationsStore)?.let { (typeDeclaration, exampleDeclaration) ->
        val typeValue = exampleDeclaration.getNewName(typeDeclaration.typeValue, typeDeclaration.types.keys)

        val bodyClause = GherkinClause("request-body $typeValue", When)

        Triple(listOf(bodyClause), typeDeclaration.types, exampleDeclaration)
    }

    return declarations ?: Triple(emptyList(), types, exampleDeclarationsStore)
}

fun toGherkinClauses(patterns: Map<String, Pattern>): List<GherkinClause> {
    return patterns.entries.map { (key, pattern) -> toClause(key, pattern) }
}

fun headersToGherkin(headers: Map<String, String>, keyword: String, types: Map<String, Pattern>, exampleDeclarationsStore: ExampleDeclarations, section: GherkinSection): Triple<List<GherkinClause>, Map<String, Pattern>, ExampleDeclarations> {
    val (dictionaryTypeMap, newTypes, newExamples) = dictionaryToDeclarations(stringMapToValueMap(headers), types, exampleDeclarationsStore)

    val headerClauses = dictionaryTypeMap.entries.map {
        "$keyword ${it.key} ${it.value.pattern}"
    }.map { GherkinClause(it, section) }

    return Triple(headerClauses, newTypes, newExamples)
}

fun toClause(key: String, type: Pattern): GherkinClause {
    val title = "type ${withoutPatternDelimiters(key)}"

    val table = when (type) {
        is TabularPattern -> patternMapToString(type.pattern)
        else -> throw ContractException("Type not recognised: ${type.toString()}")
    }

    return GherkinClause("$title\n$table", Given)
}

private fun patternMapToString(json: Map<String, Pattern>): String {
    return json.entries.joinToString("\n") {
        "  | ${it.key.replace("|", "\\|")} | ${it.value.pattern.toString().replace("|", "\\|")} |"
    }
}

fun withFeatureClause(name: String, scenarios: String): String {
    return """Feature: $name
${scenarios.prependIndent("  ")}
"""
}

fun withScenarioClause(scenarioName: String, scenarioData: String): String {
    return """Scenario: $scenarioName
${scenarioData.prependIndent("  ")}
"""
}

fun toGherkinFeature(scenarioName: String, clauses: Pair<List<GherkinClause>, ExampleDeclarations>): String = withFeatureClause("New Feature", toGherkinScenario(scenarioName, clauses))

fun toGherkinScenario(scenarioName: String, declarations: Pair<List<GherkinClause>, ExampleDeclarations>): String {
    val (clauses, exampleDeclaration) = declarations
    val groupedClauses = clauses.groupBy { it.section }

    val prefixesInOrder = listOf(Given, When, Then, `*`)

    val statements = prefixesInOrder.flatMap { section ->
        val sectionClauses = groupedClauses[section] ?: emptyList()
        val prefixes = listOf(section.prefix).plus(1.until(sectionClauses.size).map { "And" })
        sectionClauses.zip(prefixes).map { (clause, prefix) -> GherkinStatement(clause.content, prefix) }
    }

    val statementString = statements.joinToString("\n") { it.toGherkinString() }

    val scenarioGherkin = when {
        exampleDeclaration.examples.isNotEmpty() -> {
            val examplesString = toExampleGherkinString(exampleDeclaration)
            "$statementString\n\n$examplesString"
        }
        else -> statementString
    }

    return withScenarioClause(scenarioName, scenarioGherkin)
}

internal fun toExampleGherkinString(exampleDeclarationsStore: ExampleDeclarations): String {
    val entries = exampleDeclarationsStore.examples.entries.toList()
    val heading = """| ${entries.joinToString(" | ") { it.key.replace("|", "\\|") }} |"""
    val firstRow = """| ${entries.joinToString(" | ") { it.value.replace("|", "\\|") }} |"""

    return "Examples:\n$heading\n$firstRow"
}
