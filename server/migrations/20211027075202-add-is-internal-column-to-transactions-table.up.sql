ALTER TABLE transactions
    ADD is_internal bool not null default false;