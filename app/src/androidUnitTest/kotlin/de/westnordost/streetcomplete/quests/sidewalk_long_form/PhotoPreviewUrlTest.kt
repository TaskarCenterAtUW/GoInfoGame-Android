package de.westnordost.streetcomplete.quests.sidewalk_long_form

import de.westnordost.streetcomplete.data.preferences.Environment
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.getPhotoPreviewUrl
import kotlin.test.Test
import kotlin.test.assertEquals

/** The form's small photo preview loads the Panoramax thumbnail, not the (possibly several MB)
 *  HD image - and leaves any other photo URL alone. */
class PhotoPreviewUrlTest {

    private val pictures = "${Environment.PANORAMAX_DEV_URL}api/pictures/abc-123"

    @Test fun `Panoramax HD picture is previewed by its thumbnail`() {
        assertEquals("$pictures/thumb.jpg", getPhotoPreviewUrl("$pictures/hd.jpg"))
    }

    @Test fun `KartaView photo is previewed as is`() {
        val url = "https://storage.kartaview.org/files/photo/lth/1.jpg"
        assertEquals(url, getPhotoPreviewUrl(url))
    }

    @Test fun `HD picture of another server is previewed as is`() {
        val url = "https://panoramax.example/api/pictures/abc-123/hd.jpg"
        assertEquals(url, getPhotoPreviewUrl(url))
    }

    @Test fun `other image of our Panoramax is previewed as is`() {
        assertEquals("$pictures/sd.jpg", getPhotoPreviewUrl("$pictures/sd.jpg"))
    }
}
