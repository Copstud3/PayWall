alter table payments
    add idempotency_key varchar(255) not null
        constraint idempotency_key
            unique;