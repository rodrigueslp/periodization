package com.extrabox.periodization.entity

import jakarta.persistence.*

@Entity
@Table(name = "benchmark_data")
class BenchmarkData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "plan_id", nullable = false)
    var planId: String = ""

    @Column(name = "back_squat")
    var backSquat: Double? = null

    @Column(name = "deadlift")
    var deadlift: Double? = null

    @Column(name = "clean")
    var clean: Double? = null

    @Column(name = "snatch")
    var snatch: Double? = null

    @Column(name = "fran")
    var fran: String? = null

    @Column(name = "grace")
    var grace: String? = null

    // Construtor secundário que recebe todos os parâmetros
    constructor(
        id: Long? = null,
        planId: String,
        backSquat: Double? = null,
        deadlift: Double? = null,
        clean: Double? = null,
        snatch: Double? = null,
        fran: String? = null,
        grace: String? = null
    ) {
        this.id = id
        this.planId = planId
        this.backSquat = backSquat
        this.deadlift = deadlift
        this.clean = clean
        this.snatch = snatch
        this.fran = fran
        this.grace = grace
    }

    // Construtor padrão sem argumentos necessário para o JPA
    constructor()
}
