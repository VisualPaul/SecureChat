CREATE TABLE login
(
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    public_key BLOB NOT NULL,
    private_key BLOB NOT NULL
);
CREATE UNIQUE INDEX login_name_uindex ON login (name);
CREATE TABLE users
(
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    public_key BLOB NOT NULL
);
CREATE UNIQUE INDEX users_name_uindex ON users (name);
