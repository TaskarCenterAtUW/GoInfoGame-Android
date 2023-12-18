package com.tcatuw.goinfo.util.ktx

import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class SQLiteDatabaseKtTest : com.tcatuw.goinfo.data.ApplicationDbTestCase() {

    @BeforeTest fun setUp() {
        dbHelper.writableDatabase.execSQL("CREATE TABLE t (a int, b int)")
    }

    @AfterTest fun tearDown() {
        dbHelper.writableDatabase.execSQL("DROP TABLE t")
    }
}
