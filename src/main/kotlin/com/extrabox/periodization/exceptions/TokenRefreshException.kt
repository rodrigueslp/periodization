package com.extrabox.periodization.exceptions

class TokenRefreshException(token: String, message: String) : RuntimeException("Failed for [$token]: $message")
