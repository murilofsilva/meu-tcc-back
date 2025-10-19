package br.edu.ufms.schoollab_manager.domain.entity

import br.edu.ufms.schoollab_manager.domain.enums.StatusPlanejamento
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "planejamento_status_hist",
    indexes = [
        Index(name = "ix_pl_status_hist_planejamento", columnList = "id_planejamento,criado_em")
    ]
)
class PlanejamentoStatusHist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_planejamento", nullable = false)
    val planejamento: Planejamento,

    @Enumerated(EnumType.STRING)
    @Column(name = "status_de", nullable = false)
    val statusDe: StatusPlanejamento,

    @Enumerated(EnumType.STRING)
    @Column(name = "status_para", nullable = false)
    val statusPara: StatusPlanejamento,

    @Column(name = "criado_em", nullable = false, updatable = false)
    val criadoEm: Instant = Instant.now()
) {
    init {
        require(statusDe != statusPara) { "Status de origem e destino devem ser diferentes" }
    }
}
