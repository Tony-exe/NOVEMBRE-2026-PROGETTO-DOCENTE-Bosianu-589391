package it.uniroma3.siw.festival.dto.form;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PeriodoValidoValidator implements ConstraintValidator<PeriodoValido, FestivalForm> {

    @Override
    public boolean isValid(FestivalForm form, ConstraintValidatorContext context) {
        // Date mancanti: se ne occupa @NotNull sui singoli campi
        if (form.getDataInizio() == null || form.getDataFine() == null) {
            return true;
        }
        if (!form.getDataFine().isBefore(form.getDataInizio())) {
            return true;
        }
        // Associo l'errore al campo dataFine, così Thymeleaf lo mostra accanto a quel campo
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("dataFine")
                .addConstraintViolation();
        return false;
    }
}
