package br.edu.ufms.schoollab_manager.domain.entity

import br.edu.ufms.schoollab_manager.domain.enums.StatusReserva
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "reserva_status_hist",
    indexes = [
        Index(name = "ix_res_status_hist_reserva", columnList = "reserva_id,criado_em")
    ]
)
class ReservaStatusHist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    val reserva: Reserva,

    @Enumerated(EnumType.STRING)
    @Column(name = "status_de", nullable = false)
    val statusDe: StatusReserva,

    @Enumerated(EnumType.STRING)
    @Column(name = "status_para", nullable = false)
    val statusPara: StatusReserva,

    @Column(name = "criado_em", nullable = false, updatable = false)
    val criadoEm: Instant = Instant.now()
) {
    init {
        require(statusDe != statusPara) { "Status de origem e destino devem ser diferentes" }
    }
}
