package com.extrabox.periodization.repository

import com.extrabox.periodization.entity.Payment
import com.extrabox.periodization.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByExternalReference(externalReference: String): Optional<Payment>
    fun findByUser(user: User): List<Payment>
    fun findByUserAndStatus(user: User, status: String): List<Payment>
    fun findByPaymentId(paymentId: String): Optional<Payment>
    fun findByPlanId(planId: String): List<Payment>
    fun findByPlanIdAndStatus(planId: String, status: String): Optional<Payment>
}
