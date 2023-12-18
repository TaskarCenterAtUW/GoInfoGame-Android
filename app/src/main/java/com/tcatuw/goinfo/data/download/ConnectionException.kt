package com.tcatuw.goinfo.data.download

class ConnectionException @JvmOverloads constructor(
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)
