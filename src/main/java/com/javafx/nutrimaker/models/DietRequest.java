package com.javafx.nutrimaker.models;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class DietRequest {
    private int user_id;
    private int patient_id;
    private double calories;
    private double fat;
    private double cholesterol;
    private double sodium;
    private double carbohydrates;
    private double protein;
    private double calcium;
    private double iron;
    private String note;
    private String rest_day;
    private String target_gender;
    private int meals_per_day;
    private String creation_date; // <-- Nuevo campo

    public DietRequest(int userId, int patientId, ValoresNutricionales v,
                       String restDay, int mealsPerDay, String note) {
        this.user_id = userId;
        this.patient_id = patientId;
        this.calories = v.getCalories();
        this.fat = v.getFat();
        this.cholesterol = v.getCholesterol();
        this.sodium = v.getSodium();
        this.carbohydrates = v.getCarbohydrates();
        this.protein = v.getProtein();
        this.calcium = v.getCalcium();
        this.iron = v.getIron();
        this.rest_day = restDay == null ? null : restDay.toUpperCase(java.util.Locale.ROOT);
        this.meals_per_day = mealsPerDay;
        this.note = note;

        // Formato ISO 8601 en UTC
        this.creation_date = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }
}


