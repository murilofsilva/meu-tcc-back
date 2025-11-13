package br.edu.ufms.schoollab_manager.service

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
import br.edu.ufms.schoollab_manager.repository.PlanejamentoRepository
import br.edu.ufms.schoollab_manager.repository.UsuarioRepository
import org.springframework.stereotype.Service

/**
 * Service responsável pela lógica de negócio relacionada a planejamentos de aula
 */
@Service
class PlanejamentoService(
    private val planejamentoRepository: PlanejamentoRepository,
    private val usuarioRepository: UsuarioRepository
) {

    /**
     * Cria um novo planejamento de aula
     * O planejamento é criado com status PENDENTE (aguardando aprovação)
     */
    fun criarPlanejamento(request: CreatePlanejamentoRequest, emailProfessor: String): PlanejamentoDTO {
        // Busca o professor (usuário logado)
        val professor = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        // Cria e salva o planejamento com status PENDENTE
        val planejamento = Planejamento(
            author = professor,
            titulo = request.titulo,
            area = request.area,
            descricao = request.descricao,
            status = StatusPlanejamento.PENDENTE,
            versao = 1,
            publico = request.publico
        )

        val planejamentoSalvo = planejamentoRepository.save(planejamento)
        return PlanejamentoDTO.fromEntity(planejamentoSalvo)
    }

    /**
     * Lista planejamentos baseado no perfil do usuário:
     * - Professor: Planos PUBLICADOS e PÚBLICOS de todos + TODOS os seus próprios planos
     * - Diretor/Admin: Todos os planos (públicos e privados)
     */
    fun listarPlanejamentos(emailUsuario: String, status: StatusPlanejamento?): List<PlanejamentoDTO> {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        val planejamentos = when {
            // Professor: planos públicos publicados + meus planos
            usuario.perfil == PerfilUsuario.PROFESSOR -> {
                if (status != null) {
                    // Com filtro de status
                    val planosComStatus = planejamentoRepository.findByStatus(status)
                    val meusPlanos = planejamentoRepository.findByAuthorId(usuario.id!!)

                    // Combina: planos públicos publicados com o status OU meus planos com o status
                    (planosComStatus.filter { it.status == StatusPlanejamento.PUBLICADO && it.publico } +
                     meusPlanos.filter { it.status == status }).distinctBy { it.id }
                } else {
                    // Sem filtro: apenas planos PÚBLICOS e PUBLICADOS + todos meus
                    val planosPublicos = planejamentoRepository.findByStatus(StatusPlanejamento.PUBLICADO)
                        .filter { it.publico }
                    val meusPlanos = planejamentoRepository.findByAuthorId(usuario.id!!)
                    (planosPublicos + meusPlanos).distinctBy { it.id }
                }
            }
            // Diretor/Admin: todos os planos (públicos e privados)
            else -> {
                if (status != null) {
                    planejamentoRepository.findByStatus(status)
                } else {
                    planejamentoRepository.findAll().toList()
                }
            }
        }

        return planejamentos
            .sortedByDescending { it.criadoEm }
            .map { PlanejamentoDTO.fromEntity(it) }
    }

    /**
     * Lista apenas os planejamentos do professor autenticado
     */
    fun listarMeusPlanejamentos(emailProfessor: String): List<PlanejamentoDTO> {
        val professor = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        val planejamentos = planejamentoRepository.findByAuthorId(professor.id!!)
        return planejamentos
            .sortedByDescending { it.criadoEm }
            .map { PlanejamentoDTO.fromEntity(it) }
    }

    /**
     * Busca planejamentos com filtros (palavra-chave, área, author, status)
     * Respeitando as mesmas regras de visibilidade do listarPlanejamentos
     */
    fun buscarComFiltros(
        emailUsuario: String,
        palavraChave: String?,
        area: String?,
        authorId: Long?,
        status: StatusPlanejamento?
    ): List<PlanejamentoDTO> {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        // Primeiro, busca respeitando a visibilidade
        var planejamentos = when {
            usuario.perfil == PerfilUsuario.PROFESSOR -> {
                val planosPublicos = planejamentoRepository.findByStatus(StatusPlanejamento.PUBLICADO)
                    .filter { it.publico }
                val meusPlanos = planejamentoRepository.findByAuthorId(usuario.id!!)
                (planosPublicos + meusPlanos).distinctBy { it.id }
            }
            else -> planejamentoRepository.findAll().toList()
        }

        // Aplica filtros
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

        return planejamentos
            .sortedByDescending { it.criadoEm }
            .map { PlanejamentoDTO.fromEntity(it) }
    }

    /**
     * Busca um planejamento por ID
     * Professor só pode ver planos públicos ou seus próprios
     */
    fun buscarPorId(id: Long, emailUsuario: String): PlanejamentoDTO {
        val planejamento = planejamentoRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Planejamento não encontrado") }

        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        // Valida permissão: professor só vê públicos ou seus próprios
        if (usuario.perfil == PerfilUsuario.PROFESSOR) {
            if (planejamento.status != StatusPlanejamento.PUBLICADO &&
                planejamento.author.id != usuario.id) {
                throw ForbiddenException("Você não tem permissão para visualizar este planejamento")
            }
        }

        return PlanejamentoDTO.fromEntity(planejamento)
    }

    /**
     * Atualiza um planejamento existente
     * - Incrementa a versão
     * - Volta o status para PENDENTE (aguardando nova aprovação)
     * - Apenas o autor pode atualizar
     */
    fun atualizarPlanejamento(
        id: Long,
        request: UpdatePlanejamentoRequest,
        emailProfessor: String
    ): PlanejamentoDTO {
        val planejamento = planejamentoRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Planejamento não encontrado") }

        val usuario = usuarioRepository.findByEmail(emailProfessor)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        // Valida permissão: apenas o autor pode atualizar
        if (planejamento.author.id != usuario.id) {
            throw ForbiddenException("Você não tem permissão para atualizar este planejamento")
        }

        // Não permite atualizar planos reprovados
        if (planejamento.status == StatusPlanejamento.REPROVADO) {
            throw ValidationException("Planejamentos reprovados não podem ser atualizados. Crie um novo planejamento.")
        }

        // Verifica se mudou de privado para público
        val mudouParaPublico = !planejamento.publico && request.publico

        // Atualiza os campos
        planejamento.titulo = request.titulo
        planejamento.area = request.area
        planejamento.descricao = request.descricao
        planejamento.publico = request.publico

        // Se estava publicado OU mudou para público, volta para pendente e incrementa versão
        if (planejamento.status == StatusPlanejamento.PUBLICADO ||
            planejamento.status == StatusPlanejamento.AGUARDANDO_AJUSTES ||
            mudouParaPublico) {
            planejamento.status = StatusPlanejamento.PENDENTE
            planejamento.versao += 1
        }

        val planejamentoAtualizado = planejamentoRepository.save(planejamento)
        return PlanejamentoDTO.fromEntity(planejamentoAtualizado)
    }

    /**
     * Deleta um planejamento
     * - Professor só pode deletar seus próprios planos não publicados
     * - Diretor/Admin podem deletar qualquer plano
     */
    fun deletarPlanejamento(id: Long, emailUsuario: String) {
        val planejamento = planejamentoRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Planejamento não encontrado") }

        val usuario = usuarioRepository.findByEmail(emailUsuario)
            .orElseThrow { ResourceNotFoundException("Usuário não encontrado") }

        // Valida permissão
        if (usuario.perfil == PerfilUsuario.PROFESSOR) {
            // Professor só pode deletar seus próprios
            if (planejamento.author.id != usuario.id) {
                throw ForbiddenException("Você não tem permissão para deletar este planejamento")
            }
            // Professor não pode deletar planos já publicados
            if (planejamento.status == StatusPlanejamento.PUBLICADO) {
                throw ValidationException("Planejamentos publicados não podem ser deletados")
            }
        }

        planejamentoRepository.delete(planejamento)
    }

    /**
     * Altera o status de um planejamento (usado por Diretor/Coordenador)
     * - PUBLICADO: Aprova e torna público
     * - REPROVADO: Rejeita definitivamente
     * - AGUARDANDO_AJUSTES: Solicita correções
     */
    fun alterarStatus(id: Long, request: AlterarStatusPlanejamentoRequest): PlanejamentoDTO {
        val planejamento = planejamentoRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Planejamento não encontrado") }

        // Validações de negócio
        if (planejamento.status == StatusPlanejamento.REPROVADO) {
            throw ValidationException("Planejamentos reprovados não podem ter o status alterado")
        }

        if (request.status == StatusPlanejamento.AGUARDANDO_AJUSTES && request.motivo.isNullOrBlank()) {
            throw ValidationException("Motivo é obrigatório ao solicitar ajustes")
        }

        // Só pode alterar status de planos PENDENTES
        if (planejamento.status != StatusPlanejamento.PENDENTE) {
            throw ValidationException("Apenas planejamentos pendentes podem ter o status alterado")
        }

        // Atualiza o status
        planejamento.status = request.status

        val planejamentoAtualizado = planejamentoRepository.save(planejamento)
        return PlanejamentoDTO.fromEntity(planejamentoAtualizado)
    }

    /**
     * Lista planejamentos pendentes (para aprovação)
     * Usado por Diretor/Coordenador
     */
    fun listarPlanejamentosPendentes(): List<PlanejamentoDTO> {
        val planejamentos = planejamentoRepository.findByStatus(StatusPlanejamento.PENDENTE)
        return planejamentos
            .sortedByDescending { it.criadoEm }
            .map { PlanejamentoDTO.fromEntity(it) }
    }
}
