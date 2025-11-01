package br.edu.ufms.schoollab_manager.controller

import br.edu.ufms.schoollab_manager.dto.RegisterProfessorRequest
import br.edu.ufms.schoollab_manager.dto.UsuarioDTO
import br.edu.ufms.schoollab_manager.exception.ValidationException
import br.edu.ufms.schoollab_manager.service.ProfessorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Controller responsável por receber e devolver requisições relacionadas a professores.
 * Toda a lógica de negócio é delegada ao ProfessorService.
 */
@RestController
@RequestMapping("/api/professores")
@PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
class ProfessorController(
    private val professorService: ProfessorService
) {

    /**
     * Cadastra um novo professor
     * Apenas DIRETOR e ADMIN podem cadastrar professores
     */
    @PostMapping
    fun cadastrarProfessor(@Valid @RequestBody request: RegisterProfessorRequest): ResponseEntity<UsuarioDTO> {
        val usuarioDTO = professorService.cadastrarProfessor(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioDTO)
    }

    /**
     * Lista todos os professores
     */
    @GetMapping
    fun listarProfessores(): ResponseEntity<List<UsuarioDTO>> {
        val professores = professorService.listarProfessores()
        return ResponseEntity.ok(professores)
    }

    /**
     * Busca um professor por ID
     */
    @GetMapping("/{id}")
    fun buscarProfessor(@PathVariable id: Long): ResponseEntity<UsuarioDTO> {
        val usuarioDTO = professorService.buscarProfessor(id)
        return ResponseEntity.ok(usuarioDTO)
    }

    /**
     * Ativa ou desativa um professor
     */
    @PatchMapping("/{id}/status")
    fun alterarStatus(
        @PathVariable id: Long,
        @RequestBody body: Map<String, Boolean>
    ): ResponseEntity<UsuarioDTO> {
        val novoStatus = body["status"]
            ?: throw ValidationException("Campo 'status' é obrigatório")

        val usuarioDTO = professorService.alterarStatus(id, novoStatus)
        return ResponseEntity.ok(usuarioDTO)
    }
}
