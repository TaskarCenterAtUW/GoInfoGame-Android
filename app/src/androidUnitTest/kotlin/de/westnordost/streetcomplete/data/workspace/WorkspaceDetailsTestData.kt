package de.westnordost.streetcomplete.data.workspace

import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/* What GET {workspaceBaseUrl}/{id} returns when the user taps a workspace in the list - the
 * workspace details, with the long form in `longFormQuestDef` and the conflict mode in
 * `overrideConflicts`. */

/** A versioned long form: one element covering every question type, a dependency and a photo
 *  follow-up, plus feature presets and custom icons. */
const val VALID_LONG_FORM = """{
  "version": "3.2.0",
  "recency_period": 30,
  "feature-presets": [{"name": "Bench", "icon": "preset_temaki_bench", "tags": {"amenity": "bench"}}],
  "custom-icons": [{"name": "streetlight", "url": "https://pinhead.ink/v25/lantern_lamppost.svg", "type": "feature-preset"}],
  "elements": [{
    "element_type": "Sidewalks",
    "element_type_icon": "sidewalk",
    "quest_query": "ways with (highway=footway and footway=sidewalk)",
    "quests": [
      {"quest_id": 101, "quest_title": "Surface?", "quest_type": "ExclusiveChoice", "quest_tag": "ext:surface",
       "quest_answer_choices": [{"value": "asphalt", "choice_text": "Asphalt"}, {"value": "other", "choice_text": "Other"}]},
      {"quest_id": 102, "quest_title": "Describe the surface", "quest_type": "TextEntry", "quest_tag": "ext:surface:description",
       "quest_answer_dependency": {"question_id": 101, "required_value": "other"}},
      {"quest_id": 103, "quest_title": "Width?", "quest_type": "Numeric", "quest_tag": "width",
       "quest_answer_validation": {"min": 12, "max": 240}},
      {"quest_id": 104, "quest_title": "Obstructions?", "quest_type": "MultipleChoice", "quest_tag": "ext:obstruction:type",
       "quest_answer_choices": [{"value": "bollard", "choice_text": "Bollard"},
                                {"value": "other", "choice_text": "Other", "choice_follow_up": "Please take a photo of the obstruction."}]}
    ]
  }]
}"""

/** The older format: just the elements array, no version/presets/recency. */
const val LEGACY_LONG_FORM = """[{
  "element_type": "Kerb",
  "quest_query": "nodes with barrier=kerb",
  "quests": [{"quest_id": 1, "quest_title": "Kerb type?", "quest_type": "ExclusiveChoice", "quest_tag": "kerb",
              "quest_answer_choices": [{"value": "raised", "choice_text": "Raised"}]}]
}]"""

/**
 * A workspace-details response body. [overrideConflicts] and [longFormQuestDef] are raw JSON
 * (e.g. `"true"`, `"null"`); passing null for [overrideConflicts] leaves the key out entirely,
 * as older backends do.
 */
fun workspaceDetailsJson(
    id: Int = 7,
    overrideConflicts: String? = null,
    longFormQuestDef: String = VALID_LONG_FORM,
    imageryListDef: String = "null",
): String {
    val overrideField = overrideConflicts?.let { """"overrideConflicts": $it,""" } ?: ""
    return """{
      "id": $id,
      "title": "Test Workspace",
      "type": "osw",
      "description": null,
      "createdAt": "2025-01-01T00:00:00Z",
      "createdBy": "user-1",
      "createdByName": "Jane Doe",
      "externalAppAccess": 1,
      $overrideField
      "imageryListDef": $imageryListDef,
      "kartaViewToken": null,
      "longFormQuestDef": $longFormQuestDef,
      "tdeiMetadata": null,
      "tdeiProjectGroupId": "pg-1",
      "tdeiRecordId": null,
      "tdeiServiceId": null,
      "someFieldAddedLater": {"nested": true}
    }"""
}

private val KNOWN_QUEST_TYPES = setOf("ExclusiveChoice", "MultipleChoice", "TextEntry", "Numeric")

/**
 * What a long form must satisfy for the map and the form to work with it: every element has a
 * type and a parseable quest query, and every question has a unique id, a tag, a title and a
 * known type; choice questions have choices with values; dependencies point at a question of the
 * same element; numeric ranges aren't inverted.
 */
fun assertValidLongForm(elements: List<Elements>) {
    assertTrue(elements.isNotEmpty(), "long form has no elements")
    for (element in elements) {
        val name = element.elementType
        assertFalse(name.isNullOrBlank(), "element without element_type")
        val query = assertNotNull(element.questQuery, "$name: no quest_query")
        try {
            query.toElementFilterExpression()
        } catch (e: Exception) {
            fail("$name: quest_query doesn't parse: ${e.message}")
        }
        val quests = element.quests.map { assertNotNull(it, "$name: null question") }
        assertTrue(quests.isNotEmpty(), "$name: no questions")
        val ids = quests.map { assertNotNull(it.questId, "$name: question without quest_id") }
        assertEquals(ids.size, ids.toSet().size, "$name: duplicate quest_id in $ids")

        for (quest in quests) {
            val label = "$name/${quest.questId}"
            assertFalse(quest.questTag.isNullOrBlank(), "$label: no quest_tag")
            assertFalse(quest.questTitle.isNullOrBlank(), "$label: no quest_title")
            assertTrue(quest.questType in KNOWN_QUEST_TYPES, "$label: unknown quest_type ${quest.questType}")
            if (quest.questType == "ExclusiveChoice" || quest.questType == "MultipleChoice") {
                val choices = quest.questAnswerChoices.orEmpty()
                assertTrue(choices.isNotEmpty(), "$label: choice question without choices")
                assertTrue(choices.all { !it?.value.isNullOrBlank() }, "$label: choice without a value")
            }
            quest.questAnswerValidation?.let { v ->
                val min = v.min
                val max = v.max
                if (min != null && max != null) assertTrue(min <= max, "$label: min > max")
            }
            quest.questAnswerDependency?.forEach { dependency ->
                assertTrue(dependency.questionId in ids, "$label: depends on unknown question ${dependency.questionId}")
            }
        }
    }
}
