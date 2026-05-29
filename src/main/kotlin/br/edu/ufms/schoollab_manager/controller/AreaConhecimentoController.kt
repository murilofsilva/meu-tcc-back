package br.edu.ufms.schoollab_manager.controller

import br.edu.ufms.schoollab_manager.dto.AreaConhecimentoDTO
import br.edu.ufms.schoollab_manager.dto.CreateAreaConhecimentoRequest
import br.edu.ufms.schoollab_manager.dto.UpdateAreaConhecimentoRequest
import br.edu.ufms.schoollab_manager.service.AreaConhecimentoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/areas-conhecimento")
class AreaConhecimentoController(
    private val areaService: AreaConhecimentoService
) {

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun cadastrar(@Valid @RequestBody request: CreateAreaConhecimentoRequest): ResponseEntity<AreaConhecimentoDTO> {
        return ResponseEntity.status(HttpStatus.CREATED).body(areaService.cadastrar(request))
    }

    @GetMapping
    fun listar(
        @RequestParam(required = false, defaultValue = "true") somenteAtivas: Boolean
    ): ResponseEntity<List<AreaConhecimentoDTO>> {
        return ResponseEntity.ok(areaService.listar(somenteAtivas))
    }

    @GetMapping("/{id}")
    fun buscar(@PathVariable id: Long): ResponseEntity<AreaConhecimentoDTO> {
        return ResponseEntity.ok(areaService.buscarPorId(id))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun atualizar(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateAreaConhecimentoRequest
    ): ResponseEntity<AreaConhecimentoDTO> {
        return ResponseEntity.ok(areaService.atualizar(id, request))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun deletar(@PathVariable id: Long): ResponseEntity<Void> {
        areaService.deletar(id)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
