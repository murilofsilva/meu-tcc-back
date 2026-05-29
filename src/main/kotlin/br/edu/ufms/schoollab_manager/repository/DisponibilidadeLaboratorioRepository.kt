package br.edu.ufms.schoollab_manager.repository

import br.edu.ufms.schoollab_manager.domain.entity.DisponibilidadeLaboratorio
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.DayOfWeek

@Repository
interface DisponibilidadeLaboratorioRepository : JpaRepository<DisponibilidadeLaboratorio, Long> {
    fun findByLaboratorioIdOrderByDiaSemanaAscHoraInicioAsc(laboratorioId: Long): List<DisponibilidadeLaboratorio>
    fun findByLaboratorioIdAndDiaSemana(laboratorioId: Long, diaSemana: DayOfWeek): List<DisponibilidadeLaboratorio>
    fun deleteByLaboratorioId(laboratorioId: Long)
}
