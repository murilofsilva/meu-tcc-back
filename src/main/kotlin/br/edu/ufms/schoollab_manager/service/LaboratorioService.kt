package br.edu.ufms.schoollab_manager.service

import br.edu.ufms.schoollab_manager.domain.entity.Laboratorio
import br.edu.ufms.schoollab_manager.dto.CreateLaboratorioRequest
import br.edu.ufms.schoollab_manager.dto.LaboratorioDTO
import br.edu.ufms.schoollab_manager.dto.UpdateLaboratorioRequest
import br.edu.ufms.schoollab_manager.exception.ConflictException
import br.edu.ufms.schoollab_manager.exception.ResourceNotFoundException
import br.edu.ufms.schoollab_manager.repository.LaboratorioRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class LaboratorioService(
    private val laboratorioRepository: LaboratorioRepository
) {

    fun cadastrarLaboratorio(request: CreateLaboratorioRequest): LaboratorioDTO {
        if (laboratorioRepository.existsByNome(request.nome)) {
            throw ConflictException("Já existe um laboratório com este nome")
        }

        val laboratorio = Laboratorio(
            nome = request.nome,
            capacidade = request.capacidade,
            quantidadeComputadores = request.quantidadeComputadores,
            descricao = request.descricao?.trim()?.ifBlank { null }
        )

        val laboratorioSalvo = laboratorioRepository.save(laboratorio)
        return LaboratorioDTO.fromEntity(laboratorioSalvo)
    }

    fun listarLaboratorios(status: Boolean?): List<LaboratorioDTO> {
        val laboratorios = if (status != null) {
            laboratorioRepository.findByStatusOrderByNomeAsc(status)
        } else {
            laboratorioRepository.findAllByOrderByNomeAsc()
        }

        return laboratorios.map { LaboratorioDTO.fromEntity(it) }
    }

    fun buscarLaboratorio(id: Long): LaboratorioDTO {
        val laboratorio = laboratorioRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Laboratório não encontrado") }

        return LaboratorioDTO.fromEntity(laboratorio)
    }

    fun atualizarLaboratorio(id: Long, request: UpdateLaboratorioRequest): LaboratorioDTO {
        val laboratorio = laboratorioRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Laboratório não encontrado") }

        if (request.nome != null && request.nome != laboratorio.nome) {
            if (laboratorioRepository.existsByNome(request.nome)) {
                throw ConflictException("Já existe um laboratório com este nome")
            }
            laboratorio.nome = request.nome
        }

        request.capacidade?.let { laboratorio.capacidade = it }
        request.quantidadeComputadores?.let { laboratorio.quantidadeComputadores = it }
        request.descricao?.let { laboratorio.descricao = it.trim().ifBlank { null } }

        val laboratorioAtualizado = laboratorioRepository.save(laboratorio)
        return LaboratorioDTO.fromEntity(laboratorioAtualizado)
    }

    fun alterarStatus(id: Long, novoStatus: Boolean): LaboratorioDTO {
        val laboratorio = laboratorioRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Laboratório não encontrado") }

        laboratorio.status = novoStatus
        val laboratorioAtualizado = laboratorioRepository.save(laboratorio)

        return LaboratorioDTO.fromEntity(laboratorioAtualizado)
    }

    fun deletarLaboratorio(id: Long) {
        val laboratorio = laboratorioRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Laboratório não encontrado") }

        try {
            laboratorioRepository.delete(laboratorio)
        } catch (e: DataIntegrityViolationException) {
            throw ConflictException("Não é possível deletar este laboratório pois existem reservas vinculadas a ele")
        }
    }
}
