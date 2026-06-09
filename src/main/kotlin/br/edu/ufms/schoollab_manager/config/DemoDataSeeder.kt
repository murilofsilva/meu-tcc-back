package br.edu.ufms.schoollab_manager.config

import br.edu.ufms.schoollab_manager.domain.entity.AreaConhecimento
import br.edu.ufms.schoollab_manager.domain.entity.DisponibilidadeLaboratorio
import br.edu.ufms.schoollab_manager.domain.entity.Indisponibilidade
import br.edu.ufms.schoollab_manager.domain.entity.Laboratorio
import br.edu.ufms.schoollab_manager.domain.entity.Planejamento
import br.edu.ufms.schoollab_manager.domain.entity.Reserva
import br.edu.ufms.schoollab_manager.domain.entity.Usuario
import br.edu.ufms.schoollab_manager.domain.enums.PerfilUsuario
import br.edu.ufms.schoollab_manager.domain.enums.StatusPlanejamento
import br.edu.ufms.schoollab_manager.domain.enums.StatusReserva
import br.edu.ufms.schoollab_manager.repository.AreaConhecimentoRepository
import br.edu.ufms.schoollab_manager.repository.DisponibilidadeLaboratorioRepository
import br.edu.ufms.schoollab_manager.repository.IndisponibilidadeRepository
import br.edu.ufms.schoollab_manager.repository.LaboratorioRepository
import br.edu.ufms.schoollab_manager.repository.PlanejamentoRepository
import br.edu.ufms.schoollab_manager.repository.ReservaRepository
import br.edu.ufms.schoollab_manager.repository.UsuarioRepository
import br.edu.ufms.schoollab_manager.service.CompetenciasComputacao
import br.edu.ufms.schoollab_manager.service.PlanejamentoCodeGenerator
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Popula a base com um conjunto de dados de demonstração suficiente para testes
 * (usuários, laboratórios, agendas, planejamentos e reservas).
 *
 * Características importantes:
 *  - Roda na inicialização em qualquer ambiente (dev e prod), pois é um CommandLineRunner.
 *  - É IDEMPOTENTE: usa o usuário [EMAIL_DIRETOR] como marcador. Se ele já existir, todo
 *    o seeding é pulado — ou seja, em produção os dados são criados apenas na primeira
 *    inicialização e nunca sobrescrevem dados reais inseridos depois.
 *  - Pode ser desligado explicitamente via `app.seed.demo.enabled=false`
 *    (env var `APP_SEED_DEMO_ENABLED=false`).
 *
 * Executa após o [DataSeeder] (@Order(1)), que garante as disciplinas/áreas iniciais.
 */
