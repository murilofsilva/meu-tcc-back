package br.edu.ufms.schoollab_manager.service

import br.edu.ufms.schoollab_manager.repository.PlanejamentoRepository
import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class PlanejamentoCodeGenerator(
    private val planejamentoRepository: PlanejamentoRepository
) {
    // Alfabeto sem caracteres ambíguos (0/O, 1/I/L) para reduzir erro de digitação.
    private val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray()
    private val random = SecureRandom()
    private val length = 8
    private val maxAttempts = 10

    fun generateUnique(): String {
        repeat(maxAttempts) {
            val code = randomCode()
            if (!planejamentoRepository.existsByCodigo(code)) {
                return code
            }
        }
        throw IllegalStateException("Não foi possível gerar um código único após $maxAttempts tentativas")
    }

    private fun randomCode(): String {
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(alphabet[random.nextInt(alphabet.size)])
        }
        return sb.toString()
    }
}
