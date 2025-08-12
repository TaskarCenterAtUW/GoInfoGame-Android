package de.westnordost.streetcomplete.util

import android.content.Context
import io.github.optimumcode.json.schema.JsonSchemaLoader
import io.github.optimumcode.json.schema.ValidationError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement


object SchemaValidator {
    private fun loadJSONFromAssets(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    fun validateJsonWithKotlinx(
        context: Context,
        jsonString: String,
        schemaFile: String
    ): Boolean {
        val schemaString = loadJSONFromAssets(
            context = context,
            fileName = schemaFile
        )
        val loader = JsonSchemaLoader.create()
        val schema = loader.fromDefinition(schemaString)

        val jsonElement: JsonElement = Json.parseToJsonElement(jsonString)
        val errors = mutableListOf<ValidationError>()

        val valid = schema.validate(jsonElement, errors::add)
        if (!valid) {
            errors.forEach { error ->
                println("Validation error: ${error.message} at ${error.objectPath}")
            }
        }
        return valid
    }
}
