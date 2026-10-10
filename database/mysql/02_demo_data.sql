-- OPCIONAL. Datos ficticios para probar la aplicaciÃ³n, no son el catÃ¡logo Oracle.
-- Ejecutar UNA VEZ en una base nueva. Los valores son ilustrativos.
USE nutrimaker;
START TRANSACTION;
INSERT INTO mealbase(name,meal_type,meal_group,calories,fat,carbohydrates,protein,calcium,iron) VALUES
('Avena de demostraciÃ³n','BREAKFAST','Demo',100,2,17,4,30,1),
('Fruta de demostraciÃ³n','SNACK','Demo',50,0,12,1,10,0.2),
('Arroz con pollo de demostraciÃ³n','LUNCH','Demo',100,2,13,8,15,0.5),
('Ensalada de demostraciÃ³n','DINNER','Demo',100,3,10,7,20,0.4);
INSERT INTO ingredient(name) VALUES ('Avena de demostraciÃ³n');
SET @ingredient_id = LAST_INSERT_ID();
INSERT INTO meal_ingredient(meal_base_id,ingredient_id,amount,unit)
SELECT meal_base_id,@ingredient_id,25,'g' FROM mealbase WHERE name='Avena de demostraciÃ³n';
COMMIT;
