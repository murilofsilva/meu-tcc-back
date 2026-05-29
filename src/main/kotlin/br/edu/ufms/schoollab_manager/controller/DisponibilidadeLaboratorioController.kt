package br.edu.ufms.schoollab_manager.controller

import br.edu.ufms.schoollab_manager.dto.CreateDisponibilidadeRequest
import br.edu.ufms.schoollab_manager.dto.CreateIndisponibilidadeRequest
import br.edu.ufms.schoollab_manager.dto.DisponibilidadeDTO
import br.edu.ufms.schoollab_manager.dto.IndisponibilidadeDTO
import br.edu.ufms.schoollab_manager.service.DisponibilidadeLaboratorioService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/laboratorios/{laboratorioId}")
class DisponibilidadeLaboratorioController(
    private val service: DisponibilidadeLaboratorioService
) {

    @GetMapping("/disponibilidades")
    fun listarDisponibilidades(@PathVariable laboratorioId: Long): ResponseEntity<List<DisponibilidadeDTO>> {
        return ResponseEntity.ok(service.listarDisponibilidades(laboratorioId))
    }

    @PutMapping("/disponibilidades")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun substituirDisponibilidades(
        @PathVariable laboratorioId: Long,
        @Valid @RequestBody novas: List<CreateDisponibilidadeRequest>
    ): ResponseEntity<List<DisponibilidadeDTO>> {
        return ResponseEntity.ok(service.substituirDisponibilidades(laboratorioId, novas))
    }

    @GetMapping("/indisponibilidades")
    fun listarIndisponibilidades(@PathVariable laboratorioId: Long): ResponseEntity<List<IndisponibilidadeDTO>> {
        return ResponseEntity.ok(service.listarIndisponibilidades(laboratorioId))
    }

    @PostMapping("/indisponibilidades")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun criarIndisponibilidade(
        @PathVariable laboratorioId: Long,
        @Valid @RequestBody request: CreateIndisponibilidadeRequest
    ): ResponseEntity<IndisponibilidadeDTO> {
        val dto = service.criarIndisponibilidade(laboratorioId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(dto)
    }

    @DeleteMapping("/indisponibilidades/{id}")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun removerIndisponibilidade(
        @PathVariable laboratorioId: Long,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        service.removerIndisponibilidade(id)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
