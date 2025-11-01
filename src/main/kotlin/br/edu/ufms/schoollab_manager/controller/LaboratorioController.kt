package br.edu.ufms.schoollab_manager.controller

import br.edu.ufms.schoollab_manager.domain.entity.Laboratorio
import br.edu.ufms.schoollab_manager.dto.CreateLaboratorioRequest
import br.edu.ufms.schoollab_manager.dto.LaboratorioDTO
import br.edu.ufms.schoollab_manager.dto.UpdateLaboratorioRequest
import br.edu.ufms.schoollab_manager.repository.LaboratorioRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/laboratorios")
class LaboratorioController(
    private val laboratorioRepository: LaboratorioRepository
) {

    /**
     * Cadastra um novo laboratório
     * Apenas DIRETOR e ADMIN podem cadastrar
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun cadastrarLaboratorio(@Valid @RequestBody request: CreateLaboratorioRequest): ResponseEntity<Any> {
        if (laboratorioRepository.existsByNome(request.nome)) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(mapOf("mensagem" to "Já existe um laboratório com este nome"))
        }

        val laboratorio = Laboratorio(
            nome = request.nome,
            capacidade = request.capacidade,
            qtdEquipamentos = request.qtdEquipamentos
        )

        val laboratorioSalvo = laboratorioRepository.save(laboratorio)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(LaboratorioDTO.fromEntity(laboratorioSalvo))
    }

    /**
     * Lista todos os laboratórios
     * Todos os usuários autenticados podem listar
     */
    @GetMapping
    fun listarLaboratorios(@RequestParam(required = false) status: Boolean?): ResponseEntity<List<LaboratorioDTO>> {
        val laboratorios = if (status != null) {
            laboratorioRepository.findByStatusOrderByNomeAsc(status)
        } else {
            laboratorioRepository.findAllByOrderByNomeAsc()
        }

        val laboratoriosDTO = laboratorios.map { LaboratorioDTO.fromEntity(it) }

        return ResponseEntity.ok(laboratoriosDTO)
    }

    /**
     * Busca um laboratório por ID
     * Todos os usuários autenticados podem visualizar
     */
    @GetMapping("/{id}")
    fun buscarLaboratorio(@PathVariable id: Long): ResponseEntity<Any> {
        val laboratorio = laboratorioRepository.findById(id)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Laboratório não encontrado"))

        return ResponseEntity.ok(LaboratorioDTO.fromEntity(laboratorio))
    }

    /**
     * Atualiza um laboratório
     * Apenas DIRETOR e ADMIN podem atualizar
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun atualizarLaboratorio(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateLaboratorioRequest
    ): ResponseEntity<Any> {
        val laboratorio = laboratorioRepository.findById(id)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Laboratório não encontrado"))

        // Verifica se o novo nome já existe em outro laboratório
        if (request.nome != null && request.nome != laboratorio.nome) {
            if (laboratorioRepository.existsByNome(request.nome)) {
                return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(mapOf("mensagem" to "Já existe um laboratório com este nome"))
            }
            laboratorio.nome = request.nome
        }

        request.capacidade?.let { laboratorio.capacidade = it }
        request.qtdEquipamentos?.let { laboratorio.qtdEquipamentos = it }

        val laboratorioAtualizado = laboratorioRepository.save(laboratorio)

        return ResponseEntity.ok(LaboratorioDTO.fromEntity(laboratorioAtualizado))
    }

    /**
     * Ativa ou desativa um laboratório
     * Apenas DIRETOR e ADMIN podem alterar status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun alterarStatus(
        @PathVariable id: Long,
        @RequestBody body: Map<String, Boolean>
    ): ResponseEntity<Any> {
        val laboratorio = laboratorioRepository.findById(id)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Laboratório não encontrado"))

        val novoStatus = body["status"]
            ?: return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensagem" to "Campo 'status' é obrigatório"))

        laboratorio.status = novoStatus
        val laboratorioAtualizado = laboratorioRepository.save(laboratorio)

        return ResponseEntity.ok(LaboratorioDTO.fromEntity(laboratorioAtualizado))
    }

    /**
     * Deleta um laboratório
     * Apenas DIRETOR e ADMIN podem deletar
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRETOR', 'ADMIN')")
    fun deletarLaboratorio(@PathVariable id: Long): ResponseEntity<Any> {
        val laboratorio = laboratorioRepository.findById(id)
            .orElse(null)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensagem" to "Laboratório não encontrado"))

        try {
            laboratorioRepository.delete(laboratorio)
            return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build()
        } catch (e: Exception) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(mapOf("mensagem" to "Não é possível deletar este laboratório pois existem reservas vinculadas a ele"))
        }
    }
}
