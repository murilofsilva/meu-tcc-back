package br.edu.ufms.schoollab_manager.dto

import br.edu.ufms.schoollab_manager.domain.entity.Planejamento
import br.edu.ufms.schoollab_manager.domain.enums.StatusPlanejamento
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

// ===== REQUEST DTOs =====

/**
 * Request para criar um planejamento de aula
 * Padrão: CreateXXXRequest
 */
data class CreatePlanejamentoRequest(
    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 3, max = 200, message = "Título deve ter entre 3 e 200 caracteres")
    val titulo: String,

    @field:NotBlank(message = "Área é obrigatória")
    @field:Size(min = 3, max = 100, message = "Área deve ter entre 3 e 100 caracteres")
    val area: String,

    @field:NotBlank(message = "Descrição é obrigatória")
    @field:Size(min = 20, max = 2000, message = "Descrição deve ter entre 20 e 2000 caracteres")
    val descricao: String
)

/**
 * Request para atualizar um planejamento
 * Padrão: UpdateXXXRequest
 */
data class UpdatePlanejamentoRequest(
    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 3, max = 200, message = "Título deve ter entre 3 e 200 caracteres")
    val titulo: String,

    @field:NotBlank(message = "Área é obrigatória")
    @field:Size(min = 3, max = 100, message = "Área deve ter entre 3 e 100 caracteres")
    val area: String,

    @field:NotBlank(message = "Descrição é obrigatória")
    @field:Size(min = 20, max = 2000, message = "Descrição deve ter entre 20 e 2000 caracteres")
    val descricao: String
)

/**
 * Request para alterar status de um planejamento (usado pelo coordenador/diretor)
 */
data class AlterarStatusPlanejamentoRequest(
    @field:NotBlank(message = "Status é obrigatório")
    val status: StatusPlanejamento,

    @field:Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    val motivo: String? = null
)

// ===== RESPONSE DTOs =====

/**
 * Response completo para um planejamento
 * Padrão: XXXDTO
 */
data class PlanejamentoDTO(
    val id: Long,
    val author: UsuarioResumoDTO,
    val titulo: String,
    val area: String,
    val descricao: String,
    val status: StatusPlanejamento,
    val versao: Int,
    val criadoEm: Instant
) {
    companion object {
        fun fromEntity(planejamento: Planejamento): PlanejamentoDTO {
            return PlanejamentoDTO(
                id = planejamento.id!!,
                author = UsuarioResumoDTO(
                    id = planejamento.author.id!!,
                    nome = planejamento.author.nome,
                    email = planejamento.author.email
                ),
                titulo = planejamento.titulo,
                area = planejamento.area,
                descricao = planejamento.descricao,
                status = planejamento.status,
                versao = planejamento.versao,
                criadoEm = planejamento.criadoEm
            )
        }
    }
}
