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

    @Column(name = "qtd_equipamentos", nullable = false)
    var qtdEquipamentos: Int = 0,

    @Column(nullable = false)
    var status: Boolean = true
) {
    init {
        require(capacidade >= 0) { "Capacidade deve ser maior ou igual a zero" }
        require(qtdEquipamentos >= 0) { "Quantidade de equipamentos deve ser maior ou igual a zero" }
    }
}
