package com.extrabox.periodization.repository

import com.extrabox.periodization.entity.BenchmarkData
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BenchmarkDataRepository : JpaRepository<BenchmarkData, Long> {
    fun findByPlanId(planId: String): Optional<BenchmarkData>
    fun deleteByPlanId(planId: String)
}