package br.edu.ufms.schoollab_manager.domain.entity

import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalTime

@Entity
@Table(
    name = "disponibilidades_laboratorio",
    indexes = [
        Index(name = "ix_disp_lab", columnList = "laboratorio_id"),
        Index(name = "ix_disp_lab_dia", columnList = "laboratorio_id,dia_semana")
    ]
)
class DisponibilidadeLaboratorio(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratorio_id", nullable = false)
    var laboratorio: Laboratorio,

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 12)
    var diaSemana: DayOfWeek,

    @Column(name = "hora_inicio", nullable = false)
    var horaInicio: LocalTime,

    @Column(name = "hora_fim", nullable = false)
    var horaFim: LocalTime
) {
    init {
        require(horaFim.isAfter(horaInicio)) { "Hora fim deve ser posterior à hora início" }
    }
}
