package br.edu.ufms.schoollab_manager.repository

import br.edu.ufms.schoollab_manager.domain.entity.AreaConhecimento
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AreaConhecimentoRepository : JpaRepository<AreaConhecimento, Long> {
    fun findByNome(nome: String): Optional<AreaConhecimento>
    fun existsByNome(nome: String): Boolean
    fun findByAtivoOrderByNomeAsc(ativo: Boolean): List<AreaConhecimento>
    fun findAllByOrderByNomeAsc(): List<AreaConhecimento>
}
