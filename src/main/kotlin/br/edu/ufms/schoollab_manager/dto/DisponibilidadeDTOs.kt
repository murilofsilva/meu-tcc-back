package br.edu.ufms.schoollab_manager.dto

import br.edu.ufms.schoollab_manager.domain.entity.DisponibilidadeLaboratorio
import br.edu.ufms.schoollab_manager.domain.entity.Indisponibilidade
import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

data class CreateDisponibilidadeRequest(
    @field:NotNull(message = "Dia da semana é obrigatório")
    val diaSemana: DayOfWeek,

    @field:NotNull(message = "Hora de início é obrigatória")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val horaInicio: LocalTime,

    @field:NotNull(message = "Hora de fim é obrigatória")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val horaFim: LocalTime
)

data class DisponibilidadeDTO(
    val id: Long,
    val laboratorioId: Long,
    val diaSemana: DayOfWeek,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val horaInicio: LocalTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val horaFim: LocalTime
) {
    companion object {
        fun fromEntity(d: DisponibilidadeLaboratorio): DisponibilidadeDTO = DisponibilidadeDTO(
            id = d.id!!,
            laboratorioId = d.laboratorio.id!!,
            diaSemana = d.diaSemana,
            horaInicio = d.horaInicio,
            horaFim = d.horaFim
        )
    }
}

data class CreateIndisponibilidadeRequest(
    @field:NotNull(message = "Data de início é obrigatória")
    val inicio: Instant,

    @field:NotNull(message = "Data de fim é obrigatória")
    val fim: Instant,

    @field:Size(min = 3, max = 500, message = "Motivo deve ter entre 3 e 500 caracteres")
    val motivo: String
)

data class IndisponibilidadeDTO(
    val id: Long,
    val laboratorioId: Long,
    val inicio: Instant,
    val fim: Instant,
    val motivo: String
) {
    companion object {
        fun fromEntity(i: Indisponibilidade): IndisponibilidadeDTO = IndisponibilidadeDTO(
            id = i.id!!,
            laboratorioId = i.laboratorio.id!!,
            inicio = i.inicio,
            fim = i.fim,
            motivo = i.motivo
        )
    }
}
