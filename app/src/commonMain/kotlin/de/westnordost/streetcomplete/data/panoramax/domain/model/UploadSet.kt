package de.westnordost.streetcomplete.data.panoramax.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of POST /api/upload_sets */
@Serializable
data class CreateUploadSetRequest(
    val title: String,
    @SerialName("estimated_nb_files")
    val estimatedNbFiles: Int,
    /** all photos of one edit go into one sequence */
    @SerialName("no_split")
    val noSplit: Boolean = true,
    /** the photos of one edit are all taken at the same position (edit.position), so with the
     *  instance's default (1 m / 60°) the server would reject all but one as duplicates */
    @SerialName("no_deduplication")
    val noDeduplication: Boolean = true,
)

/** Answer of POST /api/upload_sets and POST /api/upload_sets/{id}/complete - only what we use */
@Serializable
data class UploadSet(
    val id: String,
)

/** Answer of POST /api/upload_sets/{id}/files - only what we use */
@Serializable
data class UploadSetFile(
    @SerialName("picture_id")
    val pictureId: String? = null,
    val rejected: FileRejection? = null,
)

@Serializable
data class FileRejection(
    val reason: String,
    val message: String? = null,
)

/** A Panoramax semantic tag (key/value) on a picture - see
 *  https://docs.panoramax.fr/backend/install/deep_dive/semantics/ */
@Serializable
data class SemanticTag(
    val key: String,
    val value: String,
)

/** Body of PATCH /api/pictures/{id} - only what we use */
@Serializable
data class PatchPictureRequest(
    val semantics: List<SemanticTag>,
)
