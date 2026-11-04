# -*- coding: utf-8 -*-
"""
Script to generate large, realistic, production-like data for VEXE Bus Booking System.
Directly generates SQL and executes it into postgres_db_local.
"""
import uuid
import random

def main():
    sql_lines = []
    sql_lines.append("-- === AUTO GENERATED MASSIVE DATASET FOR VEXE BUS BOOKING SYSTEM ===")
    sql_lines.append("BEGIN;")

    # 1. NEW ROUTES (36 routes connecting all regions of Vietnam)
    routes_data = [
        ("TP.HCM", "Phan Thiết", 200, 240, 200000),
        ("Phan Thiết", "TP.HCM", 200, 240, 200000),
        ("TP.HCM", "Buôn Ma Thuột", 350, 480, 320000),
        ("Buôn Ma Thuột", "TP.HCM", 350, 480, 320000),
        ("TP.HCM", "Rạch Giá", 250, 300, 220000),
        ("Rạch Giá", "TP.HCM", 250, 300, 220000),
        ("TP.HCM", "Cà Mau", 310, 390, 260000),
        ("Cà Mau", "TP.HCM", 310, 390, 260000),
        ("TP.HCM", "Châu Đốc", 240, 300, 210000),
        ("Châu Đốc", "TP.HCM", 240, 300, 210000),
        ("TP.HCM", "Quy Nhơn", 650, 720, 420000),
        ("Quy Nhơn", "TP.HCM", 650, 720, 420000),
        ("TP.HCM", "Pleiku", 500, 600, 380000),
        ("Pleiku", "TP.HCM", 500, 600, 380000),
        ("Hà Nội", "Ninh Bình", 95, 90, 120000),
        ("Ninh Bình", "Hà Nội", 95, 90, 120000),
        ("Hà Nội", "Thanh Hóa", 160, 150, 160000),
        ("Thanh Hóa", "Hà Nội", 160, 150, 160000),
        ("Hà Nội", "Vinh", 300, 300, 250000),
        ("Vinh", "Hà Nội", 300, 300, 250000),
        ("Hà Nội", "Hà Giang", 300, 360, 300000),
        ("Hà Giang", "Hà Nội", 300, 360, 300000),
        ("Hà Nội", "Mộc Châu", 200, 240, 220000),
        ("Mộc Châu", "Hà Nội", 200, 240, 220000),
        ("Hà Nội", "Đà Nẵng", 760, 840, 550000),
        ("Đà Nẵng", "Hà Nội", 760, 840, 550000),
        ("Đà Nẵng", "Hội An", 35, 45, 60000),
        ("Hội An", "Đà Nẵng", 35, 45, 60000),
        ("Đà Nẵng", "Nha Trang", 530, 600, 400000),
        ("Nha Trang", "Đà Nẵng", 530, 600, 400000),
        ("Đà Nẵng", "Đà Lạt", 650, 750, 450000),
        ("Đà Lạt", "Đà Nẵng", 650, 750, 450000),
        ("Cần Thơ", "Rạch Giá", 110, 150, 130000),
        ("Cần Thơ", "Cà Mau", 180, 240, 180000),
        ("Cần Thơ", "Đà Lạt", 450, 540, 380000),
        ("Đà Lạt", "Cần Thơ", 450, 540, 380000)
    ]

    for dep, arr, dist, dur, price in routes_data:
        sql_lines.append(f"""
            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT '{dep}', '{arr}', {dist}, {dur}, {price}, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = '{dep}' AND arrival_location = '{arr}'
            );
        """)

    # 2. NEW VEHICLES AND SEATS
    vehicle_types = [
        ("Limousine VIP 22 phòng", 22, "Limousine"),
        ("Giường nằm Luxury 34 chỗ", 34, "Giường nằm"),
        ("Ghế ngồi Universe 45 chỗ", 45, "Ghế ngồi"),
        ("Limousine Dcar 9 chỗ", 9, "Limousine")
    ]

    plate_prefixes = [50, 51, 52, 59, 29, 30, 43, 49, 65, 79, 77, 81]
    
    for i in range(1, 41):
        v_label, cap, v_type = vehicle_types[i % len(vehicle_types)]
        pref = plate_prefixes[i % len(plate_prefixes)]
        plate = f"{pref}B-{200+i:03d}.{i:02d}"
        sql_lines.append(f"""
            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '{plate}', '{v_type}', {cap}, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '{plate}');
        """)

    # Populate seats for all vehicles that don't have seats yet
    sql_lines.append("""
        DO $$
        DECLARE
            v_rec RECORD;
            s_name TEXT;
            s_floor INT;
            i INT;
        BEGIN
            FOR v_rec IN SELECT id, capacity, type FROM vehicles LOOP
                IF NOT EXISTS (SELECT 1 FROM seats WHERE vehicle_id = v_rec.id) THEN
                    FOR i IN 1..v_rec.capacity LOOP
                        s_floor := CASE WHEN i <= (v_rec.capacity / 2) THEN 1 ELSE 2 END;
                        IF v_rec.capacity = 9 THEN
                            s_name := 'VIP' || i;
                            s_floor := 1;
                        ELSE
                            s_name := CASE WHEN i <= (v_rec.capacity / 2) 
                                           THEN 'A' || i 
                                           ELSE 'B' || (i - (v_rec.capacity / 2)) 
                                      END;
                        END IF;
                        INSERT INTO seats (vehicle_id, seat_name, floor) 
                        VALUES (v_rec.id, s_name, s_floor);
                    END LOOP;
                END IF;
            END LOOP;
        END $$;
    """)

    # 3. NEW CUSTOMERS
    first_names = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý"]
    middle_names = ["Văn", "Thị", "Quốc", "Đức", "Hoàng", "Minh", "Thanh", "Ngọc", "Thảo", "Hải", "Tuấn", "Xuân", "Hồng", "Gia", "Đình"]
    last_names = ["An", "Bình", "Châu", "Dũng", "Em", "Giang", "Huy", "Khoa", "Linh", "Mai", "Nam", "Phúc", "Quân", "Sơn", "Tâm", "Uyên", "Vinh", "Yến", "Trinh", "Kiệt", "Trí", "Thịnh", "Thắng", "Vy", "Hân"]
    
    cities = ["TP.HCM", "Hà Nội", "Đà Nẵng", "Cần Thơ", "Nha Trang", "Đà Lạt", "Vũng Tàu", "Hải Phòng", "Huế", "Quy Nhơn", "Buôn Ma Thuột"]
    
    default_pwd_hash = "$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu" # password123 / 123456
    
    random.seed(42)
    
    for i in range(101, 221): # 120 more customers
        fn = f"{random.choice(first_names)} {random.choice(middle_names)} {random.choice(last_names)}"
        email = f"user{i}_{random.choice(last_names).lower()}@gmail.com"
        phone = f"09{random.randint(10000000, 99999999)}"
        c_id = str(uuid.uuid4())
        dob = f"{random.randint(1980, 2004)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}"
        addr = f"Quận {random.randint(1, 12)}, {random.choice(cities)}"
        
        sql_lines.append(f"""
            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '{c_id}', '{fn}', '{email}', '{phone}', '{default_pwd_hash}', 'ACTIVE', '{dob}', '{addr}', NOW() - interval '{random.randint(1, 90)} days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = '{email}' OR phone_number = '{phone}');
        """)

    # 4. STAFF USERS (drivers, checkers, support)
    staff_extra = [
        ("Trần Soát Vé 1", "soatve1@vexe.vn", "0908111001", "STAFF_SV1", 2),
        ("Lê Soát Vé 2", "soatve2@vexe.vn", "0908111002", "STAFF_SV2", 2),
        ("Phạm Bác Tài 1", "taixe1@vexe.vn", "0908111003", "STAFF_TX1", 2),
        ("Ngô Bác Tài 2", "taixe2@vexe.vn", "0908111004", "STAFF_TX2", 2),
        ("Vũ Chăm Sóc Khách Hàng", "cskh@vexe.vn", "0908111005", "STAFF_CSKH", 2),
    ]
    for s_fn, s_email, s_phone, s_code, r_id in staff_extra:
        s_id = str(uuid.uuid4())
        sql_lines.append(f"""
            INSERT INTO staffs (id, full_name, email, phone_number, password, status, staff_code, role_id, created_at, updated_at, version)
            SELECT '{s_id}', '{s_fn}', '{s_email}', '{s_phone}', '{default_pwd_hash}', 'ACTIVE', '{s_code}', {r_id}, NOW() - interval '60 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM staffs WHERE email = '{s_email}' OR staff_code = '{s_code}');
        """)

    # 5. GENERATE A LARGE NUMBER OF TRIPS (PAST, TODAY, AND FUTURE)
    sql_lines.append("""
        DO $$
        DECLARE
            r_rec RECORD;
            v_rec RECORD;
            day_offset INT;
            dept_hour INT;
            dept_min INT;
            t_dept TIMESTAMP;
            t_price NUMERIC;
            t_status VARCHAR(255);
            new_trip_id BIGINT;
            
            c_rec RECORD;
            b_status VARCHAR(255);
            b_total NUMERIC;
            new_booking_id BIGINT;
            
            s_rec RECORD;
            t_code VARCHAR(64);
            t_ticket_status VARCHAR(255);
            seat_count INT;
            target_seat_count INT;
            p_method VARCHAR(255);
            rnd_check INT;
            s_staff_id UUID;
        BEGIN
            -- Get a staff id for check-in
            SELECT id INTO s_staff_id FROM staffs WHERE staff_code = 'STAFF_MOBILE' LIMIT 1;
            IF s_staff_id IS NULL THEN
                SELECT id INTO s_staff_id FROM staffs LIMIT 1;
            END IF;

            -- Loop over all routes
            FOR r_rec IN SELECT * FROM routes LOOP
                -- Schedule trips across -12 days to +20 days
                FOR day_offset IN -12..20 LOOP
                    -- For each day, schedule 3 trips at: 07:00, 14:00, 21:00
                    FOR dept_hour IN 7..21 BY 7 LOOP
                        dept_min := abs((day_offset * 17 + dept_hour * 11)) % 60;
                        
                        t_dept := (CURRENT_DATE + (day_offset || ' days')::INTERVAL) + (dept_hour || ' hours ' || dept_min || ' minutes')::INTERVAL;
                        
                        -- Pick a vehicle randomly using table column
                        SELECT * INTO v_rec FROM vehicles 
                        ORDER BY md5(vehicles.id::text || t_dept::text) 
                        LIMIT 1;
                        
                        IF v_rec.id IS NOT NULL THEN
                            -- Calculate realistic trip price
                            IF v_rec.type ILIKE '%Limousine%' THEN
                                t_price := r_rec.base_price * 1.45;
                            ELSIF v_rec.type ILIKE '%Giường%' THEN
                                t_price := r_rec.base_price * 1.15;
                            ELSE
                                t_price := r_rec.base_price;
                            END IF;
                            
                            -- Status
                            IF t_dept < NOW() THEN
                                t_status := 'COMPLETED';
                            ELSE
                                t_status := 'PENDING';
                            END IF;

                            -- Insert trip
                            INSERT INTO trips (route_id, vehicle_id, departure_time, price, status)
                            VALUES (r_rec.id, v_rec.id, t_dept, t_price, t_status)
                            RETURNING id INTO new_trip_id;

                            -- Create 1 to 4 bookings for this trip
                            FOR c_rec IN (
                                SELECT customers.id, customers.full_name FROM customers 
                                ORDER BY md5(customers.id::text || new_trip_id::text) 
                                LIMIT (1 + (new_trip_id % 4))
                            ) LOOP
                                rnd_check := (new_trip_id + ascii(substr(c_rec.full_name, 1, 1))) % 100;
                                
                                IF t_dept < NOW() THEN
                                    -- Past trips: 88% PAID, 12% CANCELLED
                                    IF rnd_check < 88 THEN
                                        b_status := 'PAID';
                                    ELSE
                                        b_status := 'CANCELLED';
                                    END IF;
                                ELSE
                                    -- Future trips: 70% PAID, 22% PENDING_PAYMENT, 8% CANCELLED
                                    IF rnd_check < 70 THEN
                                        b_status := 'PAID';
                                    ELSIF rnd_check < 92 THEN
                                        b_status := 'PENDING_PAYMENT';
                                    ELSE
                                        b_status := 'CANCELLED';
                                    END IF;
                                END IF;

                                target_seat_count := 1 + (rnd_check % 3); -- 1 to 3 seats

                                -- Check available seats on this trip
                                CREATE TEMPORARY TABLE IF NOT EXISTS temp_booked_seats (seat_id BIGINT);
                                
                                -- Find available seats that haven't been booked on this trip yet
                                b_total := t_price * target_seat_count;

                                INSERT INTO bookings (customer_id, trip_id, status, total_price, created_at, updated_at)
                                VALUES (c_rec.id, new_trip_id, b_status, b_total, 
                                        t_dept - ((1 + (rnd_check % 5)) || ' days')::INTERVAL, 
                                        NOW())
                                RETURNING id INTO new_booking_id;

                                -- Add Payment record if PAID
                                IF b_status = 'PAID' THEN
                                    p_method := CASE (rnd_check % 4)
                                                    WHEN 0 THEN 'VNPAY'
                                                    WHEN 1 THEN 'MOMO'
                                                    WHEN 2 THEN 'ZALOPAY'
                                                    ELSE 'CASH'
                                                END;
                                    INSERT INTO payments (booking_id, amount, payment_method, status, transaction_id, created_at)
                                    VALUES (new_booking_id, b_total, p_method, 'SUCCESS', 'TXN-' || gen_random_uuid(), t_dept - INTERVAL '1 day');
                                END IF;

                                -- Add Cancel Request if CANCELLED
                                IF b_status = 'CANCELLED' AND (rnd_check % 2 = 0) THEN
                                    INSERT INTO cancel_requests (booking_id, status, reject_reason, created_at, resolved_at, resolved_by)
                                    VALUES (new_booking_id, 'APPROVED', NULL, t_dept - INTERVAL '2 days', t_dept - INTERVAL '1 day', s_staff_id);
                                END IF;

                                -- Create Tickets for seats
                                seat_count := 0;
                                FOR s_rec IN (
                                    SELECT s.id, s.seat_name 
                                    FROM seats s
                                    WHERE s.vehicle_id = v_rec.id 
                                      AND NOT EXISTS (
                                          SELECT 1 
                                          FROM tickets tk 
                                          JOIN bookings bk ON tk.booking_id = bk.id 
                                          WHERE bk.trip_id = new_trip_id 
                                            AND tk.seat_id = s.id 
                                            AND tk.status != 'CANCELLED'
                                      )
                                    ORDER BY md5(s.id::text || new_booking_id::text) 
                                    LIMIT target_seat_count
                                ) LOOP
                                    seat_count := seat_count + 1;
                                    t_code := 'VX-' || new_booking_id || '-' || s_rec.id || '-' || to_char(t_dept, 'YYMMDD');
                                    
                                    IF b_status = 'PAID' THEN
                                        IF t_dept < NOW() THEN
                                            t_ticket_status := 'USED';
                                        ELSE
                                            t_ticket_status := 'ISSUED';
                                        END IF;
                                    ELSIF b_status = 'PENDING_PAYMENT' THEN
                                        t_ticket_status := 'RESERVED';
                                    ELSE
                                        t_ticket_status := 'CANCELLED';
                                    END IF;

                                    INSERT INTO tickets (booking_id, seat_id, ticket_code, price, status, created_at, updated_at, used_at, checked_by)
                                    VALUES (
                                        new_booking_id, 
                                        s_rec.id, 
                                        t_code, 
                                        t_price, 
                                        t_ticket_status, 
                                        t_dept - INTERVAL '3 days', 
                                        NOW(), 
                                        CASE WHEN t_ticket_status = 'USED' THEN t_dept + INTERVAL '10 minutes' ELSE NULL END,
                                        CASE WHEN t_ticket_status = 'USED' THEN s_staff_id ELSE NULL END
                                    );
                                END LOOP;

                            END LOOP; -- End customer booking loop
                        END IF;
                    END LOOP; -- End hour loop
                END LOOP; -- End day offset loop
            END LOOP; -- End route loop
        END $$;
    """)

    # 6. USER SESSIONS & TRACKING BEHAVIOR
    sql_lines.append("""
        DO $$
        DECLARE
            i INT;
            sess_id UUID;
            pv_id UUID;
            c_id UUID;
            anon_id VARCHAR(255);
            s_time TIMESTAMP;
            e_time TIMESTAMP;
            src VARCHAR(100);
            camp VARCHAR(255);
            u_agent VARCHAR(1000);
            ip VARCHAR(255);
        BEGIN
            FOR i IN 1..120 LOOP
                sess_id := gen_random_uuid();
                anon_id := 'anon-' || md5(i::text || sess_id::text);
                s_time := NOW() - ((i % 30) || ' days ' || (i % 24) || ' hours')::INTERVAL;
                e_time := s_time + ((120 + (i * 23) % 900) || ' seconds')::INTERVAL;
                
                src := CASE (i % 5)
                            WHEN 0 THEN 'google'
                            WHEN 1 THEN 'facebook'
                            WHEN 2 THEN 'zalo'
                            WHEN 3 THEN 'tiktok'
                            ELSE 'direct'
                       END;
                camp := CASE (i % 4)
                            WHEN 0 THEN 've-xe-tet-2026'
                            WHEN 1 THEN 'limousine-vip-sale'
                            WHEN 2 THEN 'du-lich-da-lat-nha-trang'
                            ELSE NULL
                        END;
                u_agent := 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36';
                ip := '113.161.' || (10 + (i % 200)) || '.' || (20 + (i % 220));

                SELECT customers.id INTO c_id FROM customers ORDER BY md5(customers.id::text || i::text) LIMIT 1;
                IF (i % 3 = 0) THEN c_id := NULL; END IF; -- Some sessions are guest

                INSERT INTO user_sessions (id, anonymous_id, customer_id, start_time, end_time, total_duration_seconds, traffic_source, utm_campaign, user_agent, ip_address, created_at)
                VALUES (sess_id, anon_id, c_id, s_time, e_time, (120 + (i * 23) % 900), src, camp, u_agent, ip, s_time);

                -- Page view 1: Trang chủ
                pv_id := gen_random_uuid();
                INSERT INTO page_views (id, session_id, page_url, page_title, entered_at, left_at, active_duration_seconds, idle_duration_seconds, created_at)
                VALUES (pv_id, sess_id, 'http://localhost:5173/', 'VEXE - Hệ thống Đặt Vé Xe Khách Trực Tuyến Toàn Quốc', s_time, s_time + INTERVAL '45 seconds', 40, 5, s_time);

                INSERT INTO user_actions (id, session_id, page_view_id, action_type, payload, created_at)
                VALUES (gen_random_uuid(), sess_id, pv_id, 'SEARCH_TRIP', '{"from":"TP.HCM","to":"Đà Lạt","date":"2026-11-05"}', s_time + INTERVAL '20 seconds');

                -- Page view 2: Tìm chuyến
                pv_id := gen_random_uuid();
                INSERT INTO page_views (id, session_id, page_url, page_title, entered_at, left_at, active_duration_seconds, idle_duration_seconds, created_at)
                VALUES (pv_id, sess_id, 'http://localhost:5173/trips?from=TP.HCM&to=Đà+Lạt', 'Kết quả tìm kiếm vé xe: TP.HCM đi Đà Lạt', s_time + INTERVAL '45 seconds', s_time + INTERVAL '120 seconds', 65, 10, s_time + INTERVAL '45 seconds');

                INSERT INTO user_actions (id, session_id, page_view_id, action_type, payload, created_at)
                VALUES (gen_random_uuid(), sess_id, pv_id, 'SELECT_SEAT', '{"seats":["A1","A2"],"price":500000}', s_time + INTERVAL '90 seconds');
            END LOOP;
        END $$;
    """)

    # 7. RESYNCHRONIZE ALL SEQUENCES
    sql_lines.append("""
        SELECT setval('routes_id_seq', COALESCE((SELECT max(id) FROM routes), 1));
        SELECT setval('vehicles_id_seq', COALESCE((SELECT max(id) FROM vehicles), 1));
        SELECT setval('seats_id_seq', COALESCE((SELECT max(id) FROM seats), 1));
        SELECT setval('trips_id_seq', COALESCE((SELECT max(id) FROM trips), 1));
        SELECT setval('bookings_id_seq', COALESCE((SELECT max(id) FROM bookings), 1));
        SELECT setval('tickets_id_seq', COALESCE((SELECT max(id) FROM tickets), 1));
        SELECT setval('payments_id_seq', COALESCE((SELECT max(id) FROM payments), 1));
        SELECT setval('cancel_requests_id_seq', COALESCE((SELECT max(id) FROM cancel_requests), 1));
        SELECT setval('table_metadata_id_seq', COALESCE((SELECT max(id) FROM table_metadata), 1));
    """)

    sql_lines.append("COMMIT;")

    out_path = "D:\\WWW\\project\\Nhom01_WWW_Backend\\seed_large_data.sql"
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(sql_lines))
    print(f"Generated SQL file at {out_path} with {len(sql_lines)} lines.")

if __name__ == "__main__":
    main()
