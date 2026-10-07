CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(254) PRIMARY KEY,
    password VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS authorities (
    username VARCHAR(254) NOT NULL REFERENCES users (username) ON DELETE CASCADE,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT authorities_username_authority_unique UNIQUE (username, authority)
);
