package br.edu.ufms.schoollab_manager.service

import br.edu.ufms.schoollab_manager.domain.entity.AreaConhecimento
import br.edu.ufms.schoollab_manager.dto.AreaConhecimentoDTO
import br.edu.ufms.schoollab_manager.dto.CreateAreaConhecimentoRequest
import br.edu.ufms.schoollab_manager.dto.UpdateAreaConhecimentoRequest
import br.edu.ufms.schoollab_manager.exception.ConflictException
import br.edu.ufms.schoollab_manager.exception.ResourceNotFoundException
import br.edu.ufms.schoollab_manager.repository.AreaConhecimentoRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class AreaConhecimentoService(
    private val areaRepository: AreaConhecimentoRepository
) {

    fun cadastrar(request: CreateAreaConhecimentoRequest): AreaConhecimentoDTO {
        val nomeNormalizado = request.nome.trim()
        if (areaRepository.existsByNome(nomeNormalizado)) {
            throw ConflictException("Já existe uma disciplina com este nome")
        }

        val area = AreaConhecimento(nome = nomeNormalizado, ativo = request.ativo)
        return AreaConhecimentoDTO.fromEntity(areaRepository.save(area))
    }

    fun listar(somenteAtivas: Boolean): List<AreaConhecimentoDTO> {
        val areas = if (somenteAtivas) {
            areaRepository.findByAtivoOrderByNomeAsc(true)
        } else {
            areaRepository.findAllByOrderByNomeAsc()
        }
        return areas.map { AreaConhecimentoDTO.fromEntity(it) }
    }

    fun buscarPorId(id: Long): AreaConhecimentoDTO {
        val area = areaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Disciplina não encontrada") }
        return AreaConhecimentoDTO.fromEntity(area)
    }

    fun atualizar(id: Long, request: UpdateAreaConhecimentoRequest): AreaConhecimentoDTO {
        val area = areaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Disciplina não encontrada") }

        request.nome?.let {
            val novoNome = it.trim()
            if (novoNome != area.nome && areaRepository.existsByNome(novoNome)) {
                throw ConflictException("Já existe uma disciplina com este nome")
            }
            area.nome = novoNome
        }

        request.ativo?.let { area.ativo = it }

        return AreaConhecimentoDTO.fromEntity(areaRepository.save(area))
    }

    fun deletar(id: Long) {
        val area = areaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Disciplina não encontrada") }

        try {
            areaRepository.delete(area)
        } catch (e: DataIntegrityViolationException) {
            throw ConflictException(
                "Não é possível excluir esta disciplina pois existem planos de aula vinculados. " +
                "Considere inativá-la."
            )
        }
    }
}
