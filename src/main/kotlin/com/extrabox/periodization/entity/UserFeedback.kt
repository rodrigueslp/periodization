package com.extrabox.periodization.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_feedback")
class UserFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "feedback_text", columnDefinition = "TEXT", nullable = false)
    var feedbackText: String = ""

    @Column(name = "feedback_type", nullable = false)
    var feedbackType: String = "" // "GENERAL", "BUG", "FEATURE_REQUEST", "IMPROVEMENT"

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @Column(name = "plan_id")
    var planId: String? = null

    // Construtor
    constructor(
        feedbackText: String,
        feedbackType: String,
        user: User? = null,
        planId: String? = null
    ) {
        this.feedbackText = feedbackText
        this.feedbackType = feedbackType
        this.user = user
        this.planId = planId
        this.createdAt = LocalDateTime.now()
    }

    // Construtor padrão necessário para JPA
    constructor()
}