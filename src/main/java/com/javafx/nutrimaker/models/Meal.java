package com.javafx.nutrimaker.models;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

public class Meal {
    @SerializedName("meal_base_id")
    private int mealBaseId;
    @SerializedName("meal_name")
    private String name;
    @SerializedName("base_meal_type")
    private String mealType;
    @SerializedName("meal_group")
    private String mealGroup;
    @SerializedName("meal_time")
    private String mealTime;
    @SerializedName("meal_calories")
    private double calories;
    @SerializedName("meal_fat")
    private double fat;
    @SerializedName("meal_cholesterol")
    private double cholesterol;
    @SerializedName("meal_sodium")
    private double sodium;
    @SerializedName("meal_carbohydrates")
    private double carbohydrates;
    @SerializedName("meal_protein")
    private double protein;
    @SerializedName("meal_calcium")
    private double calcium;
    @SerializedName("meal_iron")
    private double iron;

    private Date day;
    private Date timeOfDay;

    private List<Ingredient> ingredients;

    public Meal(int mealBaseId, String name, String mealType, String mealGroup,
                double calories, double fat, double cholesterol, double sodium,
                double carbohydrates, double protein, double calcium, double iron,
                Date day, Date timeOfDay, List<Ingredient> ingredients) {
        this.mealBaseId = mealBaseId;
        this.name = name;
        this.mealType = mealType;
        this.mealGroup = mealGroup;
        this.calories = calories;
        this.fat = fat;
        this.cholesterol = cholesterol;
        this.sodium = sodium;
        this.carbohydrates = carbohydrates;
        this.protein = protein;
        this.calcium = calcium;
        this.iron = iron;
        this.day = day;
        this.timeOfDay = timeOfDay;
        this.ingredients = ingredients;
    }

    public Meal() {

    }

    public int getMealBaseId() {
        return mealBaseId;
    }

    public void setMealBaseId(int mealBaseId) {
        this.mealBaseId = mealBaseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getMealGroup() {
        return mealGroup;
    }

    public void setMealGroup(String mealGroup) {
        this.mealGroup = mealGroup;
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    public double getFat() {
        return fat;
    }

    public void setFat(double fat) {
        this.fat = fat;
    }

    public double getCholesterol() {
        return cholesterol;
    }

    public void setCholesterol(double cholesterol) {
        this.cholesterol = cholesterol;
    }

    public double getSodium() {
        return sodium;
    }

    public void setSodium(double sodium) {
        this.sodium = sodium;
    }

    public double getCarbohydrates() {
        return carbohydrates;
    }

    public void setCarbohydrates(double carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    public double getProtein() {
        return protein;
    }

    public void setProtein(double protein) {
        this.protein = protein;
    }

    public double getCalcium() {
        return calcium;
    }

    public void setCalcium(double calcium) {
        this.calcium = calcium;
    }

    public double getIron() {
        return iron;
    }

    public void setIron(double iron) {
        this.iron = iron;
    }

    public Date getDay() {
        return day;
    }

    public void setDay(Date day) {
        this.day = day;
    }

    public Date getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(Date timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }
    public String getMealTime() {
        return mealTime;
    }

    public void setMealTime(String mealTime) {
        this.mealTime = mealTime;
    }
}
