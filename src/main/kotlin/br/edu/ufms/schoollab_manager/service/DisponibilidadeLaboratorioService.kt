package br.edu.ufms.schoollab_manager.service

import br.edu.ufms.schoollab_manager.domain.entity.DisponibilidadeLaboratorio
import br.edu.ufms.schoollab_manager.domain.entity.Indisponibilidade
import br.edu.ufms.schoollab_manager.dto.CreateDisponibilidadeRequest
import br.edu.ufms.schoollab_manager.dto.CreateIndisponibilidadeRequest
import br.edu.ufms.schoollab_manager.dto.DisponibilidadeDTO
import br.edu.ufms.schoollab_manager.dto.IndisponibilidadeDTO
import br.edu.ufms.schoollab_manager.exception.ResourceNotFoundException
import br.edu.ufms.schoollab_manager.exception.ValidationException
import br.edu.ufms.schoollab_manager.repository.DisponibilidadeLaboratorioRepository
import br.edu.ufms.schoollab_manager.repository.IndisponibilidadeRepository
import br.edu.ufms.schoollab_manager.repository.LaboratorioRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class DisponibilidadeLaboratorioService(
    private val disponibilidadeRepository: DisponibilidadeLaboratorioRepository,
    private val indisponibilidadeRepository: IndisponibilidadeRepository,
    private val laboratorioRepository: LaboratorioRepository
) {

    @Transactional
    fun substituirDisponibilidades(
        laboratorioId: Long,
        novas: List<CreateDisponibilidadeRequest>
    ): List<DisponibilidadeDTO> {
        val laboratorio = laboratorioRepository.findById(laboratorioId)
            .orElseThrow { ResourceNotFoundException("Laboratório não encontrado") }

        novas.forEach {
            if (!it.horaFim.isAfter(it.horaInicio)) {
                throw ValidationException("Hora fim deve ser posterior à hora início em todas as janelas")
            }
        }
        novas.groupBy { it.diaSemana }.forEach { (dia, doDia) ->
            val ordenadas = doDia.sortedBy { it.horaInicio }
            for (i in 0 until ordenadas.size - 1) {
                if (ordenadas[i + 1].horaInicio.isBefore(ordenadas[i].horaFim)) {
                    throw ValidationException("Há sobreposição de horários em $dia")
                }
            }
        }

        disponibilidadeRepository.deleteByLaboratorioId(laboratorioId)

        val salvas = novas.map {
            DisponibilidadeLaboratorio(
                laboratorio = laboratorio,
                diaSemana = it.diaSemana,
                horaInicio = it.horaInicio,
                horaFim = it.horaFim
            )
        }.map { disponibilidadeRepository.save(it) }

        return salvas.map { DisponibilidadeDTO.fromEntity(it) }
    }

    fun listarDisponibilidades(laboratorioId: Long): List<DisponibilidadeDTO> {
        if (!laboratorioRepository.existsById(laboratorioId)) {
            throw ResourceNotFoundException("Laboratório não encontrado")
        }
        return disponibilidadeRepository
            .findByLaboratorioIdOrderByDiaSemanaAscHoraInicioAsc(laboratorioId)
            .map { DisponibilidadeDTO.fromEntity(it) }
    }

    fun criarIndisponibilidade(
        laboratorioId: Long,
        request: CreateIndisponibilidadeRequest
    ): IndisponibilidadeDTO {
        val laboratorio = laboratorioRepository.findById(laboratorioId)
            .orElseThrow { ResourceNotFoundException("Laboratório não encontrado") }

        if (!request.fim.isAfter(request.inicio)) {
            throw ValidationException("Data fim deve ser posterior à data início")
        }

        val indisponibilidade = Indisponibilidade(
            laboratorio = laboratorio,
            inicio = request.inicio,
            fim = request.fim,
            motivo = request.motivo.trim()
        )
        return IndisponibilidadeDTO.fromEntity(indisponibilidadeRepository.save(indisponibilidade))
    }

    fun listarIndisponibilidades(laboratorioId: Long): List<IndisponibilidadeDTO> {
        if (!laboratorioRepository.existsById(laboratorioId)) {
            throw ResourceNotFoundException("Laboratório não encontrado")
        }
        return indisponibilidadeRepository
            .findByLaboratorioIdOrderByInicioAsc(laboratorioId)
            .map { IndisponibilidadeDTO.fromEntity(it) }
    }

    fun removerIndisponibilidade(id: Long) {
        if (!indisponibilidadeRepository.existsById(id)) {
            throw ResourceNotFoundException("Bloqueio não encontrado")
        }
        indisponibilidadeRepository.deleteById(id)
    }

    // Sem janelas cadastradas: horário irrestrito (compat. com laboratórios pré-existentes).
    fun validarReservaCabeNasJanelas(
        laboratorioId: Long,
        inicio: Instant,
        fim: Instant,
        zoneId: ZoneId = ZoneId.of("America/Cuiaba")
    ) {
        val disp = disponibilidadeRepository
            .findByLaboratorioIdOrderByDiaSemanaAscHoraInicioAsc(laboratorioId)

        if (disp.isNotEmpty()) {
            val inicioLocal = LocalDateTime.ofInstant(inicio, zoneId)
            val fimLocal = LocalDateTime.ofInstant(fim, zoneId)

            if (inicioLocal.toLocalDate() != fimLocal.toLocalDate()) {
                throw ValidationException(
                    "Reservas devem começar e terminar no mesmo dia"
                )
            }

            val diaSemana = inicioLocal.dayOfWeek
            val horaIni = inicioLocal.toLocalTime()
            val horaFim = fimLocal.toLocalTime()

            val cabe = disp.any {
                it.diaSemana == diaSemana &&
                        !horaIni.isBefore(it.horaInicio) &&
                        !horaFim.isAfter(it.horaFim)
            }
            if (!cabe) {
                throw ValidationException(
                    "O laboratório não atende neste horário. Confira a agenda semanal."
                )
            }
        }

        if (indisponibilidadeRepository.existsConflito(laboratorioId, inicio, fim)) {
            throw ValidationException(
                "O laboratório está bloqueado neste período. Selecione outra data."
            )
        }
    }
}
