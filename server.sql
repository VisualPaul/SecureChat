CREATE USER 'chat'@'localhost' IDENTIFIED BY 'YwtMLkrzSPy3';
CREATE DATABASE chat;
GRANT ALL ON chat TO 'chat'@'localhost';
USE chat;
CREATE TABLE users
(
    id INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    name VARCHAR(40) NOT NULL,
    public_key BLOB NOT NULL
);
CREATE UNIQUE INDEX users_name_uindex ON users (name);
