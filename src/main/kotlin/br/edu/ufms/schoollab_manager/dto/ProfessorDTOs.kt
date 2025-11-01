package br.edu.ufms.schoollab_manager.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request para cadastro de professor
 * Usado apenas por DIRETOR e ADMIN
 */
data class RegisterProfessorRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    val nome: String,

    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    val senha: String
)
