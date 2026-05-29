package br.edu.ufms.schoollab_manager.domain.entity

import br.edu.ufms.schoollab_manager.domain.enums.StatusPlanejamento
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "planejamentos",
    indexes = [
        Index(name = "ix_planejamentos_author", columnList = "author_id"),
        Index(name = "ix_planejamentos_status", columnList = "status"),
        Index(name = "ix_planejamentos_codigo", columnList = "codigo", unique = true),
        Index(name = "ix_planejamentos_area_conhec", columnList = "area_conhecimento_id")
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

    @Column(length = 12, unique = true)
    var codigo: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_conhecimento_id")
    var areaConhecimento: AreaConhecimento? = null,

    // Coluna legada mantida como cache denormalizado para preservar dados existentes;
    // fonte da verdade é areaConhecimento.
    @Column(nullable = false, length = 120)
    var area: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var descricao: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StatusPlanejamento = StatusPlanejamento.PENDENTE,

    @Column(nullable = false)
    var versao: Int = 1,

    @Column(nullable = false)
    var publico: Boolean = false,

    @Column(name = "mobiliza_comp_computacao", nullable = false)
    var mobilizaCompetenciasComputacao: Boolean = false,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "planejamento_competencias_computacao",
        joinColumns = [JoinColumn(name = "planejamento_id")]
    )
    @Column(name = "competencia", length = 60, nullable = false)
    var competenciasComputacao: MutableSet<String> = mutableSetOf(),

    @Column(name = "utiliza_recursos_acessibilidade", nullable = false)
    var utilizaRecursosAcessibilidade: Boolean = false,

    @Column(name = "descricao_recursos_acessibilidade", columnDefinition = "TEXT")
    var descricaoRecursosAcessibilidade: String? = null,

    @Column(name = "criado_em", nullable = false, updatable = false)
    val criadoEm: Instant = Instant.now()
) {
    init {
        require(versao >= 1) { "Versão deve ser maior ou igual a 1" }
    }
}
