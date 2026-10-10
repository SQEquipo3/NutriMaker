USE nutrimaker;
SHOW TABLES;
SELECT user_id,email FROM useraccount;
SELECT * FROM patient;
SELECT meal_type,COUNT(*) AS opciones,MIN(calories) AS calorias_minimas FROM mealbase GROUP BY meal_type;
SELECT d.diet_id,u.email,p.name,d.creation_date,d.calories
FROM diet d JOIN useraccount u ON u.user_id=d.user_id
JOIN patient p ON p.patient_id=d.patient_id ORDER BY d.diet_id DESC;
SELECT dm.diet_id,dm.day,dm.time_of_day,dm.meal_type,m.name
FROM diet_meal dm JOIN mealbase m ON m.meal_base_id=dm.meal_base_id
ORDER BY dm.diet_id,dm.day,dm.time_of_day;
