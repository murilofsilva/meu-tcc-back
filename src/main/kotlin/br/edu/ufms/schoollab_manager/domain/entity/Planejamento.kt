package br.edu.ufms.schoollab_manager.domain.entity

import br.edu.ufms.schoollab_manager.domain.enums.StatusPlanejamento
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "planejamentos",
    indexes = [
        Index(name = "ix_planejamentos_author", columnList = "author_id"),
        Index(name = "ix_planejamentos_status", columnList = "status")
    ]
)
class Planejamento(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    var author: Usuario,

    @Column(nullable = false, length = 160)
    var titulo: String,

    @Column(nullable = false, length = 120)
    var area: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var descricao: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StatusPlanejamento = StatusPlanejamento.PENDENTE,

    @Column(nullable = false)
    var versao: Int = 1,

    @Column(name = "criado_em", nullable = false, updatable = false)
    val criadoEm: Instant = Instant.now()
) {
    init {
        require(versao >= 1) { "Versão deve ser maior ou igual a 1" }
    }
}
