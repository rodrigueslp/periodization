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
        createdAt: LocalDateTime = LocalDateTime.now()
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
    }

    // Construtor padrão sem argumentos necessário para o JPA
    constructor()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrainingPlan

        if (planId != other.planId) return false
        if (athleteName != other.athleteName) return false
        if (athleteAge != other.athleteAge) return false
        if (athleteWeight != other.athleteWeight) return false
        if (athleteHeight != other.athleteHeight) return false
        if (experienceLevel != other.experienceLevel) return false
        if (trainingGoal != other.trainingGoal) return false
        if (availability != other.availability) return false
        if (injuries != other.injuries) return false
        if (trainingHistory != other.trainingHistory) return false
        if (planDuration != other.planDuration) return false
        if (planContent != other.planContent) return false
        if (excelFilePath != other.excelFilePath) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = planId.hashCode()
        result = 31 * result + athleteName.hashCode()
        result = 31 * result + athleteAge
        result = 31 * result + athleteWeight.hashCode()
        result = 31 * result + athleteHeight
        result = 31 * result + experienceLevel.hashCode()
        result = 31 * result + trainingGoal.hashCode()
        result = 31 * result + availability
        result = 31 * result + (injuries?.hashCode() ?: 0)
        result = 31 * result + (trainingHistory?.hashCode() ?: 0)
        result = 31 * result + planDuration
        result = 31 * result + planContent.hashCode()
        result = 31 * result + excelFilePath.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
