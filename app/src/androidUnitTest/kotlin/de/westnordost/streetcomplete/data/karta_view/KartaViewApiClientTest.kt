package de.westnordost.streetcomplete.data.karta_view

import com.russhwolf.settings.MapSettings
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.SystemFileSystem
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Every way a photo upload can fail must surface as [KartaViewException] - that's what the edit
 *  and note uploaders treat as "photo failed, keep the edit for the next sync" (and count towards
 *  the stuck-photo notice); anything else aborts the whole upload run. */
class KartaViewApiClientTest {

    private var requests = 0

    private fun client(answer: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        KartaViewApiClient(
            SystemFileSystem,
            HttpClient(MockEngine { requests++; answer(it) }),
            Preferences(MapSettings()),
        )

    private val image = listOf(ByteArray(8) to 0f)
    private val position = LatLon(47.6553, -122.3035)

    @Test fun `unreachable KartaView is a KartaViewException`() = runTest {
        assertFailsWith<KartaViewException> {
            client { throw IOException("connection refused") }.uploadImages(image, position)
        }
    }

    @Test fun `DNS failure is a KartaViewException`() = runTest {
        // not an IOException
        assertFailsWith<KartaViewException> {
            client { throw UnresolvedAddressException() }.uploadImages(image, position)
        }
    }

    @Test fun `error status is a KartaViewException`() = runTest {
        assertFailsWith<KartaViewException> {
            client { respond("down", HttpStatusCode.ServiceUnavailable) }.uploadImages(image, position)
        }
    }

    @Test fun `nothing to upload makes no request`() = runTest {
        assertEquals(emptyList(), client { error("no request expected") }.uploadImages(emptyList(), position))
        assertEquals(0, requests)
    }
}
