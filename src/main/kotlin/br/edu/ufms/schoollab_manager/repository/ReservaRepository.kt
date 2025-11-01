package br.edu.ufms.schoollab_manager.repository

import br.edu.ufms.schoollab_manager.domain.entity.Reserva
import br.edu.ufms.schoollab_manager.domain.enums.StatusReserva
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ReservaRepository : JpaRepository<Reserva, Long> {

    /**
     * Busca todas as reservas de um professor específico
     */
    fun findByProfessorIdOrderByCriadoEmDesc(professorId: Long): List<Reserva>

    /**
     * Busca todas as reservas de um laboratório específico
     */
    fun findByLaboratorioIdOrderByInicioDesc(laboratorioId: Long): List<Reserva>

    /**
     * Busca reservas por status
     */
    fun findByStatusOrderByCriadoEmDesc(status: StatusReserva): List<Reserva>

    /**
     * Busca reservas por múltiplos status
     */
    fun findByStatusInOrderByCriadoEmDesc(status: List<StatusReserva>): List<Reserva>

    /**
     * Verifica se existe conflito de horário para um laboratório
     * Considera apenas reservas APROVADAS, PENDENTES ou AGUARDANDO_AJUSTES
     */
    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM Reserva r
        WHERE r.laboratorio.id = :laboratorioId
        AND r.status IN ('APROVADO', 'PENDENTE', 'AGUARDANDO_AJUSTES')
        AND r.id != :excludeId
        AND (
            (r.inicio < :fim AND r.fim > :inicio)
        )
    """)
    fun existsConflito(
        @Param("laboratorioId") laboratorioId: Long,
        @Param("inicio") inicio: Instant,
        @Param("fim") fim: Instant,
        @Param("excludeId") excludeId: Long = 0
    ): Boolean

    /**
     * Busca reservas de um laboratório em um período específico
     */
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.laboratorio.id = :laboratorioId
        AND r.status IN ('APROVADO', 'PENDENTE', 'AGUARDANDO_AJUSTES')
        AND (r.inicio < :fim AND r.fim > :inicio)
        ORDER BY r.inicio ASC
    """)
    fun findByLaboratorioAndPeriodo(
        @Param("laboratorioId") laboratorioId: Long,
        @Param("inicio") inicio: Instant,
        @Param("fim") fim: Instant
    ): List<Reserva>

    /**
     * Busca próximas reservas de um professor
     */
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.professor.id = :professorId
        AND r.status = 'APROVADO'
        AND r.inicio >= :agora
        ORDER BY r.inicio ASC
    """)
    fun findProximasReservasProfessor(
        @Param("professorId") professorId: Long,
        @Param("agora") agora: Instant = Instant.now()
    ): List<Reserva>

    /**
     * Lista todas as reservas ordenadas por data de criação
     */
    fun findAllByOrderByCriadoEmDesc(): List<Reserva>
}
