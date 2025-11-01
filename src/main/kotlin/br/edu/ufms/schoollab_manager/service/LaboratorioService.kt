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

/**
 * Service responsável pela lógica de negócio relacionada a laboratórios
 */
@Service
class LaboratorioService(
    private val laboratorioRepository: LaboratorioRepository
) {

    /**
     * Cadastra um novo laboratório
     */
    fun cadastrarLaboratorio(request: CreateLaboratorioRequest): LaboratorioDTO {
        // Valida se já existe um laboratório com o mesmo nome
        if (laboratorioRepository.existsByNome(request.nome)) {
            throw ConflictException("Já existe um laboratório com este nome")
        }

        // Cria o laboratório
        val laboratorio = Laboratorio(
            nome = request.nome,
            capacidade = request.capacidade,
            qtdEquipamentos = request.qtdEquipamentos
        )

        // Salva e retorna
        val laboratorioSalvo = laboratorioRepository.save(laboratorio)
        return LaboratorioDTO.fromEntity(laboratorioSalvo)
    }

    /**
     * Lista todos os laboratórios, opcionalmente filtrados por status
     */
    fun listarLaboratorios(status: Boolean?): List<LaboratorioDTO> {
        val laboratorios = if (status != null) {
            laboratorioRepository.findByStatusOrderByNomeAsc(status)
        } else {
            laboratorioRepository.findAllByOrderByNomeAsc()
        }

        return laboratorios.map { LaboratorioDTO.fromEntity(it) }
    }

    /**
     * Busca um laboratório por ID
     */
    fun buscarLaboratorio(id: Long): LaboratorioDTO {
        val laboratorio = laboratorioRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Laboratório não encontrado") }

        return LaboratorioDTO.fromEntity(laboratorio)
    }

    /**
     * Atualiza um laboratório
     */
    fun atualizarLaboratorio(id: Long, request: UpdateLaboratorioRequest): LaboratorioDTO {
        val laboratorio = laboratorioRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Laboratório não encontrado") }

        // Verifica se o novo nome já existe em outro laboratório
        if (request.nome != null && request.nome != laboratorio.nome) {
            if (laboratorioRepository.existsByNome(request.nome)) {
                throw ConflictException("Já existe um laboratório com este nome")
            }
            laboratorio.nome = request.nome
        }

        // Atualiza os campos opcionais
        request.capacidade?.let { laboratorio.capacidade = it }
        request.qtdEquipamentos?.let { laboratorio.qtdEquipamentos = it }

        // Salva e retorna
        val laboratorioAtualizado = laboratorioRepository.save(laboratorio)
        return LaboratorioDTO.fromEntity(laboratorioAtualizado)
    }

    /**
     * Altera o status de um laboratório (ativo/inativo)
     */
    fun alterarStatus(id: Long, novoStatus: Boolean): LaboratorioDTO {
        val laboratorio = laboratorioRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Laboratório não encontrado") }

        // Atualiza o status
        laboratorio.status = novoStatus
        val laboratorioAtualizado = laboratorioRepository.save(laboratorio)

        return LaboratorioDTO.fromEntity(laboratorioAtualizado)
    }

    /**
     * Deleta um laboratório
     */
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
