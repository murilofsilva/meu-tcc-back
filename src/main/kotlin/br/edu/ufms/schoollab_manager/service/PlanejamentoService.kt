package br.edu.ufms.schoollab_manager.service

import br.edu.ufms.schoollab_manager.domain.entity.AreaConhecimento
import br.edu.ufms.schoollab_manager.domain.entity.Planejamento
import br.edu.ufms.schoollab_manager.domain.enums.PerfilUsuario
import br.edu.ufms.schoollab_manager.domain.enums.StatusPlanejamento
import br.edu.ufms.schoollab_manager.dto.AlterarStatusPlanejamentoRequest
import br.edu.ufms.schoollab_manager.dto.CreatePlanejamentoRequest
import br.edu.ufms.schoollab_manager.dto.PlanejamentoDTO
import br.edu.ufms.schoollab_manager.dto.UpdatePlanejamentoRequest
import br.edu.ufms.schoollab_manager.exception.ForbiddenException
import br.edu.ufms.schoollab_manager.exception.ResourceNotFoundException
import br.edu.ufms.schoollab_manager.exception.ValidationException
import br.edu.ufms.schoollab_manager.repository.AreaConhecimentoRepository
import br.edu.ufms.schoollab_manager.repository.PlanejamentoRepository
import br.edu.ufms.schoollab_manager.repository.UsuarioRepository
import org.springframework.stereotype.Service

object CompetenciasComputacao {
    const val PENSAMENTO_COMPUTACIONAL = "PENSAMENTO_COMPUTACIONAL"
    const val CULTURA_DIGITAL = "CULTURA_DIGITAL"
    const val MUNDO_DIGITAL = "MUNDO_DIGITAL"

    val TODAS = setOf(PENSAMENTO_COMPUTACIONAL, CULTURA_DIGITAL, MUNDO_DIGITAL)
}

