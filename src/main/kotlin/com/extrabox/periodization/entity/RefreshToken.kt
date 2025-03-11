package com.extrabox.periodization.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "refresh_tokens")
class RefreshToken (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val token: String,

    @Column(nullable = false)
    val userEmail: String,

    @Column(nullable = false)
    val expiryDate: Instant
)
