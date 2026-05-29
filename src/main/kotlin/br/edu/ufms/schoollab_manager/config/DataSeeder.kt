package br.edu.ufms.schoollab_manager.config

import br.edu.ufms.schoollab_manager.domain.entity.AreaConhecimento
import br.edu.ufms.schoollab_manager.repository.AreaConhecimentoRepository
import br.edu.ufms.schoollab_manager.repository.PlanejamentoRepository
import br.edu.ufms.schoollab_manager.service.PlanejamentoCodeGenerator
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DataSeeder(
    private val areaRepository: AreaConhecimentoRepository,
    private val planejamentoRepository: PlanejamentoRepository,
    private val codeGenerator: PlanejamentoCodeGenerator
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(DataSeeder::class.java)

    private val areasIniciais = listOf(
        "Matemática", "Português", "Ciências", "História", "Geografia",
        "Computação", "Ética", "Arte", "Educação Física", "Inglês"
    )

    @Transactional
    override fun run(vararg args: String?) {
        seedAreasConhecimento()
        migrarPlanosSemArea()
        migrarPlanosSemCodigo()
    }

    private fun seedAreasConhecimento() {
        val criadas = areasIniciais.count { nome ->
            if (!areaRepository.existsByNome(nome)) {
                areaRepository.save(AreaConhecimento(nome = nome, ativo = true))
                true
            } else false
        }
        if (criadas > 0) {
            log.info("DataSeeder: $criadas disciplina(s) inicial(is) cadastrada(s)")
        }
    }

    private fun migrarPlanosSemArea() {
        val planosSemFk = planejamentoRepository.findAll()
            .filter { it.areaConhecimento == null && it.area.isNotBlank() }
        if (planosSemFk.isEmpty()) return

        val areasPorNome = areaRepository.findAll().associateBy { it.nome }
        var migrados = 0
        planosSemFk.forEach { plano ->
            val area = areasPorNome[plano.area] ?: areaRepository.save(
                AreaConhecimento(nome = plano.area, ativo = true)
            )
            plano.areaConhecimento = area
            planejamentoRepository.save(plano)
            migrados++
        }
        log.info("DataSeeder: $migrados plano(s) migrado(s) para FK de área")
    }

    private fun migrarPlanosSemCodigo() {
        val planosSemCodigo = planejamentoRepository.findAll()
            .filter { it.codigo.isNullOrBlank() }
        if (planosSemCodigo.isEmpty()) return

        planosSemCodigo.forEach { plano ->
            plano.codigo = codeGenerator.generateUnique()
            planejamentoRepository.save(plano)
        }
        log.info("DataSeeder: ${planosSemCodigo.size} plano(s) receberam código gerado")
    }
}
