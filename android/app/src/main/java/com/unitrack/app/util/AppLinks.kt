package com.unitrack.app.util

import com.unitrack.app.BuildConfig

object AppLinks {
    const val PRIVACY_PATH = "/legal/privacy"
    const val DELETE_ACCOUNT_PATH = "/legal/delete-account"

    val privacyPolicyUrl: String
        get() = BuildConfig.WEB_BASE_URL + PRIVACY_PATH

    val deleteAccountWebUrl: String
        get() = BuildConfig.WEB_BASE_URL + DELETE_ACCOUNT_PATH
}