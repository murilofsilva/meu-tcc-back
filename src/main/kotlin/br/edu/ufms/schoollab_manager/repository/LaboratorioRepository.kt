package br.edu.ufms.schoollab_manager.repository

import br.edu.ufms.schoollab_manager.domain.entity.Laboratorio
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LaboratorioRepository : JpaRepository<Laboratorio, Long> {
    fun findByNome(nome: String): Optional<Laboratorio>
    fun existsByNome(nome: String): Boolean
    fun findByStatusOrderByNomeAsc(status: Boolean): List<Laboratorio>
    fun findAllByOrderByNomeAsc(): List<Laboratorio>
}
