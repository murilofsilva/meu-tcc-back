package br.edu.ufms.schoollab_manager.service

import br.edu.ufms.schoollab_manager.domain.entity.Reserva
import br.edu.ufms.schoollab_manager.domain.enums.PerfilUsuario
import br.edu.ufms.schoollab_manager.domain.enums.StatusReserva
import br.edu.ufms.schoollab_manager.dto.AlterarStatusReservaRequest
import br.edu.ufms.schoollab_manager.dto.CreateReservaRequest
import br.edu.ufms.schoollab_manager.dto.ReservaDTO
import br.edu.ufms.schoollab_manager.dto.UpdateReservaRequest
import br.edu.ufms.schoollab_manager.exception.ConflictException
import br.edu.ufms.schoollab_manager.exception.ForbiddenException
import br.edu.ufms.schoollab_manager.exception.ResourceNotFoundException
import br.edu.ufms.schoollab_manager.exception.ValidationException
import br.edu.ufms.schoollab_manager.repository.LaboratorioRepository
import br.edu.ufms.schoollab_manager.repository.PlanejamentoRepository
import br.edu.ufms.schoollab_manager.repository.ReservaRepository
import br.edu.ufms.schoollab_manager.repository.UsuarioRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
class ReservaService(
    private val reservaRepository: ReservaRepository,
    private val laboratorioRepository: LaboratorioRepository,
    private val usuarioRepository: UsuarioRepository,
    private val planejamentoRepository: PlanejamentoRepository,
    private val disponibilidadeService: DisponibilidadeLaboratorioService
) {
    private val zonaLocal: ZoneId = ZoneId.of("America/Cuiaba")

    fun criarReserva(request: CreateReservaRequest, emailProfessor: String): ReservaDTO {
        validarPeriodoReserva(request.inicio, request.fim)

        val laboratorio = laboratorioRepository.findById(request.laboratorioId)
            .orElseThrow { ResourceNotFoundException("Laboratório não encontrado") }

        if (!laboratorio.status) {
            throw ValidationException("Laboratório está inativo")
        }

        disponibilidadeService.validarReservaCabeNasJanelas(
            laboratorioId = laboratorio.id!!,
            inicio = request.inicio,
            fim = request.fim,
            zoneId = zonaLocal
        )

        val professor = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        val existeConflito = reservaRepository.existsConflito(
            laboratorioId = request.laboratorioId,
            inicio = request.inicio,
            fim = request.fim
        )

        if (existeConflito) {
            throw ConflictException("Já existe uma reserva neste horário para este laboratório")
        }

        val planejamento = request.planejamentoId?.let {
            planejamentoRepository.findById(it).orElse(null)
        }

        val reserva = Reserva(
            laboratorio = laboratorio,
            professor = professor,
            inicio = request.inicio,
            fim = request.fim,
            titulo = request.titulo,
            turma = request.turma,
            descricao = request.descricao,
            planejamento = planejamento,
            status = StatusReserva.PENDENTE
        )

        val reservaSalva = reservaRepository.save(reserva)
        return ReservaDTO.fromEntity(reservaSalva)
    }

    fun listarReservas(emailUsuario: String, status: StatusReserva?): List<ReservaDTO> {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        val reservas = when {
            usuario.perfil == PerfilUsuario.PROFESSOR -> {
                if (status != null) {
                    reservaRepository.findByStatusOrderByCriadoEmDesc(status)
                        .filter { it.professor.id == usuario.id }
                } else {
                    reservaRepository.findByProfessorIdOrderByCriadoEmDesc(usuario.id!!)
                }
            }
            else -> {
                if (status != null) {
                    reservaRepository.findByStatusOrderByCriadoEmDesc(status)
                } else {
                    reservaRepository.findAllByOrderByCriadoEmDesc()
                }
            }
        }

        return reservas.map { ReservaDTO.fromEntity(it) }
    }

    fun listarReservasPendentes(): List<ReservaDTO> {
        val reservas = reservaRepository.findByStatusOrderByCriadoEmDesc(StatusReserva.PENDENTE)
        return reservas.map { ReservaDTO.fromEntity(it) }
    }

    fun buscarReserva(id: Long, emailUsuario: String): ReservaDTO {
        val reserva = reservaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Reserva não encontrada") }

        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        if (usuario.perfil == PerfilUsuario.PROFESSOR && reserva.professor.id != usuario.id) {
            throw ForbiddenException("Você não tem permissão para visualizar esta reserva")
        }

        return ReservaDTO.fromEntity(reserva)
    }

    fun buscarReservasPorLaboratorio(laboratorioId: Long, inicio: Instant, fim: Instant): List<ReservaDTO> {
        if (!laboratorioRepository.existsById(laboratorioId)) {
            throw ResourceNotFoundException("Laboratório não encontrado")
        }

        val reservas = reservaRepository.findByLaboratorioAndPeriodo(
            laboratorioId = laboratorioId,
            inicio = inicio,
            fim = fim
        )

        return reservas.map { ReservaDTO.fromEntity(it) }
    }

    fun atualizarReserva(id: Long, request: UpdateReservaRequest, emailProfessor: String): ReservaDTO {
        val reserva = reservaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Reserva não encontrada") }

        val usuario = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        if (reserva.professor.id != usuario.id) {
            throw ForbiddenException("Você não tem permissão para atualizar esta reserva")
        }

        if (reserva.status !in listOf(StatusReserva.PENDENTE, StatusReserva.AGUARDANDO_AJUSTES)) {
            throw ValidationException("Apenas reservas pendentes ou aguardando ajustes podem ser atualizadas")
        }

        validarPeriodoReserva(request.inicio, request.fim)

        disponibilidadeService.validarReservaCabeNasJanelas(
            laboratorioId = reserva.laboratorio.id!!,
            inicio = request.inicio,
            fim = request.fim,
            zoneId = zonaLocal
        )

        val existeConflito = reservaRepository.existsConflito(
            laboratorioId = reserva.laboratorio.id!!,
            inicio = request.inicio,
            fim = request.fim,
            excludeId = id
        )

        if (existeConflito) {
            throw ConflictException("Já existe uma reserva neste horário para este laboratório")
        }

        reserva.inicio = request.inicio
        reserva.fim = request.fim
        reserva.titulo = request.titulo
        reserva.turma = request.turma
        reserva.descricao = request.descricao

        if (reserva.status == StatusReserva.AGUARDANDO_AJUSTES) {
            reserva.status = StatusReserva.PENDENTE
            reserva.motivoStatus = null
        }

        val reservaAtualizada = reservaRepository.save(reserva)
        return ReservaDTO.fromEntity(reservaAtualizada)
    }

    fun alterarStatus(id: Long, request: AlterarStatusReservaRequest): ReservaDTO {
        val reserva = reservaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Reserva não encontrada") }

        if (reserva.status == StatusReserva.CANCELADO) {
            throw ValidationException("Não é possível alterar status de reserva cancelada")
        }

        if (request.status == StatusReserva.REPROVADO && request.motivo.isNullOrBlank()) {
            throw ValidationException("Motivo é obrigatório ao reprovar uma reserva")
        }

        if (request.status == StatusReserva.AGUARDANDO_AJUSTES && request.motivo.isNullOrBlank()) {
            throw ValidationException("Motivo é obrigatório ao solicitar ajustes")
        }

        if (request.status == StatusReserva.APROVADO) {
            val existeConflito = reservaRepository.existsConflito(
                laboratorioId = reserva.laboratorio.id!!,
                inicio = reserva.inicio,
                fim = reserva.fim,
                excludeId = id
            )

            if (existeConflito) {
                throw ConflictException("Não é possível aprovar: já existe outra reserva neste horário")
            }
        }

        reserva.status = request.status
        reserva.motivoStatus = request.motivo

        val reservaAtualizada = reservaRepository.save(reserva)
        return ReservaDTO.fromEntity(reservaAtualizada)
    }

    fun cancelarReserva(id: Long, emailUsuario: String) {
        val reserva = reservaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Reserva não encontrada") }

        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        if (usuario.perfil == PerfilUsuario.PROFESSOR && reserva.professor.id != usuario.id) {
            throw ForbiddenException("Você não tem permissão para cancelar esta reserva")
        }

        reserva.status = StatusReserva.CANCELADO
        reservaRepository.save(reserva)
    }

    fun listarReservasFuturas(emailProfessor: String): List<ReservaDTO> {
        val professor = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        val agora = Instant.now()

        val reservas = reservaRepository.findByProfessorIdOrderByCriadoEmDesc(professor.id!!)
            .filter { it.inicio.isAfter(agora) }

        return reservas.map { ReservaDTO.fromEntity(it) }
    }

    fun vincularPlanejamento(id: Long, planejamentoId: Long, emailProfessor: String): ReservaDTO {
        val reserva = reservaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Reserva não encontrada") }

        val professor = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        if (reserva.professor.id != professor.id) {
            throw ForbiddenException("Você não tem permissão para modificar esta reserva")
        }

        val planejamento = planejamentoRepository.findById(planejamentoId)
            .orElseThrow { ResourceNotFoundException("Planejamento não encontrado") }

        reserva.planejamento = planejamento

        val reservaAtualizada = reservaRepository.save(reserva)
        return ReservaDTO.fromEntity(reservaAtualizada)
    }

    private fun validarPeriodoReserva(inicio: Instant, fim: Instant) {
        if (!fim.isAfter(inicio)) {
            throw ValidationException("Data de fim deve ser posterior à data de início")
        }
        val hoje = LocalDate.now(zonaLocal)
        val diaInicio = inicio.atZone(zonaLocal).toLocalDate()
        if (diaInicio.isBefore(hoje)) {
            throw ValidationException("Não é possível criar reservas em datas anteriores a hoje")
        }
    }
}
