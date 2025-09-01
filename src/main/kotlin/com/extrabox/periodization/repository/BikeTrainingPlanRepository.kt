package com.extrabox.periodization.repository

import com.extrabox.periodization.entity.BikeTrainingPlan
import com.extrabox.periodization.entity.User
import com.extrabox.periodization.enums.PlanStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BikeTrainingPlanRepository : JpaRepository<BikeTrainingPlan, String> {
    fun findByPlanId(planId: String): java.util.Optional<BikeTrainingPlan>
    fun findByPlanIdAndUser(planId: String, user: User): BikeTrainingPlan?
    fun findByUser(user: User): List<BikeTrainingPlan>
    fun findByStatus(status: PlanStatus): List<BikeTrainingPlan>
    fun findByUserOrderByCreatedAtDesc(user: User): List<BikeTrainingPlan>
}
