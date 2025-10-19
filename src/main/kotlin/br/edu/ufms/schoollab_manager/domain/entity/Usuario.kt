package br.edu.ufms.schoollab_manager.domain.entity

import br.edu.ufms.schoollab_manager.domain.enums.PerfilUsuario
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "usuario")
class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 150)
    var nome: String,

    @Column(nullable = false, length = 150, unique = true)
    var email: String,

    @Column(nullable = false, length = 255)
    var senha: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var perfil: PerfilUsuario,

    @Column(nullable = false)
    var status: Boolean = true,

    @Column(name = "criado_em", nullable = false, updatable = false)
    val criadoEm: Instant = Instant.now()
)
