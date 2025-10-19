package br.edu.ufms.schoollab_manager.domain.entity

import br.edu.ufms.schoollab_manager.domain.enums.StatusReserva
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "reservas",
    indexes = [
        Index(name = "ix_reservas_lab", columnList = "laboratorio_id"),
        Index(name = "ix_reservas_prof", columnList = "professor_id"),
        Index(name = "ix_reservas_status", columnList = "status"),
        Index(name = "ix_reservas_periodo", columnList = "inicio,fim")
    ]
)
class Reserva(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratorio_id", nullable = false)
    var laboratorio: Laboratorio,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    var professor: Usuario,

    @Column(nullable = false)
    var inicio: Instant,

    @Column(nullable = false)
    var fim: Instant,

    @Column(nullable = false, length = 160)
    var titulo: String,

    @Column(length = 80)
    var turma: String? = null,

    @Column(columnDefinition = "TEXT")
    var descricao: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planejamento_id")
    var planejamento: Planejamento? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StatusReserva = StatusReserva.PENDENTE,

    @Column(name = "motivo_status", columnDefinition = "TEXT")
    var motivoStatus: String? = null,

    @Column(name = "criado_em", nullable = false, updatable = false)
    val criadoEm: Instant = Instant.now()
) {
    init {
        require(fim.isAfter(inicio)) { "Data fim deve ser posterior à data início" }
    }
}
