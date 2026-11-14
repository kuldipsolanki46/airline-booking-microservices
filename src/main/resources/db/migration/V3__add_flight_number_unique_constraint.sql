ALTER TABLE flights
    ADD CONSTRAINT uk_flight_number
        UNIQUE (flight_number);