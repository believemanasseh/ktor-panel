package com.administrativektor

import io.ktor.server.application.*


class Admin(private val application: Application, val configuration: Configuration) {
    init {
        this.application.log.info("Admin class initialisation completed.")
    }
}