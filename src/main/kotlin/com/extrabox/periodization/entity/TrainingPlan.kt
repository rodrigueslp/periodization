package com.extrabox.periodization.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "training_plans")
class TrainingPlan {
    @Id
    @Column(name = "plan_id")
    var planId: String = ""

    @Column(name = "athlete_name", nullable = false)
    var athleteName: String = ""

    @Column(name = "athlete_age", nullable = false)
    var athleteAge: Int = 0

    @Column(name = "athlete_weight", nullable = false)
    var athleteWeight: Double = 0.0

    @Column(name = "athlete_height", nullable = false)
    var athleteHeight: Int = 0

    @Column(name = "experience_level", nullable = false)
    var experienceLevel: String = ""

    @Column(name = "training_goal", nullable = false)
    var trainingGoal: String = ""

    @Column(name = "availability", nullable = false)
    var availability: Int = 0

    @Column(name = "injuries")
    var injuries: String? = null

    @Column(name = "training_history")
    var trainingHistory: String? = null

    @Column(name = "plan_duration", nullable = false)
    var planDuration: Int = 0

    @Column(name = "plan_content", columnDefinition = "TEXT", nullable = false)
    var planContent: String = ""

    @Column(name = "excel_file_path", nullable = false)
    var excelFilePath: String = ""

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    // Nova relação com o usuário
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    // Construtor secundário que recebe todos os parâmetros para facilitar a migração do código existente
    constructor(
        planId: String,
        athleteName: String,
        athleteAge: Int,
        athleteWeight: Double,
        athleteHeight: Int,
        experienceLevel: String,
        trainingGoal: String,
        availability: Int,
        injuries: String?,
        trainingHistory: String?,
        planDuration: Int,
        planContent: String,
        excelFilePath: String,
        createdAt: LocalDateTime = LocalDateTime.now(),
        user: User? = null
    ) {
        this.planId = planId
        this.athleteName = athleteName
        this.athleteAge = athleteAge
        this.athleteWeight = athleteWeight
        this.athleteHeight = athleteHeight
        this.experienceLevel = experienceLevel
        this.trainingGoal = trainingGoal
        this.availability = availability
        this.injuries = injuries
        this.trainingHistory = trainingHistory
        this.planDuration = planDuration
        this.planContent = planContent
        this.excelFilePath = excelFilePath
        this.createdAt = createdAt
        this.user = user
    }

    // Construtor padrão sem argumentos necessário para o JPA
    constructor()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrainingPlan

        if (planId != other.planId) return false

        return true
    }

    override fun hashCode(): Int {
        return planId.hashCode()
    }
}
