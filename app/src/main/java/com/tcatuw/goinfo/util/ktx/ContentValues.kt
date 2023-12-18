package com.tcatuw.goinfo.util.ktx

import android.content.ContentValues

operator fun ContentValues.plus(b: ContentValues) = ContentValues(this).also { it.putAll(b) }
