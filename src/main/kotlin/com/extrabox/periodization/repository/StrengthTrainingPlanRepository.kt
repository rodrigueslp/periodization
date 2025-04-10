package com.extrabox.periodization.repository

import com.extrabox.periodization.entity.StrengthTrainingPlan
import com.extrabox.periodization.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface StrengthTrainingPlanRepository : JpaRepository<StrengthTrainingPlan, Long> {
    fun findByAthleteNameContainingIgnoreCase(name: String): List<StrengthTrainingPlan>
    fun findByTrainingGoal(goal: String): List<StrengthTrainingPlan>
    fun findByTrainingFocus(focus: String): List<StrengthTrainingPlan>
    fun findTop10ByOrderByCreatedAtDesc(): List<StrengthTrainingPlan>
    fun findByPlanId(planId: String): Optional<StrengthTrainingPlan>
    fun findByUserOrderByCreatedAtDesc(user: User): List<StrengthTrainingPlan>
}