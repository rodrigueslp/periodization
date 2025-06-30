package com.extrabox.periodization.repository

import com.extrabox.periodization.entity.RunningTrainingPlan
import com.extrabox.periodization.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RunningTrainingPlanRepository : JpaRepository<RunningTrainingPlan, Long> {
    fun findByAthleteNameContainingIgnoreCase(name: String): List<RunningTrainingPlan>
    fun findByTrainingGoal(goal: String): List<RunningTrainingPlan>
    fun findByExperienceLevel(level: String): List<RunningTrainingPlan>
    fun findTop10ByOrderByCreatedAtDesc(): List<RunningTrainingPlan>
    fun findByPlanId(planId: String): Optional<RunningTrainingPlan>
    fun findByUserOrderByCreatedAtDesc(user: User): List<RunningTrainingPlan>
    fun findByDataProvaIsNotNull(): List<RunningTrainingPlan> // Planos com prova específica
    fun findByVolumeSemanalAtualBetween(minVolume: Int, maxVolume: Int): List<RunningTrainingPlan>
}
