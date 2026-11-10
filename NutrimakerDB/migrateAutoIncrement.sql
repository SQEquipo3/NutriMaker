-- Issue #73: generación automática de identificadores.
-- Ejecutar únicamente después de respaldar la base de datos.
-- No elimina registros ni modifica los identificadores existentes.

ALTER TABLE USUARIO
    MODIFY COLUMN ID_USUARIO INT NOT NULL AUTO_INCREMENT;

ALTER TABLE PACIENTE
    MODIFY COLUMN ID_PACIENTE INT NOT NULL AUTO_INCREMENT;

ALTER TABLE DIETA
    MODIFY COLUMN ID_DIETA INT NOT NULL AUTO_INCREMENT;

ALTER TABLE COMIDA
    MODIFY COLUMN ID_COMIDA INT NOT NULL AUTO_INCREMENT;
