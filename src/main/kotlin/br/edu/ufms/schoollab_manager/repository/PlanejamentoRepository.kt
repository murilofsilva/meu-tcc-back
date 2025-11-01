package br.edu.ufms.schoollab_manager.repository

import br.edu.ufms.schoollab_manager.domain.entity.Planejamento
import br.edu.ufms.schoollab_manager.domain.enums.StatusPlanejamento
import org.springframework.data.jpa.repository.JpaRepository

interface PlanejamentoRepository : JpaRepository<Planejamento, Long> {

    /**
     * Busca planejamentos por status
     */
    fun findByStatusOrderByCriadoEmDesc(status: StatusPlanejamento): List<Planejamento>

    /**
     * Busca planejamentos por autor
     */
    fun findByAuthorIdOrderByCriadoEmDesc(authorId: Long): List<Planejamento>

    /**
     * Lista todos os planejamentos ordenados por data de criação
     */
    fun findAllByOrderByCriadoEmDesc(): List<Planejamento>
}
