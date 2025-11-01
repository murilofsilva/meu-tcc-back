package br.edu.ufms.schoollab_manager.service

import br.edu.ufms.schoollab_manager.domain.entity.Usuario
import br.edu.ufms.schoollab_manager.dto.LoginRequest
import br.edu.ufms.schoollab_manager.dto.LoginResponse
import br.edu.ufms.schoollab_manager.dto.RegisterRequest
import br.edu.ufms.schoollab_manager.dto.UsuarioDTO
import br.edu.ufms.schoollab_manager.exception.ConflictException
import br.edu.ufms.schoollab_manager.exception.ResourceNotFoundException
import br.edu.ufms.schoollab_manager.repository.UsuarioRepository
import br.edu.ufms.schoollab_manager.security.JwtService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

/**
 * Service responsável pela lógica de autenticação e autorização
 */
@Service
class AuthService(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager
) {

    /**
     * Realiza o login de um usuário
     */
    fun login(request: LoginRequest): LoginResponse {
        // Autentica o usuário
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.senha)
        )

        // Busca o usuário
        val usuario = usuarioRepository.findByEmail(request.email)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        // Gera o token JWT
        val token = jwtService.generateToken(
            username = usuario.email,
            userId = usuario.id!!,
            perfil = usuario.perfil.name
        )

        // Retorna a resposta
        return LoginResponse(
            token = token,
            usuario = toDTO(usuario)
        )
    }

    /**
     * Registra um novo usuário
     */
    fun register(request: RegisterRequest): UsuarioDTO {
        // Valida se o email já existe
        if (usuarioRepository.existsByEmail(request.email)) {
            throw ConflictException("Email já cadastrado")
        }

        // Cria o usuário
        val usuario = Usuario(
            nome = request.nome,
            email = request.email,
            senha = passwordEncoder.encode(request.senha),
            perfil = request.perfil
        )

        // Salva e retorna
        val usuarioSalvo = usuarioRepository.save(usuario)
        return toDTO(usuarioSalvo)
    }

    /**
     * Busca o usuário atual a partir do token JWT
     */
    fun getCurrentUser(token: String): UsuarioDTO {
        // Remove o prefixo "Bearer " se presente
        val jwtToken = if (token.startsWith("Bearer ")) token.substring(7) else token

        // Extrai o email do token
        val email = jwtService.extractUsername(jwtToken)

        // Busca o usuário
        val usuario = usuarioRepository.findByEmail(email)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        return toDTO(usuario)
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
