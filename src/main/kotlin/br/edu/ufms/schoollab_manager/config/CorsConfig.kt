package br.edu.ufms.schoollab_manager.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig(
    @Value("\${app.cors.allowed-origins:*}") private val allowedOrigins: String
) {

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val origins = allowedOrigins.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val config = CorsConfiguration().apply {
            if (origins.size == 1 && origins[0] == "*") {
                addAllowedOriginPattern("*")
            } else {
                origins.forEach { addAllowedOriginPattern(it) }
            }
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Authorization")
            allowCredentials = true
            maxAge = 3600
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
