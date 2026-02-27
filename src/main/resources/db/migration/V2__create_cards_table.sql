CREATE TABLE cards (
    id                    BIGSERIAL       PRIMARY KEY,
    encrypted_card_number VARCHAR(512)    NOT NULL,
    user_id               BIGINT          NOT NULL,
    expiry_date           DATE            NOT NULL,
    status                VARCHAR(50)     NOT NULL,
    balance               NUMERIC(19, 2)  NOT NULL DEFAULT 0.00,
    version               BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT fk_cards_user FOREIGN KEY (user_id) REFERENCES users(id)
);
