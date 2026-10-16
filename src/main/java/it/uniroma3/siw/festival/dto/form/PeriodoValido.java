package it.uniroma3.siw.festival.dto.form;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Vincolo di classe: la data di fine non può precedere la data di inizio.
 * È a livello di classe perché confronta due campi diversi dello stesso oggetto.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PeriodoValidoValidator.class)
public @interface PeriodoValido {

    String message() default "La data di fine non può precedere la data di inizio";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
