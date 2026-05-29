package br.edu.ufms.schoollab_manager.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "laboratorio")
class Laboratorio(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 120, unique = true)
    var nome: String,

    @Column(nullable = false)
    var capacidade: Int,

    // Coluna mantém o nome legado `qtd_equipamentos` para preservar dados existentes.
    @Column(name = "qtd_equipamentos", nullable = false)
    var quantidadeComputadores: Int = 0,

    @Column(columnDefinition = "TEXT")
    var descricao: String? = null,

    @Column(nullable = false)
    var status: Boolean = true
) {
    init {
        require(capacidade >= 0) { "Capacidade deve ser maior ou igual a zero" }
        require(quantidadeComputadores >= 0) { "Quantidade de computadores deve ser maior ou igual a zero" }
    }
}
