CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE accounts (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
                          version BIGINT NOT NULL DEFAULT 0,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          CONSTRAINT fk_accounts_user
                              FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE transfers (
                           id BIGSERIAL PRIMARY KEY,
                           from_account_id BIGINT NOT NULL,
                           to_account_id BIGINT NOT NULL,
                           amount NUMERIC(19, 2) NOT NULL,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);