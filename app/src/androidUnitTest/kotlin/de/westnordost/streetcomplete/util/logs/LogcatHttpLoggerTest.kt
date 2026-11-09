package de.westnordost.streetcomplete.util.logs

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LogcatHttpLoggerTest {
    private val received = mutableListOf<String>()

    /** stands in for DatabaseLogger, which [Log] forwards every message to */
    private val appLogger = object : Logger {
        override fun v(tag: String, message: String) { received += message }
        override fun d(tag: String, message: String) { received += message }
        override fun i(tag: String, message: String) { received += message }
        override fun w(tag: String, message: String, exception: Throwable?) { received += message }
        override fun e(tag: String, message: String, exception: Throwable?) { received += message }
    }

    @BeforeTest fun setUp() {
        Log.instances.add(appLogger)
    }

    @AfterTest fun tearDown() {
        Log.instances.remove(appLogger)
    }

    @Test fun `http logs do not go through the app's Log`() {
        LogcatHttpLogger("OsmClient").log("RESPONSE: 200 OK\nBODY START\n<osm>...</osm>\nBODY END")

        assertEquals(emptyList(), received)
    }
}
