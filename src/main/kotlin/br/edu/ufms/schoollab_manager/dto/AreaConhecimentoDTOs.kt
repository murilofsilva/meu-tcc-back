package br.edu.ufms.schoollab_manager.dto

import br.edu.ufms.schoollab_manager.domain.entity.AreaConhecimento
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateAreaConhecimentoRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
    val nome: String,

    val ativo: Boolean = true
)

data class UpdateAreaConhecimentoRequest(
    @field:Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
    val nome: String? = null,

    val ativo: Boolean? = null
)

data class AreaConhecimentoDTO(
    val id: Long,
    val nome: String,
    val ativo: Boolean
) {
    companion object {
        fun fromEntity(area: AreaConhecimento): AreaConhecimentoDTO {
            return AreaConhecimentoDTO(
                id = area.id!!,
                nome = area.nome,
                ativo = area.ativo
            )
        }
    }
}
