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

@RestController
@RequestMapping("/api/planejamentos")
class PlanejamentoController(
    private val planejamentoService: PlanejamentoService
) {

    @PostMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'DIRETOR', 'ADMIN')")
    fun criarPlanejamento(
        @Valid @RequestBody request: CreatePlanejamentoRequest,
        authentication: Authentication
    ): ResponseEntity<PlanejamentoDTO> {
        val planejamentoDTO = planejamentoService.criarPlanejamento(request, authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(planejamentoDTO)
    }

    @GetMapping
    fun listarPlanejamentos(
        @RequestParam(required = false) status: StatusPlanejamento?,
        authentication: Authentication
    ): ResponseEntity<List<PlanejamentoDTO>> {
        val planejamentos = planejamentoService.listarPlanejamentos(authentication.name, status)
        return ResponseEntity.ok(planejamentos)
    }

    @GetMapping("/meus")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'DIRETOR', 'ADMIN')")
    fun listarMeusPlanejamentos(
        authentication: Authentication
    ): ResponseEntity<List<PlanejamentoDTO>> {
        val planejamentos = planejamentoService.listarMeusPlanejamentos(authentication.name)
        return ResponseEntity.ok(planejamentos)
    }

    @GetMapping("/buscar")
    fun buscarComFiltros(
        @RequestParam(required = false) palavraChave: String?,
        @RequestParam(required = false) area: String?,
        @RequestParam(required = false) authorId: Long?,
        @RequestParam(required = false) status: StatusPlanejamento?,
        @RequestParam(required = false, defaultValue = "false") somenteComCompetenciasComputacao: Boolean,
        @RequestParam(required = false, defaultValue = "false") somenteComRecursosAcessibilidade: Boolean,
        authentication: Authentication
    ): ResponseEntity<List<PlanejamentoDTO>> {
        val planejamentos = planejamentoService.buscarComFiltros(
            emailUsuario = authentication.name,
            palavraChave = palavraChave,
            area = area,
            authorId = authorId,
            status = status,
            somenteComCompetenciasComputacao = somenteComCompetenciasComputacao,
            somenteComRecursosAcessibilidade = somenteComRecursosAcessibilidade
        )
        return ResponseEntity.ok(planejamentos)
    }

    @GetMapping("/codigo/{codigo}")
    fun buscarPorCodigo(@PathVariable codigo: String): ResponseEntity<PlanejamentoDTO> {
        return ResponseEntity.ok(planejamentoService.buscarPorCodigo(codigo))
    }

    @GetMapping("/pendentes")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun listarPlanejamentosPendentes(): ResponseEntity<List<PlanejamentoDTO>> {
        return ResponseEntity.ok(planejamentoService.listarPlanejamentosPendentes())
    }

    @GetMapping("/{id}")
    fun buscarPlanejamento(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<PlanejamentoDTO> {
        return ResponseEntity.ok(planejamentoService.buscarPorId(id, authentication.name))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'DIRETOR', 'ADMIN')")
    fun atualizarPlanejamento(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePlanejamentoRequest,
        authentication: Authentication
    ): ResponseEntity<PlanejamentoDTO> {
        return ResponseEntity.ok(planejamentoService.atualizarPlanejamento(id, request, authentication.name))
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun alterarStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: AlterarStatusPlanejamentoRequest
    ): ResponseEntity<PlanejamentoDTO> {
        return ResponseEntity.ok(planejamentoService.alterarStatus(id, request))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'DIRETOR', 'ADMIN')")
    fun deletarPlanejamento(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<Void> {
        planejamentoService.deletarPlanejamento(id, authentication.name)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun aprovarPlanejamento(@PathVariable id: Long): ResponseEntity<PlanejamentoDTO> {
        val request = AlterarStatusPlanejamentoRequest(
            status = StatusPlanejamento.PUBLICADO,
            motivo = null
        )
        return ResponseEntity.ok(planejamentoService.alterarStatus(id, request))
    }

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
        return ResponseEntity.ok(planejamentoService.alterarStatus(id, request))
    }
}
