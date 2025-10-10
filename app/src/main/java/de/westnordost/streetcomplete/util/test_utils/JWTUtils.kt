package de.westnordost.streetcomplete.util.test_utils

import com.auth0.android.jwt.JWT

fun getEmailFromJWT(token: String): String? {
    val jwt = JWT(token)
    return jwt.getClaim("email").asString()
}

