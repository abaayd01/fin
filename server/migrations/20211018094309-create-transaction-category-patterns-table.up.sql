create table if not exists transaction_category_patterns
(
    id          int not null auto_increment primary key,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    pattern     varchar(255),
    category_id int not null,
    CONSTRAINT fk_category_id
        FOREIGN KEY (category_id)
            REFERENCES categories (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);