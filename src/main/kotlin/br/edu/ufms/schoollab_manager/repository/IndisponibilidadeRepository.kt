package br.edu.ufms.schoollab_manager.repository

import br.edu.ufms.schoollab_manager.domain.entity.Indisponibilidade
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface IndisponibilidadeRepository : JpaRepository<Indisponibilidade, Long> {

    fun findByLaboratorioIdOrderByInicioAsc(laboratorioId: Long): List<Indisponibilidade>

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM Indisponibilidade i
        WHERE i.laboratorio.id = :laboratorioId
        AND (i.inicio < :fim AND i.fim > :inicio)
    """)
    fun existsConflito(
        @Param("laboratorioId") laboratorioId: Long,
        @Param("inicio") inicio: Instant,
        @Param("fim") fim: Instant
    ): Boolean
}
