package br.edu.ufms.schoollab_manager.controller

import br.edu.ufms.schoollab_manager.domain.enums.StatusPlanejamento
import br.edu.ufms.schoollab_manager.dto.AlterarStatusPlanejamentoRequest
import br.edu.ufms.schoollab_manager.dto.CreatePlanejamentoRequest
import br.edu.ufms.schoollab_manager.dto.PlanejamentoDTO
import br.edu.ufms.schoollab_manager.dto.UpdatePlanejamentoRequest
import br.edu.ufms.schoollab_manager.service.PlanejamentoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

/**
 * Controller responsável por receber e devolver requisições relacionadas a planejamentos de aula.
 * Toda a lógica de negócio é delegada ao PlanejamentoService.
 */
@RestController
@RequestMapping("/api/planejamentos")
class PlanejamentoController(
    private val planejamentoService: PlanejamentoService
) {

    /**
     * Cria um novo planejamento de aula
     * POST /api/planejamentos
     * Permissão: PROFESSOR, DIRETOR, ADMIN
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'DIRETOR', 'ADMIN')")
    fun criarPlanejamento(
        @Valid @RequestBody request: CreatePlanejamentoRequest,
        authentication: Authentication
    ): ResponseEntity<PlanejamentoDTO> {
        val planejamentoDTO = planejamentoService.criarPlanejamento(request, authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(planejamentoDTO)
    }

    /**
     * Lista planejamentos baseado no perfil:
     * - Professor: Planos publicados de todos + todos os seus planos
     * - Diretor/Admin: Todos os planos
     * GET /api/planejamentos?status=PUBLICADO
     * Permissão: Autenticado
     */
    @GetMapping
    fun listarPlanejamentos(
        @RequestParam(required = false) status: StatusPlanejamento?,
        authentication: Authentication
    ): ResponseEntity<List<PlanejamentoDTO>> {
        val planejamentos = planejamentoService.listarPlanejamentos(authentication.name, status)
        return ResponseEntity.ok(planejamentos)
    }

    /**
     * Lista apenas os planejamentos do professor autenticado
     * GET /api/planejamentos/meus
     * Permissão: PROFESSOR, DIRETOR, ADMIN
     */
    @GetMapping("/meus")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'DIRETOR', 'ADMIN')")
    fun listarMeusPlanejamentos(
        authentication: Authentication
    ): ResponseEntity<List<PlanejamentoDTO>> {
        val planejamentos = planejamentoService.listarMeusPlanejamentos(authentication.name)
        return ResponseEntity.ok(planejamentos)
    }

    /**
     * Busca planejamentos com filtros (palavra-chave, área, author, status)
     * GET /api/planejamentos/buscar?palavraChave=programacao&area=Computacao
     * Permissão: Autenticado
     */
    @GetMapping("/buscar")
    fun buscarComFiltros(
        @RequestParam(required = false) palavraChave: String?,
        @RequestParam(required = false) area: String?,
        @RequestParam(required = false) authorId: Long?,
        @RequestParam(required = false) status: StatusPlanejamento?,
        authentication: Authentication
    ): ResponseEntity<List<PlanejamentoDTO>> {
        val planejamentos = planejamentoService.buscarComFiltros(
            emailUsuario = authentication.name,
            palavraChave = palavraChave,
            area = area,
            authorId = authorId,
            status = status
        )
        return ResponseEntity.ok(planejamentos)
    }

    /**
     * Lista planejamentos pendentes (para aprovação)
     * GET /api/planejamentos/pendentes
     * Permissão: DIRETOR, ADMIN
     */
    @GetMapping("/pendentes")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun listarPlanejamentosPendentes(): ResponseEntity<List<PlanejamentoDTO>> {
        val planejamentos = planejamentoService.listarPlanejamentosPendentes()
        return ResponseEntity.ok(planejamentos)
    }

    /**
     * Busca um planejamento por ID
     * GET /api/planejamentos/{id}
     * Permissão: Autenticado
     */
    @GetMapping("/{id}")
    fun buscarPlanejamento(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<PlanejamentoDTO> {
        val planejamentoDTO = planejamentoService.buscarPorId(id, authentication.name)
        return ResponseEntity.ok(planejamentoDTO)
    }

    /**
     * Atualiza um planejamento existente
     * PUT /api/planejamentos/{id}
     * Permissão: PROFESSOR (apenas autor), DIRETOR, ADMIN
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'DIRETOR', 'ADMIN')")
    fun atualizarPlanejamento(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePlanejamentoRequest,
        authentication: Authentication
    ): ResponseEntity<PlanejamentoDTO> {
        val planejamentoDTO = planejamentoService.atualizarPlanejamento(id, request, authentication.name)
        return ResponseEntity.ok(planejamentoDTO)
    }

    /**
     * Altera o status de um planejamento (aprovar, reprovar, solicitar ajustes)
     * PATCH /api/planejamentos/{id}/status
     * Permissão: DIRETOR, ADMIN
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun alterarStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: AlterarStatusPlanejamentoRequest
    ): ResponseEntity<PlanejamentoDTO> {
        val planejamentoDTO = planejamentoService.alterarStatus(id, request)
        return ResponseEntity.ok(planejamentoDTO)
    }

    /**
     * Deleta um planejamento
     * DELETE /api/planejamentos/{id}
     * Permissão: PROFESSOR (apenas autor de planos não publicados), DIRETOR, ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'DIRETOR', 'ADMIN')")
    fun deletarPlanejamento(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<Void> {
        planejamentoService.deletarPlanejamento(id, authentication.name)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    /**
     * Aprova um planejamento (atalho para alterar status para PUBLICADO)
     * PATCH /api/planejamentos/{id}/aprovar
     * Permissão: DIRETOR, ADMIN
     */
    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun aprovarPlanejamento(@PathVariable id: Long): ResponseEntity<PlanejamentoDTO> {
        val request = AlterarStatusPlanejamentoRequest(
            status = StatusPlanejamento.PUBLICADO,
            motivo = null
        )
        val planejamentoDTO = planejamentoService.alterarStatus(id, request)
        return ResponseEntity.ok(planejamentoDTO)
    }

    /**
     * Reprova um planejamento com motivo opcional
     * PATCH /api/planejamentos/{id}/reprovar
     * Permissão: DIRETOR, ADMIN
     */
    @PatchMapping("/{id}/reprovar")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun reprovarPlanejamento(
        @PathVariable id: Long,
        @RequestBody(required = false) body: Map<String, String>?
    ): ResponseEntity<PlanejamentoDTO> {
        val motivo = body?.get("motivo") ?: "Planejamento não atende aos requisitos necessários."
        val request = AlterarStatusPlanejamentoRequest(
            status = StatusPlanejamento.REPROVADO,
            motivo = motivo
        )
        val planejamentoDTO = planejamentoService.alterarStatus(id, request)
        return ResponseEntity.ok(planejamentoDTO)
    }
}
