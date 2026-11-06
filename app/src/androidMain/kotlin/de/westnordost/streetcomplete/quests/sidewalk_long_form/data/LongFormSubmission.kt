package de.westnordost.streetcomplete.quests.sidewalk_long_form.data

import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhoto
import de.westnordost.streetcomplete.data.preferences.Environment

/** URL of the (Panoramax) photo attached to the element - see ElementEditsUploader */
const val STREET_IMAGES_TAG = "ext:street_images"
/** where the photo URL went before the switch from KartaView to Panoramax - still read, and
 *  removed along with the photo */
const val KARTAVIEW_URL_TAG = "ext:kartaview_url"

/** The URL of the photo already attached to an element with [tags], if any */
fun getAttachedPhotoUrl(tags: Map<String, String>): String? =
    tags[STREET_IMAGES_TAG] ?: tags[KARTAVIEW_URL_TAG]

/** The URL to show the attached photo at [url] with in the form's small preview: for a picture of
 *  our Panoramax instance its thumbnail (~500px wide) instead of the HD image - the tag may also
 *  come from other clients that upload full-size photos (several MB). Opening the photo
 *  full-screen still uses [url]. Any other URL (e.g. KartaView) is returned as is. */
fun getPhotoPreviewUrl(url: String): String {
    val isPanoramaxHd = url.endsWith(PANORAMAX_HD_SUFFIX) &&
        Environment.entries.any { url.startsWith(it.panoramaxBaseUrl + "api/pictures/") }
    return if (isPanoramaxHd) url.removeSuffix(PANORAMAX_HD_SUFFIX) + "/thumb.jpg" else url
}

private const val PANORAMAX_HD_SUFFIX = "/hd.jpg"

/** The photo-related state of a long-form quest while it is open - see ALongForm. Kept as plain
 *  values (with the pure transitions below) rather than private Fragment state so the photo
 *  lifecycle and the submit decision can be unit-tested without a Fragment. */
data class PhotoState(
    val attachment: PhotoAttachment = PhotoAttachment.None,
    /** Set when the user deletes an already-synced photo (from a previous visit) without
     *  attaching a replacement - signals the submit to actually remove the tag. */
    val existingPhotoRemoved: Boolean = false,
)

/** A new [PhotoState] plus the local file (a no-longer-needed pending capture) the caller must
 *  delete from disk, if any - the only side effect these transitions need. */
data class PhotoTransition(val state: PhotoState, val fileToDelete: String? = null)

/** single-photo model: a new capture replaces whatever was pending before. [replaces]
 *  records what that was (an already-synced photo, or one already marked for removal) so
 *  cancelling THIS capture (afterPhotoDeleted, below) can revert to it instead of forgetting
 *  it ever existed. Retaking again before submitting (Pending -> camera -> new Pending)
 *  carries the ORIGINAL replaces forward, not the intermediate Pending, so no matter how
 *  many times the user retakes, cancelling always lands back on the true starting point. */
fun PhotoState.afterPhotoCaptured(path: String, bearing: Float?): PhotoTransition {
    val previous = attachment
    val replaces = when (previous) {
        is PhotoAttachment.Pending -> previous.replaces
        is PhotoAttachment.Uploaded, is PhotoAttachment.PendingRemoval -> previous
        PhotoAttachment.None -> null
    }
    return PhotoTransition(
        copy(attachment = PhotoAttachment.Pending(path, bearing, replaces)),
        fileToDelete = (previous as? PhotoAttachment.Pending)?.path
    )
}

/** A not-yet-synced capture is cancelled by reverting to whatever it was replacing (an
 *  already-synced photo, or one already marked for removal) rather than always dropping to
 *  [PhotoAttachment.None] - otherwise cancelling a retake would silently discard an
 *  already-synced photo that was never actually asked to be removed. An already-synced photo
 *  instead moves to PendingRemoval: visibly marked for removal but reversible (see
 *  afterPhotoUndoRemoval) until Submit actually applies it. */
