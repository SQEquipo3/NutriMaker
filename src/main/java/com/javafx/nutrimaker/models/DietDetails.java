package com.javafx.nutrimaker.models;

import java.math.BigDecimal;

/** Fila de la tabla DIETA, sin comidas ni paciente. */
public record DietDetails(
        int dietId,
        int userId,
        int patientId,
        BigDecimal calories,
        BigDecimal fat,
        BigDecimal cholesterol,
        BigDecimal sodium,
        BigDecimal carbohydrates,
        BigDecimal protein,
        BigDecimal calcium,
        BigDecimal iron,
        String note,
        String creationDate,
        String restDay,
        String targetGender,
        Integer mealsPerDay) {
}