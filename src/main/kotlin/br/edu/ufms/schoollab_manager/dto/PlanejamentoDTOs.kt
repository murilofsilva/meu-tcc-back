package br.edu.ufms.schoollab_manager.dto

import br.edu.ufms.schoollab_manager.domain.entity.Planejamento
import br.edu.ufms.schoollab_manager.domain.enums.StatusPlanejamento
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreatePlanejamentoRequest(
    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 3, max = 200, message = "Título deve ter entre 3 e 200 caracteres")
    val titulo: String,

    @field:NotNull(message = "Disciplina/Área é obrigatória")
    val areaConhecimentoId: Long,

    @field:NotBlank(message = "Descrição é obrigatória")
    @field:Size(min = 20, max = 2000, message = "Descrição deve ter entre 20 e 2000 caracteres")
    val descricao: String,

    val publico: Boolean = false,

    val mobilizaCompetenciasComputacao: Boolean = false,

    val competenciasComputacao: List<String> = emptyList(),

    val utilizaRecursosAcessibilidade: Boolean = false,

    @field:Size(max = 2000, message = "Descrição de recursos de acessibilidade deve ter no máximo 2000 caracteres")
    val descricaoRecursosAcessibilidade: String? = null
)

data class UpdatePlanejamentoRequest(
    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 3, max = 200, message = "Título deve ter entre 3 e 200 caracteres")
    val titulo: String,

    @field:NotNull(message = "Disciplina/Área é obrigatória")
    val areaConhecimentoId: Long,

    @field:NotBlank(message = "Descrição é obrigatória")
    @field:Size(min = 20, max = 2000, message = "Descrição deve ter entre 20 e 2000 caracteres")
    val descricao: String,

    val publico: Boolean = false,

    val mobilizaCompetenciasComputacao: Boolean = false,

    val competenciasComputacao: List<String> = emptyList(),

    val utilizaRecursosAcessibilidade: Boolean = false,

    @field:Size(max = 2000, message = "Descrição de recursos de acessibilidade deve ter no máximo 2000 caracteres")
    val descricaoRecursosAcessibilidade: String? = null
)

data class AlterarStatusPlanejamentoRequest(
    @field:NotBlank(message = "Status é obrigatório")
    val status: StatusPlanejamento,

    @field:Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    val motivo: String? = null
)

data class AreaConhecimentoResumoDTO(
    val id: Long,
    val nome: String
)

data class PlanejamentoDTO(
    val id: Long,
    val author: UsuarioResumoDTO,
    val titulo: String,
    val codigo: String?,
    val areaConhecimento: AreaConhecimentoResumoDTO?,
    val area: String,
    val descricao: String,
    val status: StatusPlanejamento,
    val versao: Int,
    val publico: Boolean,
    val mobilizaCompetenciasComputacao: Boolean,
    val competenciasComputacao: List<String>,
    val utilizaRecursosAcessibilidade: Boolean,
    val descricaoRecursosAcessibilidade: String?,
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
                codigo = planejamento.codigo,
                areaConhecimento = planejamento.areaConhecimento?.let {
                    AreaConhecimentoResumoDTO(id = it.id!!, nome = it.nome)
                },
                area = planejamento.area,
                descricao = planejamento.descricao,
                status = planejamento.status,
                versao = planejamento.versao,
                publico = planejamento.publico,
                mobilizaCompetenciasComputacao = planejamento.mobilizaCompetenciasComputacao,
                competenciasComputacao = planejamento.competenciasComputacao.toList().sorted(),
                utilizaRecursosAcessibilidade = planejamento.utilizaRecursosAcessibilidade,
                descricaoRecursosAcessibilidade = planejamento.descricaoRecursosAcessibilidade,
                criadoEm = planejamento.criadoEm
            )
        }
    }
}
