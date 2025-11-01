package br.edu.ufms.schoollab_manager.controller

import br.edu.ufms.schoollab_manager.domain.enums.StatusReserva
import br.edu.ufms.schoollab_manager.dto.AlterarStatusReservaRequest
import br.edu.ufms.schoollab_manager.dto.CreateReservaRequest
import br.edu.ufms.schoollab_manager.dto.ReservaDTO
import br.edu.ufms.schoollab_manager.dto.UpdateReservaRequest
import br.edu.ufms.schoollab_manager.service.ReservaService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * Controller responsável por receber e devolver requisições relacionadas a reservas.
 * Toda a lógica de negócio é delegada ao ReservaService.
 */
@RestController
@RequestMapping("/api/reservas")
class ReservaController(
    private val reservaService: ReservaService
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
    ): ResponseEntity<ReservaDTO> {
        val reservaDTO = reservaService.criarReserva(request, authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaDTO)
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
        val reservas = reservaService.listarReservas(authentication.name, status)
        return ResponseEntity.ok(reservas)
    }

    /**
     * Lista reservas pendentes (para aprovação)
     * Apenas DIRETOR e ADMIN podem acessar
     */
    @GetMapping("/pendentes")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun listarReservasPendentes(): ResponseEntity<List<ReservaDTO>> {
        val reservas = reservaService.listarReservasPendentes()
        return ResponseEntity.ok(reservas)
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
    ): ResponseEntity<ReservaDTO> {
        val reservaDTO = reservaService.buscarReserva(id, authentication.name)
        return ResponseEntity.ok(reservaDTO)
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
    ): ResponseEntity<List<ReservaDTO>> {
        val reservas = reservaService.buscarReservasPorLaboratorio(laboratorioId, inicio, fim)
        return ResponseEntity.ok(reservas)
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
    ): ResponseEntity<ReservaDTO> {
        val reservaDTO = reservaService.atualizarReserva(id, request, authentication.name)
        return ResponseEntity.ok(reservaDTO)
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
    ): ResponseEntity<ReservaDTO> {
        val reservaDTO = reservaService.alterarStatus(id, request)
        return ResponseEntity.ok(reservaDTO)
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
    ): ResponseEntity<Void> {
        reservaService.cancelarReserva(id, authentication.name)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