fun PhotoState.afterPhotoDeleted(): PhotoTransition = when (val attachment = attachment) {
    is PhotoAttachment.Pending -> {
        val reverted = when (val prev = attachment.replaces) {
            is PhotoAttachment.Uploaded -> PhotoState(prev, existingPhotoRemoved = false)
            is PhotoAttachment.PendingRemoval -> PhotoState(prev, existingPhotoRemoved = true)
            is PhotoAttachment.Pending, PhotoAttachment.None, null ->
                copy(attachment = PhotoAttachment.None)
        }
        PhotoTransition(reverted, fileToDelete = attachment.path)
    }
    is PhotoAttachment.Uploaded ->
        PhotoTransition(PhotoState(PhotoAttachment.PendingRemoval(attachment.url), existingPhotoRemoved = true))
    is PhotoAttachment.PendingRemoval, PhotoAttachment.None ->
        PhotoTransition(copy(attachment = PhotoAttachment.None))
}

fun PhotoState.afterPhotoUndoRemoval(): PhotoState {
    val attachment = attachment as? PhotoAttachment.PendingRemoval ?: return this
    return PhotoState(PhotoAttachment.Uploaded(attachment.url), existingPhotoRemoved = false)
}

/** Unlike afterPhotoDeleted, always ends at [PhotoAttachment.None] regardless of history - used
 *  when the photo's own question is no longer applicable at all (see the orphan check in
 *  computeLongFormSubmission), where there's nothing left to revert to or keep undoable. Still
 *  correctly removes an already-synced photo's tag on submit if this was, or was replacing, one. */
fun PhotoState.afterOrphanedPhotoDiscarded(): PhotoTransition {
    val attachment = attachment
    val wasEverSynced = when (attachment) {
        is PhotoAttachment.Uploaded, is PhotoAttachment.PendingRemoval -> true
        is PhotoAttachment.Pending -> attachment.replaces != null
        PhotoAttachment.None -> false
    }
    return PhotoTransition(
        PhotoState(PhotoAttachment.None, existingPhotoRemoved = existingPhotoRemoved || wasEverSynced),
        fileToDelete = (attachment as? PhotoAttachment.Pending)?.path
    )
}

/** What a long-form submit should apply - see [computeLongFormSubmission]. Nothing to submit at
 *  all when [isEmpty]. */
data class LongFormSubmission(
    val items: List<LongFormQuest>,
    val removeTagKeys: List<String>,
    val photo: FeaturePhoto?,
    /** Non-null when the photo was orphaned and must be discarded - the caller applies it
     *  (including deleting its file) regardless of whether anything ends up being submitted. */
    val photoTransition: PhotoTransition?,
) {
    val isEmpty: Boolean get() = items.isEmpty() && removeTagKeys.isEmpty()
}

/** Decides what ALongForm.onClickOk submits, given the live question state ([givenItems] - the
 *  adapter's in-place-mutated list, NOT its rendered snapshots) and the current photo state.
 *
 *  [recheckEnabled] gates the "resubmit every answered question on a no-diff recheck" behavior.
 *  It is off in multi-select mode: resubmitting every answered field there would apply to every
 *  selected element, and an element whose field genuinely differs from the primary/session value
 *  would get silently overwritten. In that case a no-diff recheck just yields nothing to submit. */
