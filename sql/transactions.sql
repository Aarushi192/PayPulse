CREATE DATABASE IF NOT EXISTS paypulse;
USE paypulse;

CREATE TABLE IF NOT EXISTS transactions (
                                            id            INT           PRIMARY KEY,
                                            sender_upi    VARCHAR(50)   NOT NULL,
    receiver_upi  VARCHAR(50)   NOT NULL,
    amount        DECIMAL(12,2) NOT NULL,
    fee           DECIMAL(12,2) NOT NULL DEFAULT 0,
    note          VARCHAR(255)  NOT NULL DEFAULT '',
    txn_type      VARCHAR(10)   NOT NULL,
    status        VARCHAR(10)   NOT NULL,
    created_at    DATETIME      NOT NULL,
    FOREIGN KEY (sender_upi)   REFERENCES users(upi_id),
    FOREIGN KEY (receiver_upi) REFERENCES users(upi_id)
    );