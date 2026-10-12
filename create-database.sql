-- Ejecutar en pgAdmin conectado a postgres como administrador.
-- Sustituir la contraseña antes de ejecutar. No usar la cuenta postgres en la aplicación.
CREATE ROLE vitna LOGIN PASSWORD 'CAMBIAR_CONTRASENA';
CREATE DATABASE vitna OWNER vitna;
