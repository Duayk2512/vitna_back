-- Solo lectura. No devuelve contrasenas ni hashes.
SELECT
  EXISTS (SELECT 1 FROM accounts WHERE lower(email) = 'admin@vitna.pe') AS admin_exists,
  (SELECT count(*) FROM accounts) AS account_count;

SELECT
  email,
  role,
  active,
  CASE
    WHEN password_hash IS NULL OR password_hash = '' THEN 'MISSING'
    WHEN password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$' THEN 'BCRYPT_FORMAT'
    ELSE 'UNEXPECTED_FORMAT'
  END AS password_format,
  email = btrim(email) AS email_without_outer_spaces
FROM accounts
WHERE lower(btrim(email)) = 'admin@vitna.pe';
