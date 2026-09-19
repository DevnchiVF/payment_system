CREATE TABLE clients (
                         id           BIGSERIAL PRIMARY KEY,
                         dni          VARCHAR(255) NOT NULL UNIQUE,
                         name         VARCHAR(255) NOT NULL,
                         birth_date   DATE         NOT NULL,
                         phone_number VARCHAR(255) NOT NULL UNIQUE,
                         email        VARCHAR(255) NOT NULL UNIQUE,
                         password     VARCHAR(255) NOT NULL
);

CREATE TABLE accounts (
                          id           BIGSERIAL PRIMARY KEY,
                          client_id    BIGINT       NOT NULL UNIQUE REFERENCES clients(id),
                          phone_number VARCHAR(255) NOT NULL UNIQUE,
                          balance      NUMERIC(19,2) NOT NULL
);

CREATE TABLE transactions (
                              id                BIGSERIAL PRIMARY KEY,
                              source_account_id BIGINT        REFERENCES accounts(id),
                              target_account_id BIGINT        REFERENCES accounts(id),
                              amount            NUMERIC(19,2) NOT NULL,
                              type              VARCHAR(50)   NOT NULL,
                              status            VARCHAR(50)   NOT NULL,
                              idempotency_key   VARCHAR(255)  UNIQUE,
                              timestamp         TIMESTAMP
);