fun computeLongFormSubmission(
    givenItems: List<LongFormQuest>,
    photoState: PhotoState,
    recheckEnabled: Boolean,
): LongFormSubmission {
    // No null/isEmpty guard here on purpose: a question the user deselected/cleared back to
    // nothing (userInput null or empty) after it had a seeded answer must still be included -
    // otherwise the clear is silently dropped and the stale tag from before never gets
    // removed (contentEquals(null, null) already excludes a question that was never touched,
    // so this alone is sufficient to also exclude untouched blanks).
    //
    // A question currently hidden by questAnswerDependency (its controlling answer no longer
    // satisfies the dependency - e.g. the user deselected/changed the controlling answer this
    // visit) is submitted as cleared too, via a .copy() used ONLY for this comparison/submit -
    // the live item in givenItems is left untouched. This matters: if the user flips
    // the controlling answer back before submitting, the dependent question must still show
    // whatever was already entered, not something wiped out mid-edit. Only the final state at
    // submit time decides whether a hidden question's old value actually gets removed.
    val editedItems =
        givenItems.mapNotNull { quest ->
            val effective = if (quest.visible) quest else quest.copy(userInput = null)
            effective.takeIf { !it.userInput.contentEquals(it.seededAnswer) }
        }
    // If the choice this photo was attached to is no longer selected (the user changed their
    // answer this visit), the photo is orphaned - the card is already hidden in that case
    // (see LongFormAdapter.updateChoiceFollowUp), but without this the underlying tag/local
    // file would silently survive unchanged. Discard it entirely, using the final state at
    // submit time - same principle as editedItems above. Deliberately NOT afterPhotoDeleted():
    // that reverts one step (back to whatever a pending capture would replace, or to a
    // reversible "marked for removal"), which is right for the remove button but wrong here - the
    // question itself is gone, so there's nothing left to revert to or keep undoable.
    val photoTransition =
        if (photoState.attachment != PhotoAttachment.None &&
            givenItems.none { it.visible && it.activeChoiceFollowUp() != null }
        ) {
            photoState.afterOrphanedPhotoDiscarded()
        } else {
            null
        }
    val finalPhotoState = photoTransition?.state ?: photoState

    // the photo's URL isn't known yet if it was just captured this visit (upload happens
    // later, in the background sync - see AbstractOsmQuestForm.applyAnswer) - only an
    // explicit removal of an already-synced photo (without a replacement) is a tag change we
    // can express right away
    // the question whose choiceFollowUp asked for the photo - the same one whose row shows it
    // (see LongFormAdapter.photoAttachment)
    val photoQuestion = givenItems.firstOrNull { it.visible && it.activeChoiceFollowUp() != null }
    val photo = (finalPhotoState.attachment as? PhotoAttachment.Pending)?.let {
        FeaturePhoto(it.path, it.bearing, photoQuestion?.questTitle, photoQuestion?.questId)
    }
    val removeTagKeys = if (photo == null && finalPhotoState.existingPhotoRemoved) {
        listOf(STREET_IMAGES_TAG, KARTAVIEW_URL_TAG)
    } else {
        emptyList()
    }

    // a "recheck" of an already fully-answered element - reopened purely because
    // AddGenericLong.isApplicableTo's recency check resurfaced it - has nothing to actually change, so editedItems above is
    // empty and this used to always be blocked with the "No changes" toast, leaving the pin
    // stuck reappearing forever (submitting never bumped the element's OSM timestamp).
    //
    // Only when EVERY currently-visible question already has a seeded answer (i.e. there is no
    // genuinely unanswered question left - the same condition AddGenericLong.isApplicableTo's
    // recency branch itself gates on) do we resubmit every already-answered question's OWN
    // existing value unchanged, purely to force a real, non-empty edit through so the element's
    // timestamp bumps. StringMapChangesBuilder records a same-value set as a real change
    // regardless of value equality, so this produces one Modify(key, x, x) per question - undo
    // reverts each back to the same value, so this is safe. If a real gap remains (some visible
    // question was never answered), this is NOT a recheck - nothing to submit, so the user is
    // still nudged to actually answer it.
    val isFullyAnswered = givenItems.none { it.visible && it.seededAnswer == null }
    val submittedItems = editedItems.ifEmpty {
        if (photo != null && recheckEnabled) {
            // A still-pending photo capture has no tag value of its own yet (the URL only
            // exists after upload - see the photo/removeTagKeys comment above), so attaching
            // it to a real edit means riding along on some other tag change. Resubmit ONLY the
            // one question this photo's follow-up is tied to (guaranteed to already have a
            // seeded answer here, since editedItems being empty means that question's
            // selection is unchanged from its seed) rather than every answered question, so a
            // photo-only submit doesn't silently bump every other tag on the element too.
            givenItems.filter { it.visible && it.activeChoiceFollowUp() != null }
        } else if (isFullyAnswered && recheckEnabled) {
            givenItems.filter { it.visible && it.seededAnswer != null }
        } else {
            emptyList()
        }
    }

    // removeTagKeys alone (an explicit photo removal, with no replacement and no question
    // changes) is already a real, non-empty tag change on its own - createQuestChanges applies
    // it regardless of how many items are in submittedItems - so it also counts as something to
    // submit even when submittedItems ends up empty (see LongFormSubmission.isEmpty).
    return LongFormSubmission(submittedItems, removeTagKeys, photo, photoTransition)
}
