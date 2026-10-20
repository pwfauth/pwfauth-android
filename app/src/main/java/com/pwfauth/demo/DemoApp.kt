package com.pwfauth.demo

import android.app.Application

/** Holds the sign-in state for the whole process, so turning the phone doesn't sign anyone out. */
class DemoApp : Application() {
    val auth by lazy { AuthController(this) }
}
