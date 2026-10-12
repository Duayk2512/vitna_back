-- Authorized one-time test access. Passwords are private; only BCrypt hashes are stored here.
-- Flyway records this migration and does not repeat it on restart.
DO $bootstrap$
DECLARE target RECORD;
BEGIN
 FOR target IN SELECT * FROM (VALUES
('admin@vitna.pe', 'ADMIN', '$2a$12$mplmguvkatrjz4aLzZzRjuvxvGtUMwPHndyDx3VBJo9poULv2/0E2'),
('cliente.test@vitna.pe', 'CLIENTE', '$2a$12$vEB.E7mOIlZE7LmGvy6wg.9dLronz9b.CXPOYcQNWSqKup0BnzMxa'),
('almacenero.test@vitna.pe', 'ALMACENERO', '$2a$12$5ybJUkINxVpaw8uYJtgOg.mqkuwocYiKtIN04DHWZpBJhldWhAmn.'),
('jefe_produccion.test@vitna.pe', 'JEFE_PRODUCCION', '$2a$12$ae7iLwDaLNJcu3Vqi9KeguM2ZuLouGKl5AQSwHctpKV.ysGuc2ubm'),
('trabajador.test@vitna.pe', 'TRABAJADOR', '$2a$12$KsdT.K0WTZEbzLrgjXQjVeZI60R6mwN/eugm.xD.L9pe1B0vsqxS2'),
('calidad.test@vitna.pe', 'CALIDAD', '$2a$12$qHJpKb.D2pX6nZK5kShJ8.S601qEAVUKLPJYTwnuY9nvBFCzfc3kS')
 ) AS credentials(email, role, password_hash)
 LOOP
  IF (SELECT count(*) FROM accounts WHERE lower(email)=target.email)>1 THEN
   RAISE EXCEPTION 'Duplicate test access email; migration aborted';
  END IF;
  IF EXISTS (SELECT 1 FROM accounts WHERE lower(email)=target.email AND role IS DISTINCT FROM target.role) THEN
   RAISE EXCEPTION 'Test access target has a different role; migration aborted';
  END IF;
 END LOOP;
 FOR target IN SELECT * FROM (VALUES
('admin@vitna.pe', 'ADMIN', '$2a$12$mplmguvkatrjz4aLzZzRjuvxvGtUMwPHndyDx3VBJo9poULv2/0E2'),
('cliente.test@vitna.pe', 'CLIENTE', '$2a$12$vEB.E7mOIlZE7LmGvy6wg.9dLronz9b.CXPOYcQNWSqKup0BnzMxa'),
('almacenero.test@vitna.pe', 'ALMACENERO', '$2a$12$5ybJUkINxVpaw8uYJtgOg.mqkuwocYiKtIN04DHWZpBJhldWhAmn.'),
('jefe_produccion.test@vitna.pe', 'JEFE_PRODUCCION', '$2a$12$ae7iLwDaLNJcu3Vqi9KeguM2ZuLouGKl5AQSwHctpKV.ysGuc2ubm'),
('trabajador.test@vitna.pe', 'TRABAJADOR', '$2a$12$KsdT.K0WTZEbzLrgjXQjVeZI60R6mwN/eugm.xD.L9pe1B0vsqxS2'),
('calidad.test@vitna.pe', 'CALIDAD', '$2a$12$qHJpKb.D2pX6nZK5kShJ8.S601qEAVUKLPJYTwnuY9nvBFCzfc3kS')
 ) AS credentials(email, role, password_hash)
 LOOP
  UPDATE accounts SET password_hash=target.password_hash, active=true WHERE lower(email)=target.email;
  INSERT INTO accounts(name,email,password_hash,role,active)
   SELECT 'Pruebas VITNA - ' || target.role,target.email,target.password_hash,target.role,true
   WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE lower(email)=target.email);
 END LOOP;
END
$bootstrap$;