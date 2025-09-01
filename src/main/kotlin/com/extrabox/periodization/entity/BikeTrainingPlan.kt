package com.extrabox.periodization.entity

import com.extrabox.periodization.enums.PlanStatus
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "bike_training_plans")
class BikeTrainingPlan {
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

    @Column(name = "dias_disponiveis", nullable = false)
    var diasDisponiveis: Int = 0

    @Column(name = "volume_semanal_atual", nullable = false)
    var volumeSemanalAtual: Int = 0 // horas por semana

    @Column(name = "tipo_bike")
    var tipoBike: String? = null // speed, mountain bike, indoor, etc.

    @Column(name = "ftp_atual")
    var ftpAtual: Int? = null // Functional Threshold Power

    @Column(name = "potencia_media_atual")
    var potenciaMediaAtual: Int? = null

    @Column(name = "melhor_tempo_40km")
    var melhorTempo40km: String? = null

    @Column(name = "melhor_tempo_100km")
    var melhorTempo100km: String? = null

    @Column(name = "melhor_tempo_160km")
    var melhorTempo160km: String? = null

    @Column(name = "tempo_objetivo")
    var tempoObjetivo: String? = null

    @Column(name = "data_prova")
    var dataProva: String? = null

    @Column(name = "historico_lesoes")
    var historicoLesoes: String? = null

    @Column(name = "experiencia_anterior")
    var experienciaAnterior: String? = null

    @Column(name = "preferencia_treino")
    var preferenciaTreino: String? = null // indoor, outdoor, misto

    @Column(name = "equipamentos_disponiveis")
    var equipamentosDisponiveis: String? = null // smart trainer, power meter, etc.

    @Column(name = "zona_treino_preferida")
    var zonaTreinoPreferida: String? = null // resistência, força, velocidade

    @Column(name = "plan_duration", nullable = false)
    var planDuration: Int = 0

    @Column(name = "plan_content", columnDefinition = "TEXT", nullable = false)
    var planContent: String = ""

    @Column(name = "excel_file_path", nullable = false)
    var excelFilePath: String = ""

    @Column(name = "pdf_file_path")
    var pdfFilePath: String = ""

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PlanStatus = PlanStatus.PAYMENT_PENDING

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    // Relação com o usuário
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @Column(name = "start_date")
    var startDate: LocalDate? = null

    @Column(name = "end_date")
    var endDate: LocalDate? = null

    // Construtor secundário
    constructor(
        planId: String,
        athleteName: String,
        athleteAge: Int,
        athleteWeight: Double,
        athleteHeight: Int,
        experienceLevel: String,
        trainingGoal: String,
        diasDisponiveis: Int,
        volumeSemanalAtual: Int,
        tipoBike: String?,
        ftpAtual: Int?,
        potenciaMediaAtual: Int?,
        melhorTempo40km: String?,
        melhorTempo100km: String?,
        melhorTempo160km: String?,
        tempoObjetivo: String?,
        dataProva: String?,
        historicoLesoes: String?,
        experienciaAnterior: String?,
        preferenciaTreino: String?,
        equipamentosDisponiveis: String?,
        zonaTreinoPreferida: String?,
        planDuration: Int,
        planContent: String,
        excelFilePath: String,
        pdfFilePath: String,
        status: PlanStatus,
        createdAt: LocalDateTime = LocalDateTime.now(),
        user: User? = null,
        startDate: LocalDate?,
        endDate: LocalDate?
    ) {
        this.planId = planId
        this.athleteName = athleteName
        this.athleteAge = athleteAge
        this.athleteWeight = athleteWeight
        this.athleteHeight = athleteHeight
        this.experienceLevel = experienceLevel
        this.trainingGoal = trainingGoal
        this.diasDisponiveis = diasDisponiveis
        this.volumeSemanalAtual = volumeSemanalAtual
        this.tipoBike = tipoBike
        this.ftpAtual = ftpAtual
        this.potenciaMediaAtual = potenciaMediaAtual
        this.melhorTempo40km = melhorTempo40km
        this.melhorTempo100km = melhorTempo100km
        this.melhorTempo160km = melhorTempo160km
        this.tempoObjetivo = tempoObjetivo
        this.dataProva = dataProva
        this.historicoLesoes = historicoLesoes
        this.experienciaAnterior = experienciaAnterior
        this.preferenciaTreino = preferenciaTreino
        this.equipamentosDisponiveis = equipamentosDisponiveis
        this.zonaTreinoPreferida = zonaTreinoPreferida
        this.planDuration = planDuration
        this.planContent = planContent
        this.excelFilePath = excelFilePath
        this.pdfFilePath = pdfFilePath
        this.status = status
        this.createdAt = createdAt
        this.user = user
        this.startDate = startDate
        this.endDate = endDate
    }

    // Construtor padrão
    constructor()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BikeTrainingPlan

        if (planId != other.planId) return false

        return true
    }

    override fun hashCode(): Int {
        return planId.hashCode()
    }
}
