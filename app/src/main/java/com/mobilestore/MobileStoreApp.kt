package com.mobilestore

import android.app.Application
import com.mobilestore.core.NotificationHelper

class MobileStoreApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // El canal se crea al inicio; el permiso se pide recien al usarlo.
        NotificationHelper.ensureChannel(this)
    }
}
