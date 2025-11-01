package br.edu.ufms.schoollab_manager.controller

import br.edu.ufms.schoollab_manager.domain.entity.Usuario
import br.edu.ufms.schoollab_manager.domain.enums.PerfilUsuario
import br.edu.ufms.schoollab_manager.dto.RegisterProfessorRequest
import br.edu.ufms.schoollab_manager.dto.UsuarioDTO
import br.edu.ufms.schoollab_manager.repository.UsuarioRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/professores")
@PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
class ProfessorController(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * Cadastra um novo professor
     * Apenas DIRETOR e ADMIN podem cadastrar professores
     */
    @PostMapping
    fun cadastrarProfessor(@Valid @RequestBody request: RegisterProfessorRequest): ResponseEntity<Any> {
        if (usuarioRepository.existsByEmail(request.email)) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(mapOf("mensagem" to "Email já cadastrado"))
        }

        val usuario = Usuario(
            nome = request.nome,
            email = request.email,
            senha = passwordEncoder.encode(request.senha),
            perfil = PerfilUsuario.PROFESSOR
        )

        val usuarioSalvo = usuarioRepository.save(usuario)

        val usuarioDTO = UsuarioDTO(
            id = usuarioSalvo.id!!,
            nome = usuarioSalvo.nome,
            email = usuarioSalvo.email,
            perfil = usuarioSalvo.perfil,
            status = usuarioSalvo.status
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(usuarioDTO)
    }

    /**
     * Lista todos os professores
     */
    @GetMapping
    fun listarProfessores(): ResponseEntity<List<UsuarioDTO>> {
        val professores = usuarioRepository.findAll()
            .filter { it.perfil == PerfilUsuario.PROFESSOR }
            .map { usuario ->
                UsuarioDTO(
                    id = usuario.id!!,
                    nome = usuario.nome,
                    email = usuario.email,
                    perfil = usuario.perfil,
                    status = usuario.status
                )
            }

        return ResponseEntity.ok(professores)
    }

    /**
     * Busca um professor por ID
     */
    @GetMapping("/{id}")
    fun buscarProfessor(@PathVariable id: Long): ResponseEntity<Any> {
        val usuario = usuarioRepository.findById(id)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Professor não encontrado"))

        if (usuario.perfil != PerfilUsuario.PROFESSOR) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Usuário não é um professor"))
        }

        val usuarioDTO = UsuarioDTO(
            id = usuario.id!!,
            nome = usuario.nome,
            email = usuario.email,
            perfil = usuario.perfil,
            status = usuario.status
        )

        return ResponseEntity.ok(usuarioDTO)
    }

    /**
     * Ativa ou desativa um professor
     */
    @PatchMapping("/{id}/status")
    fun alterarStatus(
        @PathVariable id: Long,
        @RequestBody body: Map<String, Boolean>
    ): ResponseEntity<Any> {
        val usuario = usuarioRepository.findById(id)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Professor não encontrado"))

        if (usuario.perfil != PerfilUsuario.PROFESSOR) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Usuário não é um professor"))
        }

        val novoStatus = body["status"]
            ?: return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Campo 'status' é obrigatório"))

        usuario.status = novoStatus
        val usuarioAtualizado = usuarioRepository.save(usuario)

        val usuarioDTO = UsuarioDTO(
            id = usuarioAtualizado.id!!,
            nome = usuarioAtualizado.nome,
            email = usuarioAtualizado.email,
            perfil = usuarioAtualizado.perfil,
            status = usuarioAtualizado.status
        )

        return ResponseEntity.ok(usuarioDTO)
    }
}
