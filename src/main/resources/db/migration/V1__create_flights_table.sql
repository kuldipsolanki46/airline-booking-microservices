CREATE TABLE flights (
                         id BIGSERIAL PRIMARY KEY,

                         version BIGINT,

                         flight_number VARCHAR(20) NOT NULL,

                         source VARCHAR(100) NOT NULL,

                         destination VARCHAR(100) NOT NULL,

                         departure TIMESTAMP NOT NULL,

                         arrival TIMESTAMP NOT NULL,

                         total_seats INTEGER NOT NULL,

                         available_seats INTEGER NOT NULL,

                         price NUMERIC(10, 2) NOT NULL,

                         created_at TIMESTAMP NOT NULL,

                         updated_at TIMESTAMP NOT NULL
);