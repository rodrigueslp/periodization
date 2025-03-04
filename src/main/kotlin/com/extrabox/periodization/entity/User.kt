package com.extrabox.periodization.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false, unique = true)
    var email: String = ""

    @Column(name = "full_name", nullable = false)
    var fullName: String = ""

    @Column(name = "profile_picture")
    var profilePicture: String? = null

    @Column(name = "google_id", unique = true)
    var googleId: String? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "last_login")
    var lastLogin: LocalDateTime? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @Column(name = "subscription_plan")
    var subscriptionPlan: String? = null

    @Column(name = "subscription_expiry")
    var subscriptionExpiry: LocalDateTime? = null

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var roles: MutableSet<Role> = mutableSetOf()

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var trainingPlans: MutableList<TrainingPlan> = mutableListOf()

    // Construtor secundário
    constructor(
        id: Long? = null,
        email: String,
        fullName: String,
        profilePicture: String? = null,
        googleId: String? = null,
        active: Boolean = true,
        subscriptionPlan: String? = null,
        subscriptionExpiry: LocalDateTime? = null
    ) {
        this.id = id
        this.email = email
        this.fullName = fullName
        this.profilePicture = profilePicture
        this.googleId = googleId
        this.active = active
        this.subscriptionPlan = subscriptionPlan
        this.subscriptionExpiry = subscriptionExpiry
    }

    // Construtor padrão sem argumentos necessário para o JPA
    constructor()
}