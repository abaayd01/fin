create table if not exists transactions_categories
(
    transaction_id INT,
    category_id    INT,
    PRIMARY KEY (transaction_id, category_id),
    CONSTRAINT fk_transaction
        FOREIGN KEY (transaction_id)
            REFERENCES transactions (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    CONSTRAINT fk_category
        FOREIGN KEY (category_id)
            REFERENCES categories (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);
