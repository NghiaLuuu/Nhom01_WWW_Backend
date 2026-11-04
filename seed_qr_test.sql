DO $$
DECLARE
    r_id BIGINT;
    v1_id BIGINT;
    v2_id BIGINT;
    c_id UUID;
    s1_id BIGINT;
    s2_id BIGINT;
    s3_id BIGINT;
    s4_id BIGINT;
    trip_today_id BIGINT;
    trip_tomorrow_id BIGINT;
    b_valid_id BIGINT;
    b_used_id BIGINT;
    b_unpaid_id BIGINT;
    b_wrong_day_id BIGINT;
BEGIN
    SELECT id INTO r_id FROM routes LIMIT 1;
    SELECT id INTO v1_id FROM vehicles LIMIT 1;
    SELECT id INTO v2_id FROM vehicles OFFSET 1 LIMIT 1;
    SELECT id INTO c_id FROM customers WHERE email = 'khach1@gmail.com' LIMIT 1;
    IF c_id IS NULL THEN SELECT id INTO c_id FROM customers LIMIT 1; END IF;

    SELECT id INTO s1_id FROM seats WHERE vehicle_id = v1_id LIMIT 1;
    SELECT id INTO s2_id FROM seats WHERE vehicle_id = v1_id OFFSET 1 LIMIT 1;
    SELECT id INTO s3_id FROM seats WHERE vehicle_id = v1_id OFFSET 2 LIMIT 1;
    SELECT id INTO s4_id FROM seats WHERE vehicle_id = v2_id LIMIT 1;

    -- Today trip
    INSERT INTO trips (route_id, vehicle_id, departure_time, price, status)
    VALUES (r_id, v1_id, (CURRENT_DATE + TIME '23:59:00'), 250000, 'PENDING')
    RETURNING id INTO trip_today_id;

    -- Tomorrow trip
    INSERT INTO trips (route_id, vehicle_id, departure_time, price, status)
    VALUES (r_id, v2_id, (CURRENT_DATE + INTERVAL '1 day' + TIME '10:00:00'), 250000, 'PENDING')
    RETURNING id INTO trip_tomorrow_id;

    -- Bookings
    INSERT INTO bookings (customer_id, trip_id, status, total_price, created_at, updated_at)
    VALUES (c_id, trip_today_id, 'PAID', 250000, NOW(), NOW()) RETURNING id INTO b_valid_id;

    INSERT INTO bookings (customer_id, trip_id, status, total_price, created_at, updated_at)
    VALUES (c_id, trip_today_id, 'PAID', 250000, NOW(), NOW()) RETURNING id INTO b_used_id;

    INSERT INTO bookings (customer_id, trip_id, status, total_price, created_at, updated_at)
    VALUES (c_id, trip_today_id, 'PENDING_PAYMENT', 250000, NOW(), NOW()) RETURNING id INTO b_unpaid_id;

    INSERT INTO bookings (customer_id, trip_id, status, total_price, created_at, updated_at)
    VALUES (c_id, trip_tomorrow_id, 'PAID', 250000, NOW(), NOW()) RETURNING id INTO b_wrong_day_id;

    -- Payments
    INSERT INTO payments (booking_id, amount, payment_method, status, transaction_id, created_at)
    VALUES (b_valid_id, 250000, 'CASH', 'SUCCESS', 'SEED-MOBILE-VALID', NOW());

    INSERT INTO payments (booking_id, amount, payment_method, status, transaction_id, created_at)
    VALUES (b_used_id, 250000, 'CASH', 'SUCCESS', 'SEED-MOBILE-USED', NOW());

    INSERT INTO payments (booking_id, amount, payment_method, status, transaction_id, created_at)
    VALUES (b_wrong_day_id, 250000, 'CASH', 'SUCCESS', 'SEED-MOBILE-WRONG-DAY', NOW());

    -- Tickets
    INSERT INTO tickets (ticket_code, booking_id, seat_id, status, price, created_at, updated_at)
    VALUES ('QR-VALID-TODAY', b_valid_id, s1_id, 'ISSUED', 250000, NOW(), NOW());

    INSERT INTO tickets (ticket_code, booking_id, seat_id, status, price, created_at, updated_at, used_at)
    VALUES ('QR-ALREADY-USED', b_used_id, s2_id, 'USED', 250000, NOW(), NOW(), NOW() - INTERVAL '5 minutes');

    INSERT INTO tickets (ticket_code, booking_id, seat_id, status, price, created_at, updated_at)
    VALUES ('QR-UNPAID', b_unpaid_id, s3_id, 'ISSUED', 250000, NOW(), NOW());

    INSERT INTO tickets (ticket_code, booking_id, seat_id, status, price, created_at, updated_at)
    VALUES ('QR-WRONG-DAY', b_wrong_day_id, s4_id, 'ISSUED', 250000, NOW(), NOW());
END $$;
