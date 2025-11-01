package br.edu.ufms.schoollab_manager.dto

import br.edu.ufms.schoollab_manager.domain.entity.Reserva
import br.edu.ufms.schoollab_manager.domain.enums.StatusReserva
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateReservaRequest(
    @field:NotNull(message = "ID do laboratório é obrigatório")
    val laboratorioId: Long,

    @field:NotNull(message = "Data de início é obrigatória")
    val inicio: Instant,

    @field:NotNull(message = "Data de fim é obrigatória")
    val fim: Instant,

    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 3, max = 160, message = "Título deve ter entre 3 e 160 caracteres")
    val titulo: String,

    @field:Size(max = 80, message = "Turma deve ter no máximo 80 caracteres")
    val turma: String? = null,

    @field:Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    val descricao: String? = null,

    val planejamentoId: Long? = null
)

data class UpdateReservaRequest(
    @field:NotNull(message = "Data de início é obrigatória")
    val inicio: Instant,

    @field:NotNull(message = "Data de fim é obrigatória")
    val fim: Instant,

    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 3, max = 160, message = "Título deve ter entre 3 e 160 caracteres")
    val titulo: String,

    @field:Size(max = 80, message = "Turma deve ter no máximo 80 caracteres")
    val turma: String? = null,

    @field:Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    val descricao: String? = null
)

data class AlterarStatusReservaRequest(
    @field:NotNull(message = "Status é obrigatório")
    val status: StatusReserva,

    @field:Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    val motivo: String? = null
)

data class ReservaDTO(
    val id: Long,
    val laboratorio: LaboratorioResumoDTO,
    val professor: UsuarioResumoDTO,
    val inicio: Instant,
    val fim: Instant,
    val titulo: String,
    val turma: String?,
    val descricao: String?,
    val planejamento: PlanejamentoResumoDTO?,
    val status: StatusReserva,
    val motivoStatus: String?,
    val criadoEm: Instant
) {
    companion object {
        fun fromEntity(reserva: Reserva): ReservaDTO {
            return ReservaDTO(
                id = reserva.id!!,
                laboratorio = LaboratorioResumoDTO(
                    id = reserva.laboratorio.id!!,
                    nome = reserva.laboratorio.nome
                ),
                professor = UsuarioResumoDTO(
                    id = reserva.professor.id!!,
                    nome = reserva.professor.nome,
                    email = reserva.professor.email
                ),
                inicio = reserva.inicio,
                fim = reserva.fim,
                titulo = reserva.titulo,
                turma = reserva.turma,
                descricao = reserva.descricao,
                planejamento = reserva.planejamento?.let {
                    PlanejamentoResumoDTO(
                        id = it.id!!,
                        titulo = it.titulo
                    )
                },
                status = reserva.status,
                motivoStatus = reserva.motivoStatus,
                criadoEm = reserva.criadoEm
            )
        }
    }
}

data class LaboratorioResumoDTO(
    val id: Long,
    val nome: String
)

data class UsuarioResumoDTO(
    val id: Long,
    val nome: String,
    val email: String
)

data class PlanejamentoResumoDTO(
    val id: Long,
    val titulo: String
)
