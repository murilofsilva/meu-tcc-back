package br.edu.ufms.schoollab_manager.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "areas_conhecimento")
class AreaConhecimento(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 120, unique = true)
    var nome: String,

    @Column(nullable = false)
    var ativo: Boolean = true
)
