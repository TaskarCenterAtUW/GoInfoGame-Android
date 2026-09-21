package de.westnordost.streetcomplete.data.osm.edits.update_tags

import de.westnordost.streetcomplete.data.ConflictException
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.util.ktx.copy
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds

fun Element.changesApplied(changes: StringMapChanges): Element {
    val tags = tags.toMutableMap()
    try {
        changes.applyTo(tags)
    } catch (_: IllegalStateException) {
        throw ConflictException("Conflict while applying the changes")
    } catch (_: IllegalArgumentException) {
        /*
            There is a max key/value length limit of 255 characters in OSM. If we reach this
            point, it means the UI did permit an input of more than that. So, we have to catch
            this here latest.
            The UI should prevent this in the first place, at least
            for free-text input. For structured input, like opening hours, it is another matter
            because it's awkward to explain to a non-technical user this technical limitation

            See also https://github.com/openstreetmap/openstreetmap-website/issues/2025
         */
        throw ConflictException("Key or value is too long")
    }
    return this.copy(
        tags = tags,
        timestampEdited = nowAsEpochMilliseconds()
    )
}

/** Returns a copy of [this] with [key] set to [value], merged in as an Add (if [existingTags]
 *  doesn't already have that key) or a Modify (if it does). Used to fold a deferred photo's
 *  uploaded URL into an already-recorded UpdateElementTagsAction's tags at upload time, since the
 *  URL isn't known until the photo actually uploads - see
 *  [de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditsUploader.uploadPendingPhotos]. */
fun StringMapChanges.withTag(key: String, value: String, existingTags: Map<String, String>): StringMapChanges {
    val entry = if (existingTags.containsKey(key)) {
        StringMapEntryModify(key, existingTags.getValue(key), value)
    } else {
        StringMapEntryAdd(key, value)
    }
    return StringMapChanges(changes.filterNot { it.key == key }.toSet() + entry)
}
