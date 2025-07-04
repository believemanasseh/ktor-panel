package com.example

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "email", length = 50, unique = true, nullable = false)
    var email: String = "",

    @Column(name = "first_name", length = 255, nullable = true)
    var firstName: String? = null,

    @Column(name = "last_name", length = 255, nullable = true)
    var lastName: String? = null,

    @Column(name = "password", length = 255, nullable = false)
    var password: String = "",

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = false,

    @Column(name = "created", nullable = false, updatable = false)
    var created: LocalDateTime = LocalDateTime.now(),

    @Column(name = "modified", nullable = false)
    var modified: LocalDateTime = LocalDateTime.now()
)
