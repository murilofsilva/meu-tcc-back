package br.edu.ufms.schoollab_manager.dto

import br.edu.ufms.schoollab_manager.domain.enums.PerfilUsuario
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    val senha: String
)

data class LoginResponse(
    val token: String,
    val tipo: String = "Bearer",
    val usuario: UsuarioDTO
)

data class RegisterRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    val nome: String,

    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    val senha: String,

    @field:NotNull(message = "Perfil é obrigatório")
    val perfil: PerfilUsuario
)

data class UsuarioDTO(
    val id: Long,
    val nome: String,
    val email: String,
    val perfil: PerfilUsuario,
    val status: Boolean
)
