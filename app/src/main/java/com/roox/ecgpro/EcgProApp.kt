package com.roox.ecgpro

import android.app.Application
import com.roox.ecgpro.data.database.AppDatabase
import com.roox.ecgpro.data.repository.Repository

class EcgProApp : Application() {
    val db by lazy { AppDatabase.get(this) }
    val repo by lazy { Repository(db.ecgDao()) }
}
