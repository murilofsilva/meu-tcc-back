package br.edu.ufms.schoollab_manager.security

import br.edu.ufms.schoollab_manager.repository.UsuarioRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val usuarioRepository: UsuarioRepository
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val usuario = usuarioRepository.findByEmail(email)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com email: $email") }

        if (!usuario.status) {
            throw UsernameNotFoundException("Usuário inativo")
        }

        val authorities = listOf(SimpleGrantedAuthority("ROLE_${usuario.perfil.name}"))

        return User(
            usuario.email,
            usuario.senha,
            authorities
        )
    }
}
