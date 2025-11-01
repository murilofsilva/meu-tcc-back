package br.edu.ufms.schoollab_manager.service

import br.edu.ufms.schoollab_manager.domain.entity.Usuario
import br.edu.ufms.schoollab_manager.domain.enums.PerfilUsuario
import br.edu.ufms.schoollab_manager.dto.RegisterProfessorRequest
import br.edu.ufms.schoollab_manager.dto.UsuarioDTO
import br.edu.ufms.schoollab_manager.exception.ConflictException
import br.edu.ufms.schoollab_manager.exception.ResourceNotFoundException
import br.edu.ufms.schoollab_manager.exception.ValidationException
import br.edu.ufms.schoollab_manager.repository.UsuarioRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

/**
 * Service responsável pela lógica de negócio relacionada a professores
 */
@Service
class ProfessorService(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * Cadastra um novo professor
     */
    fun cadastrarProfessor(request: RegisterProfessorRequest): UsuarioDTO {
        // Valida se o email já existe
        if (usuarioRepository.existsByEmail(request.email)) {
            throw ConflictException("Email já cadastrado")
        }

        // Cria o usuário com perfil de professor
        val usuario = Usuario(
            nome = request.nome,
            email = request.email,
            senha = passwordEncoder.encode(request.senha),
            perfil = PerfilUsuario.PROFESSOR
        )

        // Salva e retorna
        val usuarioSalvo = usuarioRepository.save(usuario)
        return toDTO(usuarioSalvo)
    }

    /**
     * Lista todos os professores
     */
    fun listarProfessores(): List<UsuarioDTO> {
        return usuarioRepository.findAll()
            .filter { it.perfil == PerfilUsuario.PROFESSOR }
            .map { toDTO(it) }
    }

    /**
     * Busca um professor por ID
     */
    fun buscarProfessor(id: Long): UsuarioDTO {
        val usuario = usuarioRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Professor não encontrado") }

        // Valida se é realmente um professor
        if (usuario.perfil != PerfilUsuario.PROFESSOR) {
            throw ValidationException("Usuário não é um professor")
        }

        return toDTO(usuario)
    }

    /**
     * Altera o status de um professor (ativo/inativo)
     */
    fun alterarStatus(id: Long, novoStatus: Boolean): UsuarioDTO {
        val usuario = usuarioRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Professor não encontrado") }

        // Valida se é realmente um professor
        if (usuario.perfil != PerfilUsuario.PROFESSOR) {
            throw ValidationException("Usuário não é um professor")
        }

        // Atualiza o status
        usuario.status = novoStatus
        val usuarioAtualizado = usuarioRepository.save(usuario)

        return toDTO(usuarioAtualizado)
    }

    /**
     * Converte uma entidade Usuario para DTO
     */
    private fun toDTO(usuario: Usuario): UsuarioDTO {
        return UsuarioDTO(
            id = usuario.id!!,
            nome = usuario.nome,
            email = usuario.email,
            perfil = usuario.perfil,
            status = usuario.status
        )
    }
}
