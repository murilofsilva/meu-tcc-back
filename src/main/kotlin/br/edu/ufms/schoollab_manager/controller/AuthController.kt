package br.edu.ufms.schoollab_manager.controller

import br.edu.ufms.schoollab_manager.dto.LoginRequest
import br.edu.ufms.schoollab_manager.dto.LoginResponse
import br.edu.ufms.schoollab_manager.dto.RegisterRequest
import br.edu.ufms.schoollab_manager.dto.UsuarioDTO
import br.edu.ufms.schoollab_manager.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller responsável por receber e devolver requisições de autenticação.
 * Toda a lógica de negócio é delegada ao AuthService.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<UsuarioDTO> {
        val usuarioDTO = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioDTO)
    }

    @GetMapping("/me")
    fun getCurrentUser(@RequestHeader("Authorization") authHeader: String): ResponseEntity<UsuarioDTO> {
        val usuarioDTO = authService.getCurrentUser(authHeader)
        return ResponseEntity.ok(usuarioDTO)
    }
}
