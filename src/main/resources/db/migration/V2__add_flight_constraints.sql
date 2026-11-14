
ALTER TABLE flights
    ADD CONSTRAINT chk_total_seats
        CHECK (total_seats > 0);

ALTER TABLE flights
    ADD CONSTRAINT chk_available_seats_non_negative
        CHECK (available_seats >= 0);

ALTER TABLE flights
    ADD CONSTRAINT chk_available_seats_limit
        CHECK (available_seats <= total_seats);

ALTER TABLE flights
    ADD CONSTRAINT chk_flight_price
        CHECK (price > 0);

ALTER TABLE flights
    ADD CONSTRAINT chk_flight_arrival_after_departure
        CHECK (arrival > departure);
