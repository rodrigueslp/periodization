package com.extrabox.periodization.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "payments")
class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var paymentId: String = ""

    @Column(nullable = false)
    var preferenceId: String = ""

    @Column(name = "external_reference", nullable = false)
    var externalReference: String = ""

    @Column(nullable = false)
    var amount: Double = 0.0

    @Column(nullable = false)
    var status: String = ""

    @Column(nullable = false)
    var description: String = ""

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Column(name = "plan_id")
    var planId: String? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null

    // Construtor secundário
    constructor(
        paymentId: String,
        preferenceId: String,
        externalReference: String,
        amount: Double,
        status: String,
        description: String,
        user: User,
        planId: String? = null
    ) {
        this.paymentId = paymentId
        this.preferenceId = preferenceId
        this.externalReference = externalReference
        this.amount = amount
        this.status = status
        this.description = description
        this.user = user
        this.planId = planId
    }

    // Construtor padrão sem argumentos necessário para o JPA
    constructor()
}
