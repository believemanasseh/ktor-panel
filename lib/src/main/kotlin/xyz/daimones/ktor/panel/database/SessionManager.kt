package xyz.daimones.ktor.panel.database

import java.util.concurrent.ConcurrentHashMap

object InMemorySessionManager {
    private data class Session(val username: String, val expiresAt: Long)

    private val sessions = ConcurrentHashMap<String, Session>()

    fun set(sessionId: String, username: String, maxAge: Long) {
        val expiresAt = System.currentTimeMillis() + maxAge * 1000
        sessions[sessionId] = Session(username, expiresAt)
    }

    fun get(sessionId: String): String? {
        val session = sessions[sessionId]
        if (session != null && session.expiresAt > System.currentTimeMillis()) {
            return session.username
        } else {
            sessions.remove(sessionId)
            return null
        }
    }

    fun remove(sessionId: String) {
        sessions.remove(sessionId)
    }

    fun cleanupExpiredSessions() {
        val now = System.currentTimeMillis()
        sessions.entries.removeIf { it.value.expiresAt <= now }
    }
}