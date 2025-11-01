package br.edu.ufms.schoollab_manager.dto

import br.edu.ufms.schoollab_manager.domain.entity.Laboratorio
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateLaboratorioRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 3, max = 120, message = "Nome deve ter entre 3 e 120 caracteres")
    val nome: String,

    @field:Min(value = 0, message = "Capacidade não pode ser negativa")
    val capacidade: Int,

    @field:Min(value = 0, message = "Quantidade de equipamentos não pode ser negativa")
    val qtdEquipamentos: Int = 0
)

data class UpdateLaboratorioRequest(
    @field:Size(min = 3, max = 120, message = "Nome deve ter entre 3 e 120 caracteres")
    val nome: String? = null,

    @field:Min(value = 0, message = "Capacidade não pode ser negativa")
    val capacidade: Int? = null,

    @field:Min(value = 0, message = "Quantidade de equipamentos não pode ser negativa")
    val qtdEquipamentos: Int? = null
)

data class LaboratorioDTO(
    val id: Long,
    val nome: String,
    val capacidade: Int,
    val qtdEquipamentos: Int,
    val status: Boolean
) {
    companion object {
        fun fromEntity(laboratorio: Laboratorio): LaboratorioDTO {
            return LaboratorioDTO(
                id = laboratorio.id!!,
                nome = laboratorio.nome,
                capacidade = laboratorio.capacidade,
                qtdEquipamentos = laboratorio.qtdEquipamentos,
                status = laboratorio.status
            )
        }
    }
}
