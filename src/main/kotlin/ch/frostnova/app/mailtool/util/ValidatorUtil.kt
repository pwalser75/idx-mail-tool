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
        throw ValidationException(
            constraintViolations
                .sortedBy { it.propertyPath.toString() }
                .joinToString("\n") { "${it.propertyPath}: (${it.invalidValue}) ${it.message}" })
    }
}

private fun buildValidator(): Validator {
    Locale.setDefault(Locale.ENGLISH)
    return Validation.buildDefaultValidatorFactory().validator
}
