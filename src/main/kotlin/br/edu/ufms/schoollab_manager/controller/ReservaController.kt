package br.edu.ufms.schoollab_manager.controller

import br.edu.ufms.schoollab_manager.domain.entity.Reserva
import br.edu.ufms.schoollab_manager.domain.enums.PerfilUsuario
import br.edu.ufms.schoollab_manager.domain.enums.StatusReserva
import br.edu.ufms.schoollab_manager.dto.AlterarStatusReservaRequest
import br.edu.ufms.schoollab_manager.dto.CreateReservaRequest
import br.edu.ufms.schoollab_manager.dto.ReservaDTO
import br.edu.ufms.schoollab_manager.dto.UpdateReservaRequest
import br.edu.ufms.schoollab_manager.repository.LaboratorioRepository
import br.edu.ufms.schoollab_manager.repository.PlanejamentoRepository
import br.edu.ufms.schoollab_manager.repository.ReservaRepository
import br.edu.ufms.schoollab_manager.repository.UsuarioRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/reservas")
class ReservaController(
    private val reservaRepository: ReservaRepository,
    private val laboratorioRepository: LaboratorioRepository,
    private val usuarioRepository: UsuarioRepository,
    private val planejamentoRepository: PlanejamentoRepository
) {

    /**
     * Cria uma nova reserva
     * Apenas PROFESSOR pode criar reservas
     */
    @PostMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    fun criarReserva(
        @Valid @RequestBody request: CreateReservaRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        // Validação: fim deve ser posterior ao início
        if (!request.fim.isAfter(request.inicio)) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Data de fim deve ser posterior à data de início"))
        }

        // Validação: período deve estar no futuro
        val agora = Instant.now()
        if (request.inicio.isBefore(agora)) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Não é possível criar reservas no passado"))
        }

        // Busca o laboratório
        val laboratorio = laboratorioRepository.findById(request.laboratorioId)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Laboratório não encontrado"))

        // Verifica se o laboratório está ativo
        if (!laboratorio.status) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Laboratório está inativo"))
        }

        // Busca o professor (usuário autenticado)
        val email = authentication.name
        val professor = usuarioRepository.findByEmail(email)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Usuário não encontrado"))

        // Verifica conflito de horário
        val existeConflito = reservaRepository.existsConflito(
            laboratorioId = request.laboratorioId,
            inicio = request.inicio,
            fim = request.fim
        )

        if (existeConflito) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(mapOf("mensagem" to "Já existe uma reserva neste horário para este laboratório"))
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

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ReservaDTO.fromEntity(reservaSalva))
    }

    /**
     * Lista todas as reservas
     * PROFESSOR vê apenas as suas
     * DIRETOR e ADMIN veem todas
     */
    @GetMapping
    fun listarReservas(
        @RequestParam(required = false) status: StatusReserva?,
        authentication: Authentication
    ): ResponseEntity<List<ReservaDTO>> {
        val email = authentication.name
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

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

        val reservasDTO = reservas.map { ReservaDTO.fromEntity(it) }
        return ResponseEntity.ok(reservasDTO)
    }

    /**
     * Lista reservas pendentes (para aprovação)
     * Apenas DIRETOR e ADMIN podem acessar
     */
    @GetMapping("/pendentes")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun listarReservasPendentes(): ResponseEntity<List<ReservaDTO>> {
        val reservas = reservaRepository.findByStatusOrderByCriadoEmDesc(StatusReserva.PENDENTE)
        val reservasDTO = reservas.map { ReservaDTO.fromEntity(it) }
        return ResponseEntity.ok(reservasDTO)
    }

    /**
     * Busca uma reserva por ID
     * PROFESSOR pode ver apenas as suas
     * DIRETOR e ADMIN podem ver todas
     */
    @GetMapping("/{id}")
    fun buscarReserva(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val reserva = reservaRepository.findById(id)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Reserva não encontrada"))

        // Verifica permissão
        val email = authentication.name
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        // Professor só pode ver suas próprias reservas
        if (usuario.perfil == PerfilUsuario.PROFESSOR && reserva.professor.id != usuario.id) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(mapOf("mensagem" to "Você não tem permissão para visualizar esta reserva"))
        }

        return ResponseEntity.ok(ReservaDTO.fromEntity(reserva))
    }

    /**
     * Busca reservas de um laboratório em um período
     * Útil para visualizar agenda
     */
    @GetMapping("/laboratorio/{laboratorioId}")
    fun buscarReservasPorLaboratorio(
        @PathVariable laboratorioId: Long,
        @RequestParam inicio: Instant,
        @RequestParam fim: Instant
    ): ResponseEntity<Any> {
        // Verifica se o laboratório existe
        if (!laboratorioRepository.existsById(laboratorioId)) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Laboratório não encontrado"))
        }

        val reservas = reservaRepository.findByLaboratorioAndPeriodo(
            laboratorioId = laboratorioId,
            inicio = inicio,
            fim = fim
        )

        val reservasDTO = reservas.map { ReservaDTO.fromEntity(it) }
        return ResponseEntity.ok(reservasDTO)
    }

    /**
     * Atualiza uma reserva
     * Apenas o professor que criou pode atualizar
     * Apenas reservas PENDENTES ou AGUARDANDO_AJUSTES podem ser atualizadas
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    fun atualizarReserva(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateReservaRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val reserva = reservaRepository.findById(id)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Reserva não encontrada"))

        // Verifica se é o professor que criou
        val email = authentication.name
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        if (reserva.professor.id != usuario.id) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(mapOf("mensagem" to "Você não tem permissão para atualizar esta reserva"))
        }

        // Verifica se a reserva pode ser atualizada
        if (reserva.status !in listOf(StatusReserva.PENDENTE, StatusReserva.AGUARDANDO_AJUSTES)) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Apenas reservas pendentes ou aguardando ajustes podem ser atualizadas"))
        }

        // Validação: fim deve ser posterior ao início
        if (!request.fim.isAfter(request.inicio)) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Data de fim deve ser posterior à data de início"))
        }

        // Validação: período deve estar no futuro
        val agora = Instant.now()
        if (request.inicio.isBefore(agora)) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Não é possível criar reservas no passado"))
        }

        // Verifica conflito de horário (excluindo a própria reserva)
        val existeConflito = reservaRepository.existsConflito(
            laboratorioId = reserva.laboratorio.id!!,
            inicio = request.inicio,
            fim = request.fim,
            excludeId = id
        )

        if (existeConflito) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(mapOf("mensagem" to "Já existe uma reserva neste horário para este laboratório"))
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

        return ResponseEntity.ok(ReservaDTO.fromEntity(reservaAtualizada))
    }

    /**
     * Altera o status de uma reserva (aprovar/reprovar/solicitar ajustes)
     * Apenas DIRETOR e ADMIN podem alterar status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun alterarStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: AlterarStatusReservaRequest
    ): ResponseEntity<Any> {
        val reserva = reservaRepository.findById(id)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Reserva não encontrada"))

        // Validação: não pode alterar status de reserva cancelada
        if (reserva.status == StatusReserva.CANCELADO) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Não é possível alterar status de reserva cancelada"))
        }

        // Validação: ao reprovar, motivo é obrigatório
        if (request.status == StatusReserva.REPROVADO && request.motivo.isNullOrBlank()) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Motivo é obrigatório ao reprovar uma reserva"))
        }

        // Validação: ao solicitar ajustes, motivo é obrigatório
        if (request.status == StatusReserva.AGUARDANDO_AJUSTES && request.motivo.isNullOrBlank()) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Motivo é obrigatório ao solicitar ajustes"))
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
                return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(mapOf("mensagem" to "Não é possível aprovar: já existe outra reserva neste horário"))
            }
        }

        // Atualiza o status
        reserva.status = request.status
        reserva.motivoStatus = request.motivo

        val reservaAtualizada = reservaRepository.save(reserva)

        return ResponseEntity.ok(ReservaDTO.fromEntity(reservaAtualizada))
    }

    /**
     * Cancela uma reserva
     * Professor pode cancelar suas próprias
     * DIRETOR e ADMIN podem cancelar qualquer uma
     */
    @DeleteMapping("/{id}")
    fun cancelarReserva(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val reserva = reservaRepository.findById(id)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Reserva não encontrada"))

        // Verifica permissão
        val email = authentication.name
        val usuario = usuarioRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        // Professor só pode cancelar suas próprias reservas
        if (usuario.perfil == PerfilUsuario.PROFESSOR && reserva.professor.id != usuario.id) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(mapOf("mensagem" to "Você não tem permissão para cancelar esta reserva"))
        }

        // Marca como cancelada ao invés de deletar
        reserva.status = StatusReserva.CANCELADO
        reservaRepository.save(reserva)

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build()
    }
}
