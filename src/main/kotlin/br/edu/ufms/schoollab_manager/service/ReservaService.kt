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

/**
 * Service responsável pela lógica de negócio relacionada a reservas
 */
@Service
class ReservaService(
    private val reservaRepository: ReservaRepository,
    private val laboratorioRepository: LaboratorioRepository,
    private val usuarioRepository: UsuarioRepository,
    private val planejamentoRepository: PlanejamentoRepository
) {

    /**
     * Cria uma nova reserva
     */
    fun criarReserva(request: CreateReservaRequest, emailProfessor: String): ReservaDTO {
        // Validação: fim deve ser posterior ao início
        if (!request.fim.isAfter(request.inicio)) {
            throw ValidationException("Data de fim deve ser posterior à data de início")
        }

        // Validação: período deve estar no futuro
        val agora = Instant.now()
        if (request.inicio.isBefore(agora)) {
            throw ValidationException("Não é possível criar reservas no passado")
        }

        // Busca o laboratório
        val laboratorio = laboratorioRepository.findById(request.laboratorioId)
            .orElseThrow { ResourceNotFoundException("Laboratório não encontrado") }

        // Verifica se o laboratório está ativo
        if (!laboratorio.status) {
            throw ValidationException("Laboratório está inativo")
        }

        // Busca o professor
        val professor = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        // Verifica conflito de horário
        val existeConflito = reservaRepository.existsConflito(
            laboratorioId = request.laboratorioId,
            inicio = request.inicio,
            fim = request.fim
        )

        if (existeConflito) {
            throw ConflictException("Já existe uma reserva neste horário para este laboratório")
        }

        // Busca o planejamento, se fornecido
        val planejamento = request.planejamentoId?.let {
            planejamentoRepository.findById(it).orElse(null)
        }

        // Cria a reserva
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

    /**
     * Lista reservas de acordo com o perfil do usuário
     */
    fun listarReservas(emailUsuario: String, status: StatusReserva?): List<ReservaDTO> {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        val reservas = when {
            // Se é professor, lista apenas suas reservas
            usuario.perfil == PerfilUsuario.PROFESSOR -> {
                if (status != null) {
                    reservaRepository.findByStatusOrderByCriadoEmDesc(status)
                        .filter { it.professor.id == usuario.id }
                } else {
                    reservaRepository.findByProfessorIdOrderByCriadoEmDesc(usuario.id!!)
                }
            }
            // Se é diretor ou admin, lista todas
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

    /**
     * Lista reservas pendentes (para aprovação)
     */
    fun listarReservasPendentes(): List<ReservaDTO> {
        val reservas = reservaRepository.findByStatusOrderByCriadoEmDesc(StatusReserva.PENDENTE)
        return reservas.map { ReservaDTO.fromEntity(it) }
    }

    /**
     * Busca uma reserva por ID, validando permissões
     */
    fun buscarReserva(id: Long, emailUsuario: String): ReservaDTO {
        val reserva = reservaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Reserva não encontrada") }

        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        // Professor só pode ver suas próprias reservas
        if (usuario.perfil == PerfilUsuario.PROFESSOR && reserva.professor.id != usuario.id) {
            throw ForbiddenException("Você não tem permissão para visualizar esta reserva")
        }

        return ReservaDTO.fromEntity(reserva)
    }

    /**
     * Busca reservas de um laboratório em um período
     */
    fun buscarReservasPorLaboratorio(laboratorioId: Long, inicio: Instant, fim: Instant): List<ReservaDTO> {
        // Verifica se o laboratório existe
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

    /**
     * Atualiza uma reserva
     */
    fun atualizarReserva(id: Long, request: UpdateReservaRequest, emailProfessor: String): ReservaDTO {
        val reserva = reservaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Reserva não encontrada") }

        val usuario = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        // Verifica se é o professor que criou
        if (reserva.professor.id != usuario.id) {
            throw ForbiddenException("Você não tem permissão para atualizar esta reserva")
        }

        // Verifica se a reserva pode ser atualizada
        if (reserva.status !in listOf(StatusReserva.PENDENTE, StatusReserva.AGUARDANDO_AJUSTES)) {
            throw ValidationException("Apenas reservas pendentes ou aguardando ajustes podem ser atualizadas")
        }

        // Validação: fim deve ser posterior ao início
        if (!request.fim.isAfter(request.inicio)) {
            throw ValidationException("Data de fim deve ser posterior à data de início")
        }

        // Validação: período deve estar no futuro
        val agora = Instant.now()
        if (request.inicio.isBefore(agora)) {
            throw ValidationException("Não é possível criar reservas no passado")
        }

        // Verifica conflito de horário (excluindo a própria reserva)
        val existeConflito = reservaRepository.existsConflito(
            laboratorioId = reserva.laboratorio.id!!,
            inicio = request.inicio,
            fim = request.fim,
            excludeId = id
        )

        if (existeConflito) {
            throw ConflictException("Já existe uma reserva neste horário para este laboratório")
        }

        // Atualiza os campos
        reserva.inicio = request.inicio
        reserva.fim = request.fim
        reserva.titulo = request.titulo
        reserva.turma = request.turma
        reserva.descricao = request.descricao

        // Se estava aguardando ajustes, volta para pendente
        if (reserva.status == StatusReserva.AGUARDANDO_AJUSTES) {
            reserva.status = StatusReserva.PENDENTE
            reserva.motivoStatus = null
        }

        val reservaAtualizada = reservaRepository.save(reserva)
        return ReservaDTO.fromEntity(reservaAtualizada)
    }

    /**
     * Altera o status de uma reserva (aprovar/reprovar/solicitar ajustes)
     */
    fun alterarStatus(id: Long, request: AlterarStatusReservaRequest): ReservaDTO {
        val reserva = reservaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Reserva não encontrada") }

        // Validação: não pode alterar status de reserva cancelada
        if (reserva.status == StatusReserva.CANCELADO) {
            throw ValidationException("Não é possível alterar status de reserva cancelada")
        }

        // Validação: ao reprovar, motivo é obrigatório
        if (request.status == StatusReserva.REPROVADO && request.motivo.isNullOrBlank()) {
            throw ValidationException("Motivo é obrigatório ao reprovar uma reserva")
        }

        // Validação: ao solicitar ajustes, motivo é obrigatório
        if (request.status == StatusReserva.AGUARDANDO_AJUSTES && request.motivo.isNullOrBlank()) {
            throw ValidationException("Motivo é obrigatório ao solicitar ajustes")
        }

        // Validação: ao aprovar, verifica conflito de horário
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

        // Atualiza o status
        reserva.status = request.status
        reserva.motivoStatus = request.motivo

        val reservaAtualizada = reservaRepository.save(reserva)
        return ReservaDTO.fromEntity(reservaAtualizada)
    }

    /**
     * Cancela uma reserva
     */
    fun cancelarReserva(id: Long, emailUsuario: String) {
        val reserva = reservaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Reserva não encontrada") }

        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        // Professor só pode cancelar suas próprias reservas
        if (usuario.perfil == PerfilUsuario.PROFESSOR && reserva.professor.id != usuario.id) {
            throw ForbiddenException("Você não tem permissão para cancelar esta reserva")
        }

        // Marca como cancelada ao invés de deletar
        reserva.status = StatusReserva.CANCELADO
        reservaRepository.save(reserva)
    }
}
