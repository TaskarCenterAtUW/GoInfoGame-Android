package com.tcatuw.goinfo.data

import android.database.sqlite.SQLiteOpenHelper
import com.tcatuw.goinfo.ApplicationConstants
import org.koin.dsl.module

val dbModule = module {
    single<Database> { AndroidDatabase(get()) }
    single<SQLiteOpenHelper> { StreetCompleteSQLiteOpenHelper(get(), ApplicationConstants.DATABASE_NAME) }
}
