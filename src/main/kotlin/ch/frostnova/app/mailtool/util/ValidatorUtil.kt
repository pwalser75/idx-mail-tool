package ch.frostnova.app.mailtool.util

import jakarta.validation.Validation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import java.util.Locale

val validator: Validator = buildValidator()

fun validate(any: Any) {
    val constraintViolations = (if (any is Collection<*>) any.flatMap { validator.validate(it) }.toList()
    else validator.validate(any))

    if (constraintViolations.isNotEmpty()) {
        throw ValidationException(constraintViolations.joinToString("\n") { "${it.propertyPath}: (${it.invalidValue}) ${it.message}" })
    }
}

fun validateWithLogging(any: Any) {
    try {
        validate(any)
    } catch (ex: ValidationException) {
        System.err.println("Invalid data: \n${ex.message}")
        throw ex
    }
}

private fun buildValidator(): Validator {
    Locale.setDefault(Locale.ENGLISH)
    return Validation.buildDefaultValidatorFactory().validator
}