@Component
@Order(2)
class DemoDataSeeder(
    private val usuarioRepository: UsuarioRepository,
    private val laboratorioRepository: LaboratorioRepository,
    private val reservaRepository: ReservaRepository,
    private val planejamentoRepository: PlanejamentoRepository,
    private val areaRepository: AreaConhecimentoRepository,
    private val disponibilidadeRepository: DisponibilidadeLaboratorioRepository,
    private val indisponibilidadeRepository: IndisponibilidadeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val codeGenerator: PlanejamentoCodeGenerator,
    @Value("\${app.seed.demo.enabled:true}") private val demoSeedEnabled: Boolean
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(DemoDataSeeder::class.java)
    private val zona: ZoneId = ZoneId.of("America/Cuiaba")
    private val senhaPadrao = "senha123"

    companion object {
        const val EMAIL_DIRETOR = "diretor@email.com"
        const val EMAIL_PROFESSOR = "professor@email.com"
    }

    @Transactional
    override fun run(vararg args: String?) {
        if (!demoSeedEnabled) {
            log.info("DemoDataSeeder: desabilitado por configuração (app.seed.demo.enabled=false).")
            return
        }

        // Marcador de idempotência: se o diretor demo já existe, presume-se que o
        // seeding já foi feito anteriormente. Não recria nada.
        if (usuarioRepository.existsByEmail(EMAIL_DIRETOR)) {
            log.info("DemoDataSeeder: dados de demonstração já presentes (encontrado $EMAIL_DIRETOR). Nada a fazer.")
            return
        }

        log.info("DemoDataSeeder: base sem dados de demonstração. Populando...")

        val usuarios = seedUsuarios()
        val laboratorios = seedLaboratorios()
        seedAgendas(laboratorios)
        val planejamentos = seedPlanejamentos(usuarios)
        seedReservas(usuarios, laboratorios, planejamentos)

        log.info(
            "DemoDataSeeder: concluído — {} usuários, {} laboratórios, {} planejamentos, {} reservas.",
            usuarios.size, laboratorios.size, planejamentos.size, reservaRepository.count()
        )
        log.info("DemoDataSeeder: logins de teste — diretor/professor/admin@email.com (senha: $senhaPadrao).")
    }

    // --------------------------------------------------------------------- Usuários

    private fun seedUsuarios(): Map<String, Usuario> {
        val definicoes = listOf(
            Triple("Administrador do Sistema", "admin@email.com", PerfilUsuario.ADMIN),
            Triple("Diretora Demonstração", EMAIL_DIRETOR, PerfilUsuario.DIRETOR),
            Triple("Professor Demonstração", EMAIL_PROFESSOR, PerfilUsuario.PROFESSOR),
            Triple("Ana Souza", "ana.professora@email.com", PerfilUsuario.PROFESSOR),
            Triple("Bruno Lima", "bruno.professor@email.com", PerfilUsuario.PROFESSOR),
            Triple("Carla Mendes", "carla.professora@email.com", PerfilUsuario.PROFESSOR)
        )

        return definicoes.associate { (nome, email, perfil) ->
            val usuario = usuarioRepository.findByEmail(email).orElseGet {
                usuarioRepository.save(
                    Usuario(
                        nome = nome,
                        email = email,
                        senha = passwordEncoder.encode(senhaPadrao),
                        perfil = perfil,
                        status = true
                    )
                )
            }
            email to usuario
        }
    }

    // ---------------------------------------------------------------- Laboratórios

    private fun seedLaboratorios(): List<Laboratorio> {
        val definicoes = listOf(
            LabDef("Laboratório de Informática 1", 30, 30,
                "Sala com 30 computadores desktop, projetor e quadro branco.", true),
            LabDef("Laboratório de Informática 2", 25, 20,
                "20 notebooks, lousa digital interativa e acesso à internet de alta velocidade.", true),
            LabDef("Sala Maker", 20, 6,
                "Espaço maker com impressora 3D, kit de robótica Arduino e bancadas de prototipagem.", true),
            LabDef("Laboratório Móvel (Chromebooks)", 35, 35,
                "Carrinho com 35 Chromebooks para uso em qualquer sala.", false)
        )

        return definicoes.map { def ->
            laboratorioRepository.findByNome(def.nome).orElseGet {
                laboratorioRepository.save(
                    Laboratorio(
                        nome = def.nome,
                        capacidade = def.capacidade,
                        quantidadeComputadores = def.computadores,
                        descricao = def.descricao,
                        status = def.ativo
                    )
                )
            }
        }
    }

    private data class LabDef(
        val nome: String,
        val capacidade: Int,
        val computadores: Int,
        val descricao: String,
        val ativo: Boolean
    )

    // ------------------------------------------------------- Disponibilidades/bloqueios

    private fun seedAgendas(laboratorios: List<Laboratorio>) {
        val diasUteis = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )

        // Lab 1 e Lab 2 ganham agenda semanal (manhã e tarde); Sala Maker fica
        // sem janelas (horário irrestrito), exercitando os dois cenários.
        laboratorios.take(2).forEach { lab ->
            if (disponibilidadeRepository.findByLaboratorioIdOrderByDiaSemanaAscHoraInicioAsc(lab.id!!).isNotEmpty()) {
                return@forEach
            }
            diasUteis.forEach { dia ->
                disponibilidadeRepository.save(
                    DisponibilidadeLaboratorio(
                        laboratorio = lab,
                        diaSemana = dia,
                        horaInicio = LocalTime.of(8, 0),
                        horaFim = LocalTime.of(12, 0)
                    )
                )
                disponibilidadeRepository.save(
                    DisponibilidadeLaboratorio(
                        laboratorio = lab,
                        diaSemana = dia,
                        horaInicio = LocalTime.of(13, 0),
                        horaFim = LocalTime.of(17, 0)
                    )
                )
            }
        }

        // Um bloqueio programado de exemplo no Lab 1 (formação de professores).
        val lab1 = laboratorios.first()
        if (indisponibilidadeRepository.findByLaboratorioIdOrderByInicioAsc(lab1.id!!).isEmpty()) {
            val proximaSegunda = LocalDate.now(zona).with(DayOfWeek.MONDAY).plusWeeks(1)
            indisponibilidadeRepository.save(
                Indisponibilidade(
                    laboratorio = lab1,
                    inicio = instant(proximaSegunda, 8, 0),
                    fim = instant(proximaSegunda.plusDays(2), 17, 0),
                    motivo = "Formação de professores - laboratório reservado para capacitação."
                )
            )
        }
    }

    // ------------------------------------------------------------- Planejamentos

    private fun seedPlanejamentos(usuarios: Map<String, Usuario>): List<Planejamento> {
        val professor = usuarios.getValue(EMAIL_PROFESSOR)
        val ana = usuarios.getValue("ana.professora@email.com")
        val bruno = usuarios.getValue("bruno.professor@email.com")
        val carla = usuarios.getValue("carla.professora@email.com")

        val definicoes = listOf(
            PlanDef(
                author = professor,
                titulo = "Introdução à Lógica de Programação com Scratch",
                area = "Computação",
                descricao = "Aula prática introdutória ao pensamento computacional usando o Scratch. " +
                        "Os alunos criam um pequeno jogo, exercitando sequências, repetições e condicionais.",
                status = StatusPlanejamento.PUBLICADO,
                publico = true,
                competencias = setOf(
                    CompetenciasComputacao.PENSAMENTO_COMPUTACIONAL,
                    CompetenciasComputacao.MUNDO_DIGITAL
                )
            ),
            PlanDef(
                author = ana,
                titulo = "Cidadania Digital e Segurança na Internet",
                area = "Computação",
                descricao = "Discussão sobre uso ético e seguro da internet, privacidade de dados e " +
                        "combate à desinformação, com atividades em grupo e estudo de casos reais.",
                status = StatusPlanejamento.PUBLICADO,
                publico = true,
                competencias = setOf(CompetenciasComputacao.CULTURA_DIGITAL),
                utilizaAcessibilidade = true,
                descricaoAcessibilidade = "Materiais com texto em alto contraste e compatíveis com leitor " +
                        "de tela; vídeos com legenda e audiodescrição."
            ),
            PlanDef(
                author = bruno,
                titulo = "Gráficos e Estatística com Planilhas",
                area = "Matemática",
                descricao = "Uso de planilhas eletrônicas para coletar dados de uma pesquisa da turma, " +
                        "construir tabelas de frequência e interpretar gráficos.",
                status = StatusPlanejamento.PUBLICADO,
                publico = true
            ),
            PlanDef(
                author = professor,
                titulo = "Produção de Podcast sobre História Local",
                area = "História",
                descricao = "Os alunos pesquisam a história do bairro e produzem um episódio de podcast, " +
                        "trabalhando roteiro, gravação e edição de áudio.",
                status = StatusPlanejamento.PENDENTE,
                publico = true,
                competencias = setOf(CompetenciasComputacao.CULTURA_DIGITAL)
            ),
            PlanDef(
                author = carla,
                titulo = "Robótica Educacional: primeiros circuitos",
                area = "Ciências",
                descricao = "Introdução a circuitos eletrônicos simples e montagem de protótipos com " +
                        "kit de robótica, integrando ciências e tecnologia.",
                status = StatusPlanejamento.AGUARDANDO_AJUSTES,
                publico = true,
                competencias = setOf(
                    CompetenciasComputacao.PENSAMENTO_COMPUTACIONAL,
                    CompetenciasComputacao.MUNDO_DIGITAL
                )
            ),
            PlanDef(
                author = ana,
                titulo = "Rascunho - Sequência didática de leitura",
                area = "Português",
                descricao = "Planejamento ainda em elaboração para uma sequência de leitura compartilhada. " +
                        "Mantido privado enquanto não está finalizado.",
                status = StatusPlanejamento.PENDENTE,
                publico = false
            )
        )

        return definicoes.map { def ->
            val area = resolverArea(def.area)
            planejamentoRepository.save(
                Planejamento(
                    author = def.author,
                    titulo = def.titulo,
                    codigo = codeGenerator.generateUnique(),
                    areaConhecimento = area,
                    area = area.nome,
                    descricao = def.descricao,
                    status = def.status,
                    versao = 1,
                    publico = def.publico,
                    mobilizaCompetenciasComputacao = def.competencias.isNotEmpty(),
                    competenciasComputacao = def.competencias.toMutableSet(),
                    utilizaRecursosAcessibilidade = def.utilizaAcessibilidade,
                    descricaoRecursosAcessibilidade = def.descricaoAcessibilidade
                )
            )
        }
    }

    private data class PlanDef(
        val author: Usuario,
        val titulo: String,
        val area: String,
        val descricao: String,
        val status: StatusPlanejamento,
        val publico: Boolean,
        val competencias: Set<String> = emptySet(),
        val utilizaAcessibilidade: Boolean = false,
        val descricaoAcessibilidade: String? = null
    )

    private fun resolverArea(nome: String): AreaConhecimento =
        areaRepository.findByNome(nome).orElseGet {
            areaRepository.save(AreaConhecimento(nome = nome, ativo = true))
        }

    // ------------------------------------------------------------------ Reservas

    private fun seedReservas(
        usuarios: Map<String, Usuario>,
        laboratorios: List<Laboratorio>,
        planejamentos: List<Planejamento>
    ) {
        val professor = usuarios.getValue(EMAIL_PROFESSOR)
        val ana = usuarios.getValue("ana.professora@email.com")
        val bruno = usuarios.getValue("bruno.professor@email.com")
        val carla = usuarios.getValue("carla.professora@email.com")

        val lab1 = laboratorios[0]
        val lab2 = laboratorios[1]
        val labMaker = laboratorios[2]

        val planoPorTitulo = planejamentos.associateBy { it.titulo }
        val hoje = LocalDate.now(zona)

        val definicoes = listOf(
            // Próxima reserva aprovada, com planejamento vinculado.
            ReservaDef(lab1, professor, hoje.plusDays(2), 8, 10,
                "Aula de lógica com Scratch", "9º A",
                StatusReserva.APROVADO, null,
                planoPorTitulo["Introdução à Lógica de Programação com Scratch"]),
            // Pendente de aprovação.
            ReservaDef(lab1, ana, hoje.plusDays(3), 14, 16,
                "Cidadania digital", "8º B",
                StatusReserva.PENDENTE, null,
                planoPorTitulo["Cidadania Digital e Segurança na Internet"]),
            // Aguardando ajustes (gestor pediu correção).
            ReservaDef(lab2, bruno, hoje.plusDays(1), 9, 11,
                "Estatística com planilhas", "1º EM",
                StatusReserva.AGUARDANDO_AJUSTES,
                "Ajustar o horário: choca com a reunião pedagógica. Reagende para a tarde.",
                planoPorTitulo["Gráficos e Estatística com Planilhas"]),
            // Reserva passada já realizada (aprovada).
            ReservaDef(lab2, professor, hoje.minusDays(5), 10, 12,
                "Produção de podcast - gravação", "7º A",
                StatusReserva.APROVADO, null,
                planoPorTitulo["Produção de Podcast sobre História Local"]),
            // Reprovada com justificativa.
            ReservaDef(labMaker, carla, hoje.plusDays(5), 13, 15,
                "Robótica - circuitos", "2º EM",
                StatusReserva.REPROVADO,
                "Laboratório em manutenção na data solicitada. Favor escolher outro dia.",
                planoPorTitulo["Robótica Educacional: primeiros circuitos"]),
            // Cancelada pelo professor.
            ReservaDef(lab1, professor, hoje.minusDays(2), 8, 9,
                "Aula cancelada", "9º A",
                StatusReserva.CANCELADO, null, null)
        )

        definicoes.forEach { def ->
            reservaRepository.save(
                Reserva(
                    laboratorio = def.lab,
                    professor = def.professor,
                    inicio = instant(def.data, def.horaInicio, 0),
                    fim = instant(def.data, def.horaFim, 0),
                    titulo = def.titulo,
                    turma = def.turma,
                    descricao = null,
                    planejamento = def.planejamento,
                    status = def.status,
                    motivoStatus = def.motivo
                )
            )
        }
    }

    private data class ReservaDef(
        val lab: Laboratorio,
        val professor: Usuario,
        val data: LocalDate,
        val horaInicio: Int,
        val horaFim: Int,
        val titulo: String,
        val turma: String?,
        val status: StatusReserva,
        val motivo: String?,
        val planejamento: Planejamento?
    )

    // ------------------------------------------------------------------- Helpers

    private fun instant(data: LocalDate, hora: Int, minuto: Int): Instant =
        data.atTime(hora, minuto).atZone(zona).toInstant()
}
