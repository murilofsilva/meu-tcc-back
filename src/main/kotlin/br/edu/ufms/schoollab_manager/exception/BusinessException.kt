package br.edu.ufms.schoollab_manager.exception

/**
 * Exceção base para erros de regras de negócio
 */
open class BusinessException(
    message: String,
    val httpStatus: Int = 400
) : RuntimeException(message)

/**
 * Exceção para recurso não encontrado
 */
class ResourceNotFoundException(message: String) : BusinessException(message, 404)

/**
 * Exceção para conflito de dados
 */
class ConflictException(message: String) : BusinessException(message, 409)

/**
 * Exceção para validação de negócio
 */
class ValidationException(message: String) : BusinessException(message, 400)

/**
 * Exceção para acesso não autorizado
 */
class ForbiddenException(message: String) : BusinessException(message, 403)
