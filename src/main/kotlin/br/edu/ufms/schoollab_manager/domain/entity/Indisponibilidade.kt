package br.edu.ufms.schoollab_manager.domain.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "indisponibilidades")
class Indisponibilidade(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratorio_id", nullable = false)
    var laboratorio: Laboratorio,

    @Column(nullable = false)
    var inicio: Instant,

    @Column(nullable = false)
    var fim: Instant,

    @Column(nullable = false, columnDefinition = "TEXT")
    var motivo: String
) {
    init {
        require(fim.isAfter(inicio)) { "Data fim deve ser posterior à data início" }
    }
}
