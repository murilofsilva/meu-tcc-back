package br.edu.ufms.schoollab_manager.controller

import br.edu.ufms.schoollab_manager.dto.CreateLaboratorioRequest
import br.edu.ufms.schoollab_manager.dto.LaboratorioDTO
import br.edu.ufms.schoollab_manager.dto.UpdateLaboratorioRequest
import br.edu.ufms.schoollab_manager.exception.ValidationException
import br.edu.ufms.schoollab_manager.service.LaboratorioService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Controller responsável por receber e devolver requisições relacionadas a laboratórios.
 * Toda a lógica de negócio é delegada ao LaboratorioService.
 */
@RestController
@RequestMapping("/api/laboratorios")
class LaboratorioController(
    private val laboratorioService: LaboratorioService
) {

    /**
     * Cadastra um novo laboratório
     * Apenas DIRETOR e ADMIN podem cadastrar
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun cadastrarLaboratorio(@Valid @RequestBody request: CreateLaboratorioRequest): ResponseEntity<LaboratorioDTO> {
        val laboratorioDTO = laboratorioService.cadastrarLaboratorio(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(laboratorioDTO)
    }

    /**
     * Lista todos os laboratórios
     * Todos os usuários autenticados podem listar
     */
    @GetMapping
    fun listarLaboratorios(@RequestParam(required = false) status: Boolean?): ResponseEntity<List<LaboratorioDTO>> {
        val laboratorios = laboratorioService.listarLaboratorios(status)
        return ResponseEntity.ok(laboratorios)
    }

    /**
     * Busca um laboratório por ID
     * Todos os usuários autenticados podem visualizar
     */
    @GetMapping("/{id}")
    fun buscarLaboratorio(@PathVariable id: Long): ResponseEntity<LaboratorioDTO> {
        val laboratorioDTO = laboratorioService.buscarLaboratorio(id)
        return ResponseEntity.ok(laboratorioDTO)
    }

    /**
     * Atualiza um laboratório
     * Apenas DIRETOR e ADMIN podem atualizar
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun atualizarLaboratorio(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateLaboratorioRequest
    ): ResponseEntity<LaboratorioDTO> {
        val laboratorioDTO = laboratorioService.atualizarLaboratorio(id, request)
        return ResponseEntity.ok(laboratorioDTO)
    }

    /**
     * Ativa ou desativa um laboratório
     * Apenas DIRETOR e ADMIN podem alterar status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun alterarStatus(
        @PathVariable id: Long,
        @RequestBody body: Map<String, Boolean>
    ): ResponseEntity<LaboratorioDTO> {
        val novoStatus = body["status"]
            ?: throw ValidationException("Campo 'status' é obrigatório")

        val laboratorioDTO = laboratorioService.alterarStatus(id, novoStatus)
        return ResponseEntity.ok(laboratorioDTO)
    }

    /**
     * Deleta um laboratório
     * Apenas DIRETOR e ADMIN podem deletar
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun deletarLaboratorio(@PathVariable id: Long): ResponseEntity<Void> {
        laboratorioService.deletarLaboratorio(id)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
