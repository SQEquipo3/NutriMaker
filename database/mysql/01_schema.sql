-- MySQL 8.0.16+ / 8.4. Esquema reconstruido desde los modelos Java.
-- No borra tablas ni importa datos de Oracle. Ejecutar en una base nueva.
CREATE DATABASE IF NOT EXISTS nutrimaker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nutrimaker;
CREATE TABLE IF NOT EXISTS useraccount (
    user_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password VARCHAR(60) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS patient (
    patient_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    age INT NOT NULL CHECK (age >= 0),
    weight DECIMAL(8,3) NOT NULL CHECK (weight > 0),
    height DECIMAL(8,3) NOT NULL CHECK (height > 0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS mealbase (
    meal_base_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    meal_type VARCHAR(16) NOT NULL CHECK (meal_type IN ('BREAKFAST','SNACK','LUNCH','DINNER')),
    meal_group VARCHAR(100) NOT NULL DEFAULT '',
    calories DECIMAL(12,3) NOT NULL DEFAULT 0,
    fat DECIMAL(12,3) NOT NULL DEFAULT 0,
    cholesterol DECIMAL(12,3) NOT NULL DEFAULT 0,
    sodium DECIMAL(12,3) NOT NULL DEFAULT 0,
    carbohydrates DECIMAL(12,3) NOT NULL DEFAULT 0,
    protein DECIMAL(12,3) NOT NULL DEFAULT 0,
    calcium DECIMAL(12,3) NOT NULL DEFAULT 0,
    iron DECIMAL(12,3) NOT NULL DEFAULT 0,
    CHECK (calories >= 1),
    INDEX idx_meal_selection (meal_type, calories)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS ingredient (
    ingredient_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS meal_ingredient (
    meal_base_id INT NOT NULL,
    ingredient_id INT NOT NULL,
    amount DECIMAL(12,3) NOT NULL CHECK (amount > 0),
    unit VARCHAR(30) NOT NULL DEFAULT 'g',
    PRIMARY KEY (meal_base_id, ingredient_id),
    FOREIGN KEY (meal_base_id) REFERENCES mealbase(meal_base_id) ON DELETE CASCADE,
    FOREIGN KEY (ingredient_id) REFERENCES ingredient(ingredient_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS diet (
    diet_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    patient_id INT NOT NULL,
    creation_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    calories DECIMAL(12,3) NOT NULL DEFAULT 0,
    fat DECIMAL(12,3) NOT NULL DEFAULT 0,
    cholesterol DECIMAL(12,3) NOT NULL DEFAULT 0,
    sodium DECIMAL(12,3) NOT NULL DEFAULT 0,
    carbohydrates DECIMAL(12,3) NOT NULL DEFAULT 0,
    protein DECIMAL(12,3) NOT NULL DEFAULT 0,
    calcium DECIMAL(12,3) NOT NULL DEFAULT 0,
    iron DECIMAL(12,3) NOT NULL DEFAULT 0,
    note TEXT NOT NULL,
    rest_day VARCHAR(16),
    target_gender VARCHAR(30),
    meals_per_day INT NOT NULL CHECK (meals_per_day BETWEEN 1 AND 6),
    FOREIGN KEY (user_id) REFERENCES useraccount(user_id),
    FOREIGN KEY (patient_id) REFERENCES patient(patient_id),
    INDEX idx_diet_user (user_id, diet_id),
    INDEX idx_diet_patient (patient_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS diet_meal (
    diet_meal_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    diet_id INT NOT NULL,
    meal_base_id INT NOT NULL,
    day DATE NOT NULL,
    time_of_day DATETIME NOT NULL,
    meal_type VARCHAR(16) NOT NULL CHECK (meal_type IN ('BREAKFAST','SNACK','LUNCH','DINNER')),
    FOREIGN KEY (diet_id) REFERENCES diet(diet_id) ON DELETE CASCADE,
    FOREIGN KEY (meal_base_id) REFERENCES mealbase(meal_base_id),
    INDEX idx_diet_schedule (diet_id, day, time_of_day)
) ENGINE=InnoDB;
