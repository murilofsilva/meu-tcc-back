package br.edu.ufms.schoollab_manager.repository

import br.edu.ufms.schoollab_manager.domain.entity.Planejamento
import br.edu.ufms.schoollab_manager.domain.enums.StatusPlanejamento
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PlanejamentoRepository : JpaRepository<Planejamento, Long> {

    fun findByStatusOrderByCriadoEmDesc(status: StatusPlanejamento): List<Planejamento>

    fun findByStatus(status: StatusPlanejamento): List<Planejamento>

    fun findByAuthorIdOrderByCriadoEmDesc(authorId: Long): List<Planejamento>

    fun findByAuthorId(authorId: Long): List<Planejamento>

    fun findAllByOrderByCriadoEmDesc(): List<Planejamento>

    fun findByCodigo(codigo: String): Optional<Planejamento>

    fun existsByCodigo(codigo: String): Boolean
}