@Service
class PlanejamentoService(
    private val planejamentoRepository: PlanejamentoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val areaConhecimentoRepository: AreaConhecimentoRepository,
    private val codeGenerator: PlanejamentoCodeGenerator
) {

    fun criarPlanejamento(request: CreatePlanejamentoRequest, emailProfessor: String): PlanejamentoDTO {
        val professor = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        val area = resolveArea(request.areaConhecimentoId)
        validarCompetenciasComputacao(request.mobilizaCompetenciasComputacao, request.competenciasComputacao)
        val descricaoAcessibilidade = validarAcessibilidade(
            request.utilizaRecursosAcessibilidade,
            request.descricaoRecursosAcessibilidade
        )

        val planejamento = Planejamento(
            author = professor,
            titulo = request.titulo,
            codigo = codeGenerator.generateUnique(),
            areaConhecimento = area,
            area = area.nome,
            descricao = request.descricao,
            status = StatusPlanejamento.PENDENTE,
            versao = 1,
            publico = request.publico,
            mobilizaCompetenciasComputacao = request.mobilizaCompetenciasComputacao,
            competenciasComputacao = if (request.mobilizaCompetenciasComputacao)
                request.competenciasComputacao.toMutableSet() else mutableSetOf(),
            utilizaRecursosAcessibilidade = request.utilizaRecursosAcessibilidade,
            descricaoRecursosAcessibilidade = descricaoAcessibilidade
        )

        return PlanejamentoDTO.fromEntity(planejamentoRepository.save(planejamento))
    }

    fun listarPlanejamentos(emailUsuario: String, status: StatusPlanejamento?): List<PlanejamentoDTO> {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        val planejamentos = when {
            usuario.perfil == PerfilUsuario.PROFESSOR -> {
                if (status != null) {
                    val planosComStatus = planejamentoRepository.findByStatus(status)
                    val meusPlanos = planejamentoRepository.findByAuthorId(usuario.id!!)
                    (planosComStatus.filter { it.status == StatusPlanejamento.PUBLICADO && it.publico } +
                            meusPlanos.filter { it.status == status }).distinctBy { it.id }
                } else {
                    val planosPublicos = planejamentoRepository.findByStatus(StatusPlanejamento.PUBLICADO)
                        .filter { it.publico }
                    val meusPlanos = planejamentoRepository.findByAuthorId(usuario.id!!)
                    (planosPublicos + meusPlanos).distinctBy { it.id }
                }
            }
            else -> {
                if (status != null) planejamentoRepository.findByStatus(status)
                else planejamentoRepository.findAll().toList()
            }
        }

        return planejamentos
            .sortedByDescending { it.criadoEm }
            .map { PlanejamentoDTO.fromEntity(it) }
    }

    fun listarMeusPlanejamentos(emailProfessor: String): List<PlanejamentoDTO> {
        val professor = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        return planejamentoRepository.findByAuthorId(professor.id!!)
            .sortedByDescending { it.criadoEm }
            .map { PlanejamentoDTO.fromEntity(it) }
    }

    fun buscarComFiltros(
        emailUsuario: String,
        palavraChave: String?,
        area: String?,
        authorId: Long?,
        status: StatusPlanejamento?,
        somenteComCompetenciasComputacao: Boolean = false,
        somenteComRecursosAcessibilidade: Boolean = false
    ): List<PlanejamentoDTO> {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        var planejamentos = when {
            usuario.perfil == PerfilUsuario.PROFESSOR -> {
                val planosPublicos = planejamentoRepository.findByStatus(StatusPlanejamento.PUBLICADO)
                    .filter { it.publico }
                val meusPlanos = planejamentoRepository.findByAuthorId(usuario.id!!)
                (planosPublicos + meusPlanos).distinctBy { it.id }
            }
            else -> planejamentoRepository.findAll().toList()
        }

        if (!palavraChave.isNullOrBlank()) {
            val keyword = palavraChave.lowercase()
            planejamentos = planejamentos.filter {
                it.titulo.lowercase().contains(keyword) ||
                        it.descricao.lowercase().contains(keyword) ||
                        it.area.lowercase().contains(keyword)
            }
        }

        if (!area.isNullOrBlank()) {
            planejamentos = planejamentos.filter { it.area == area }
        }

        if (authorId != null) {
            planejamentos = planejamentos.filter { it.author.id == authorId }
        }

        if (status != null) {
            planejamentos = planejamentos.filter { it.status == status }
        }

        if (somenteComCompetenciasComputacao) {
            planejamentos = planejamentos.filter { it.mobilizaCompetenciasComputacao }
        }

        if (somenteComRecursosAcessibilidade) {
            planejamentos = planejamentos.filter { it.utilizaRecursosAcessibilidade }
        }

        return planejamentos
            .sortedByDescending { it.criadoEm }
            .map { PlanejamentoDTO.fromEntity(it) }
    }

    fun buscarPorId(id: Long, emailUsuario: String): PlanejamentoDTO {
        val planejamento = planejamentoRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Planejamento não encontrado") }

        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        if (usuario.perfil == PerfilUsuario.PROFESSOR) {
            if (planejamento.status != StatusPlanejamento.PUBLICADO &&
                planejamento.author.id != usuario.id
            ) {
                throw ForbiddenException("Você não tem permissão para visualizar este planejamento")
            }
        }

        return PlanejamentoDTO.fromEntity(planejamento)
    }

    fun buscarPorCodigo(codigo: String): PlanejamentoDTO {
        val planejamento = planejamentoRepository.findByCodigo(codigo.trim().uppercase())
            .orElseThrow { ResourceNotFoundException("Nenhum plano encontrado para o código informado") }
        return PlanejamentoDTO.fromEntity(planejamento)
    }

    fun atualizarPlanejamento(
        id: Long,
        request: UpdatePlanejamentoRequest,
        emailProfessor: String
    ): PlanejamentoDTO {
        val planejamento = planejamentoRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Planejamento não encontrado") }

        val usuario = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        if (planejamento.author.id != usuario.id) {
            throw ForbiddenException("Você não tem permissão para atualizar este planejamento")
        }

        if (planejamento.status == StatusPlanejamento.REPROVADO) {
            throw ValidationException("Planejamentos reprovados não podem ser atualizados. Crie um novo planejamento.")
        }

        val area = resolveArea(request.areaConhecimentoId)
        validarCompetenciasComputacao(request.mobilizaCompetenciasComputacao, request.competenciasComputacao)
        val descricaoAcessibilidade = validarAcessibilidade(
            request.utilizaRecursosAcessibilidade,
            request.descricaoRecursosAcessibilidade
        )

        val mudouParaPublico = !planejamento.publico && request.publico

        planejamento.titulo = request.titulo
        planejamento.areaConhecimento = area
        planejamento.area = area.nome
        planejamento.descricao = request.descricao
        planejamento.publico = request.publico
        planejamento.mobilizaCompetenciasComputacao = request.mobilizaCompetenciasComputacao
        planejamento.competenciasComputacao = if (request.mobilizaCompetenciasComputacao)
            request.competenciasComputacao.toMutableSet() else mutableSetOf()
        planejamento.utilizaRecursosAcessibilidade = request.utilizaRecursosAcessibilidade
        planejamento.descricaoRecursosAcessibilidade = descricaoAcessibilidade

        if (planejamento.codigo.isNullOrBlank()) {
            planejamento.codigo = codeGenerator.generateUnique()
        }

        if (planejamento.status == StatusPlanejamento.PUBLICADO ||
            planejamento.status == StatusPlanejamento.AGUARDANDO_AJUSTES ||
            mudouParaPublico
        ) {
            planejamento.status = StatusPlanejamento.PENDENTE
            planejamento.versao += 1
        }

        return PlanejamentoDTO.fromEntity(planejamentoRepository.save(planejamento))
    }

    fun deletarPlanejamento(id: Long, emailUsuario: String) {
        val planejamento = planejamentoRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Planejamento não encontrado") }

        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        if (usuario.perfil == PerfilUsuario.PROFESSOR) {
            if (planejamento.author.id != usuario.id) {
                throw ForbiddenException("Você não tem permissão para deletar este planejamento")
            }
            if (planejamento.status == StatusPlanejamento.PUBLICADO) {
                throw ValidationException("Planejamentos publicados não podem ser deletados")
            }
        }

        planejamentoRepository.delete(planejamento)
    }

    fun alterarStatus(id: Long, request: AlterarStatusPlanejamentoRequest): PlanejamentoDTO {
        val planejamento = planejamentoRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Planejamento não encontrado") }

        if (planejamento.status == StatusPlanejamento.REPROVADO) {
            throw ValidationException("Planejamentos reprovados não podem ter o status alterado")
        }

        if (request.status == StatusPlanejamento.AGUARDANDO_AJUSTES && request.motivo.isNullOrBlank()) {
            throw ValidationException("Motivo é obrigatório ao solicitar ajustes")
        }

        if (planejamento.status != StatusPlanejamento.PENDENTE) {
            throw ValidationException("Apenas planejamentos pendentes podem ter o status alterado")
        }

        planejamento.status = request.status
        return PlanejamentoDTO.fromEntity(planejamentoRepository.save(planejamento))
    }

    fun listarPlanejamentosPendentes(): List<PlanejamentoDTO> {
        return planejamentoRepository.findByStatus(StatusPlanejamento.PENDENTE)
            .sortedByDescending { it.criadoEm }
            .map { PlanejamentoDTO.fromEntity(it) }
    }

    private fun resolveArea(id: Long): AreaConhecimento {
        val area = areaConhecimentoRepository.findById(id)
            .orElseThrow { ValidationException("Disciplina/Área informada não existe") }
        if (!area.ativo) {
            throw ValidationException("Disciplina '${area.nome}' está inativa e não pode ser usada")
        }
        return area
    }

    private fun validarCompetenciasComputacao(mobiliza: Boolean, competencias: List<String>) {
        if (!mobiliza) return

        if (competencias.isEmpty()) {
            throw ValidationException(
                "Selecione ao menos uma competência de computação quando a opção está marcada"
            )
        }
        val invalidas = competencias.toSet() - CompetenciasComputacao.TODAS
        if (invalidas.isNotEmpty()) {
            throw ValidationException("Competência(s) inválida(s): ${invalidas.joinToString()}")
        }
    }

    private fun validarAcessibilidade(utiliza: Boolean, descricao: String?): String? {
        if (!utiliza) return null
        val texto = descricao?.trim().orEmpty()
        if (texto.length < 10) {
            throw ValidationException(
                "Descreva os recursos de acessibilidade utilizados (mínimo 10 caracteres)"
            )
        }
        return texto
    }
}
