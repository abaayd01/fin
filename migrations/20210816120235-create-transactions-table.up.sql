create table transactions
(
    id               int not null auto_increment primary key,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    description      varchar(255),
    amount           decimal(13, 2),
    transaction_date date,
    source           varchar(255),
    account_number   varchar(255)
);
