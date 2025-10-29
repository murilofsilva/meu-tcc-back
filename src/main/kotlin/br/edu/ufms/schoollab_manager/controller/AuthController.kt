package br.edu.ufms.schoollab_manager.controller

import br.edu.ufms.schoollab_manager.domain.entity.Usuario
import br.edu.ufms.schoollab_manager.dto.LoginRequest
import br.edu.ufms.schoollab_manager.dto.LoginResponse
import br.edu.ufms.schoollab_manager.dto.RegisterRequest
import br.edu.ufms.schoollab_manager.dto.UsuarioDTO
import br.edu.ufms.schoollab_manager.repository.UsuarioRepository
import br.edu.ufms.schoollab_manager.security.JwtService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.senha)
        )

        val usuario = usuarioRepository.findByEmail(request.email)
            .orElseThrow { RuntimeException("Usuário não encontrado") }

        val token = jwtService.generateToken(
            username = usuario.email,
            userId = usuario.id!!,
            perfil = usuario.perfil.name
        )

        val usuarioDTO = UsuarioDTO(
            id = usuario.id!!,
            nome = usuario.nome,
            email = usuario.email,
            perfil = usuario.perfil,
            status = usuario.status
        )

        return ResponseEntity.ok(LoginResponse(token = token, usuario = usuarioDTO))
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<Any> {
        if (usuarioRepository.existsByEmail(request.email)) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(mapOf("mensagem" to "Email já cadastrado"))
        }

        val usuario = Usuario(
            nome = request.nome,
            email = request.email,
            senha = passwordEncoder.encode(request.senha),
            perfil = request.perfil
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

    @GetMapping("/me")
    fun getCurrentUser(@RequestHeader("Authorization") authHeader: String): ResponseEntity<UsuarioDTO> {
        val token = authHeader.substring(7)
        val email = jwtService.extractUsername(token)

        val usuario = usuarioRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuário não encontrado") }

        val usuarioDTO = UsuarioDTO(
            id = usuario.id!!,
            nome = usuario.nome,
            email = usuario.email,
            perfil = usuario.perfil,
            status = usuario.status
        )

        return ResponseEntity.ok(usuarioDTO)
    }
}
