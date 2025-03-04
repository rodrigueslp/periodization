package com.extrabox.periodization.repository

import com.extrabox.periodization.entity.TrainingPlan
import com.extrabox.periodization.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TrainingPlanRepository : JpaRepository<TrainingPlan, Long> {
    fun findByAthleteNameContainingIgnoreCase(name: String): List<TrainingPlan>
    fun findByTrainingGoal(goal: String): List<TrainingPlan>
    fun findTop10ByOrderByCreatedAtDesc(): List<TrainingPlan>
    fun findByPlanId(planId: String): Optional<TrainingPlan>
    fun findByUserOrderByCreatedAtDesc(user: User): List<TrainingPlan>
}
