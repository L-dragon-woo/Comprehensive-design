CREATE DATABASE IF NOT EXISTS skinai
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'skinai'@'%' IDENTIFIED BY 'skinai_password';
CREATE USER IF NOT EXISTS 'skinai'@'localhost' IDENTIFIED BY 'skinai_password';
CREATE USER IF NOT EXISTS 'skinai'@'127.0.0.1' IDENTIFIED BY 'skinai_password';

ALTER USER 'skinai'@'%' IDENTIFIED BY 'skinai_password';
ALTER USER 'skinai'@'localhost' IDENTIFIED BY 'skinai_password';
ALTER USER 'skinai'@'127.0.0.1' IDENTIFIED BY 'skinai_password';

GRANT ALL PRIVILEGES ON skinai.* TO 'skinai'@'%';
GRANT ALL PRIVILEGES ON skinai.* TO 'skinai'@'localhost';
GRANT ALL PRIVILEGES ON skinai.* TO 'skinai'@'127.0.0.1';

FLUSH PRIVILEGES;
