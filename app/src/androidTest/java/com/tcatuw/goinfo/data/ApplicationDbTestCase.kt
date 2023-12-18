package com.tcatuw.goinfo.data

import android.database.sqlite.SQLiteOpenHelper
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

open class ApplicationDbTestCase {
    protected lateinit var dbHelper: SQLiteOpenHelper
    protected lateinit var database: com.tcatuw.goinfo.data.Database

    @BeforeTest fun setUpHelper() {
        dbHelper = com.tcatuw.goinfo.data.StreetCompleteSQLiteOpenHelper(
            InstrumentationRegistry.getInstrumentation().targetContext,
            com.tcatuw.goinfo.data.ApplicationDbTestCase.Companion.DATABASE_NAME
        )
        database = com.tcatuw.goinfo.data.AndroidDatabase(dbHelper)
    }

    @Test fun databaseAvailable() {
        assertNotNull(dbHelper.readableDatabase)
    }

    @AfterTest fun tearDownHelper() {
        dbHelper.close()
        InstrumentationRegistry.getInstrumentation().targetContext
            .deleteDatabase(com.tcatuw.goinfo.data.ApplicationDbTestCase.Companion.DATABASE_NAME)
    }

    companion object {
        private const val DATABASE_NAME = "streetcomplete_test.db"
    }
}
