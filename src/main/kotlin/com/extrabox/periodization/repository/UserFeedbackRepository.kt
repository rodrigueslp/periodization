package com.extrabox.periodization.repository

import com.extrabox.periodization.entity.User
import com.extrabox.periodization.entity.UserFeedback
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserFeedbackRepository : JpaRepository<UserFeedback, Long> {
    fun findByUserOrderByCreatedAtDesc(user: User): List<UserFeedback>
    fun findByFeedbackTypeOrderByCreatedAtDesc(feedbackType: String): List<UserFeedback>
    fun findAllByOrderByCreatedAtDesc(): List<UserFeedback>
}