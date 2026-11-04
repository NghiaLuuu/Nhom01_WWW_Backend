-- === AUTO GENERATED MASSIVE DATASET FOR VEXE BUS BOOKING SYSTEM ===
BEGIN;

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'TP.HCM', 'Phan Thiết', 200, 240, 200000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'TP.HCM' AND arrival_location = 'Phan Thiết'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Phan Thiết', 'TP.HCM', 200, 240, 200000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Phan Thiết' AND arrival_location = 'TP.HCM'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'TP.HCM', 'Buôn Ma Thuột', 350, 480, 320000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'TP.HCM' AND arrival_location = 'Buôn Ma Thuột'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Buôn Ma Thuột', 'TP.HCM', 350, 480, 320000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Buôn Ma Thuột' AND arrival_location = 'TP.HCM'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'TP.HCM', 'Rạch Giá', 250, 300, 220000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'TP.HCM' AND arrival_location = 'Rạch Giá'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Rạch Giá', 'TP.HCM', 250, 300, 220000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Rạch Giá' AND arrival_location = 'TP.HCM'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'TP.HCM', 'Cà Mau', 310, 390, 260000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'TP.HCM' AND arrival_location = 'Cà Mau'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Cà Mau', 'TP.HCM', 310, 390, 260000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Cà Mau' AND arrival_location = 'TP.HCM'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'TP.HCM', 'Châu Đốc', 240, 300, 210000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'TP.HCM' AND arrival_location = 'Châu Đốc'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Châu Đốc', 'TP.HCM', 240, 300, 210000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Châu Đốc' AND arrival_location = 'TP.HCM'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'TP.HCM', 'Quy Nhơn', 650, 720, 420000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'TP.HCM' AND arrival_location = 'Quy Nhơn'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Quy Nhơn', 'TP.HCM', 650, 720, 420000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Quy Nhơn' AND arrival_location = 'TP.HCM'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'TP.HCM', 'Pleiku', 500, 600, 380000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'TP.HCM' AND arrival_location = 'Pleiku'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Pleiku', 'TP.HCM', 500, 600, 380000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Pleiku' AND arrival_location = 'TP.HCM'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Hà Nội', 'Ninh Bình', 95, 90, 120000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Hà Nội' AND arrival_location = 'Ninh Bình'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Ninh Bình', 'Hà Nội', 95, 90, 120000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Ninh Bình' AND arrival_location = 'Hà Nội'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Hà Nội', 'Thanh Hóa', 160, 150, 160000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Hà Nội' AND arrival_location = 'Thanh Hóa'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Thanh Hóa', 'Hà Nội', 160, 150, 160000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Thanh Hóa' AND arrival_location = 'Hà Nội'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Hà Nội', 'Vinh', 300, 300, 250000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Hà Nội' AND arrival_location = 'Vinh'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Vinh', 'Hà Nội', 300, 300, 250000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Vinh' AND arrival_location = 'Hà Nội'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Hà Nội', 'Hà Giang', 300, 360, 300000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Hà Nội' AND arrival_location = 'Hà Giang'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Hà Giang', 'Hà Nội', 300, 360, 300000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Hà Giang' AND arrival_location = 'Hà Nội'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Hà Nội', 'Mộc Châu', 200, 240, 220000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Hà Nội' AND arrival_location = 'Mộc Châu'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Mộc Châu', 'Hà Nội', 200, 240, 220000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Mộc Châu' AND arrival_location = 'Hà Nội'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Hà Nội', 'Đà Nẵng', 760, 840, 550000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Hà Nội' AND arrival_location = 'Đà Nẵng'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Đà Nẵng', 'Hà Nội', 760, 840, 550000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Đà Nẵng' AND arrival_location = 'Hà Nội'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Đà Nẵng', 'Hội An', 35, 45, 60000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Đà Nẵng' AND arrival_location = 'Hội An'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Hội An', 'Đà Nẵng', 35, 45, 60000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Hội An' AND arrival_location = 'Đà Nẵng'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Đà Nẵng', 'Nha Trang', 530, 600, 400000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Đà Nẵng' AND arrival_location = 'Nha Trang'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Nha Trang', 'Đà Nẵng', 530, 600, 400000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Nha Trang' AND arrival_location = 'Đà Nẵng'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Đà Nẵng', 'Đà Lạt', 650, 750, 450000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Đà Nẵng' AND arrival_location = 'Đà Lạt'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Đà Lạt', 'Đà Nẵng', 650, 750, 450000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Đà Lạt' AND arrival_location = 'Đà Nẵng'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Cần Thơ', 'Rạch Giá', 110, 150, 130000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Cần Thơ' AND arrival_location = 'Rạch Giá'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Cần Thơ', 'Cà Mau', 180, 240, 180000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Cần Thơ' AND arrival_location = 'Cà Mau'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Cần Thơ', 'Đà Lạt', 450, 540, 380000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Cần Thơ' AND arrival_location = 'Đà Lạt'
            );
        

            INSERT INTO routes (departure_location, arrival_location, distance_km, duration_minutes, base_price, status)
            SELECT 'Đà Lạt', 'Cần Thơ', 450, 540, 380000, true
            WHERE NOT EXISTS (
                SELECT 1 FROM routes WHERE departure_location = 'Đà Lạt' AND arrival_location = 'Cần Thơ'
            );
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '51B-201.01', 'Giường nằm', 34, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '51B-201.01');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '52B-202.02', 'Ghế ngồi', 45, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '52B-202.02');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '59B-203.03', 'Limousine', 9, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '59B-203.03');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '29B-204.04', 'Limousine', 22, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '29B-204.04');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '30B-205.05', 'Giường nằm', 34, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '30B-205.05');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '43B-206.06', 'Ghế ngồi', 45, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '43B-206.06');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '49B-207.07', 'Limousine', 9, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '49B-207.07');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '65B-208.08', 'Limousine', 22, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '65B-208.08');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '79B-209.09', 'Giường nằm', 34, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '79B-209.09');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '77B-210.10', 'Ghế ngồi', 45, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '77B-210.10');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '81B-211.11', 'Limousine', 9, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '81B-211.11');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '50B-212.12', 'Limousine', 22, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '50B-212.12');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '51B-213.13', 'Giường nằm', 34, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '51B-213.13');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '52B-214.14', 'Ghế ngồi', 45, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '52B-214.14');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '59B-215.15', 'Limousine', 9, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '59B-215.15');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '29B-216.16', 'Limousine', 22, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '29B-216.16');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '30B-217.17', 'Giường nằm', 34, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '30B-217.17');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '43B-218.18', 'Ghế ngồi', 45, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '43B-218.18');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '49B-219.19', 'Limousine', 9, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '49B-219.19');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '65B-220.20', 'Limousine', 22, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '65B-220.20');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '79B-221.21', 'Giường nằm', 34, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '79B-221.21');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '77B-222.22', 'Ghế ngồi', 45, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '77B-222.22');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '81B-223.23', 'Limousine', 9, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '81B-223.23');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '50B-224.24', 'Limousine', 22, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '50B-224.24');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '51B-225.25', 'Giường nằm', 34, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '51B-225.25');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '52B-226.26', 'Ghế ngồi', 45, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '52B-226.26');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '59B-227.27', 'Limousine', 9, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '59B-227.27');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '29B-228.28', 'Limousine', 22, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '29B-228.28');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '30B-229.29', 'Giường nằm', 34, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '30B-229.29');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '43B-230.30', 'Ghế ngồi', 45, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '43B-230.30');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '49B-231.31', 'Limousine', 9, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '49B-231.31');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '65B-232.32', 'Limousine', 22, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '65B-232.32');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '79B-233.33', 'Giường nằm', 34, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '79B-233.33');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '77B-234.34', 'Ghế ngồi', 45, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '77B-234.34');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '81B-235.35', 'Limousine', 9, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '81B-235.35');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '50B-236.36', 'Limousine', 22, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '50B-236.36');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '51B-237.37', 'Giường nằm', 34, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '51B-237.37');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '52B-238.38', 'Ghế ngồi', 45, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '52B-238.38');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '59B-239.39', 'Limousine', 9, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '59B-239.39');
        

            INSERT INTO vehicles (license_plate, type, capacity, status)
            SELECT '29B-240.40', 'Limousine', 22, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE license_plate = '29B-240.40');
        

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
    

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '3f5e096e-b71d-4c9d-a225-84a273799dda', 'Phạm Văn Vy', 'user101_linh@gmail.com', '0942868828', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1987-03-24', 'Quận 2, Buôn Ma Thuột', NOW() - interval '70 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user101_linh@gmail.com' OR phone_number = '0942868828');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'fa045eb5-dcd1-4210-8af8-bd1d3676ef1a', 'Lê Hải Sơn', 'user102_bình@gmail.com', '0913999315', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1982-04-08', 'Quận 9, Quy Nhơn', NOW() - interval '4 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user102_bình@gmail.com' OR phone_number = '0913999315');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'd9f3f4c0-1525-4737-9544-0713d800185d', 'Phan Xuân Trí', 'user103_thắng@gmail.com', '0983140807', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1993-04-15', 'Quận 10, Nha Trang', NOW() - interval '1 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user103_thắng@gmail.com' OR phone_number = '0983140807');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '8ba15b73-b873-4aea-8310-aaa077f9daaf', 'Huỳnh Xuân Sơn', 'user104_nam@gmail.com', '0947295260', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1984-04-25', 'Quận 6, Hà Nội', NOW() - interval '12 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user104_nam@gmail.com' OR phone_number = '0947295260');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '50d9e42c-a3dc-4a79-918e-2b06785d643a', 'Hồ Thị Phúc', 'user105_phúc@gmail.com', '0991030736', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1988-01-24', 'Quận 8, Huế', NOW() - interval '16 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user105_phúc@gmail.com' OR phone_number = '0991030736');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '9ef1a032-c804-48dc-8e5b-8aeb47d97f5f', 'Hồ Thị Yến', 'user106_mai@gmail.com', '0994374605', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1999-06-19', 'Quận 4, Hà Nội', NOW() - interval '6 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user106_mai@gmail.com' OR phone_number = '0994374605');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '6dc3ea5e-e625-4374-ab7d-4288bb5905eb', 'Vũ Hồng Mai', 'user107_châu@gmail.com', '0941244663', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1983-07-09', 'Quận 8, Buôn Ma Thuột', NOW() - interval '47 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user107_châu@gmail.com' OR phone_number = '0941244663');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '91724e3d-68af-4446-863d-9536d39a9c37', 'Huỳnh Minh Phúc', 'user108_huy@gmail.com', '0999949389', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1988-12-22', 'Quận 11, Hà Nội', NOW() - interval '78 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user108_huy@gmail.com' OR phone_number = '0999949389');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'c3aa2c9a-589e-4f8e-bd49-013d1e3e1154', 'Huỳnh Thảo Vy', 'user109_khoa@gmail.com', '0931931511', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1994-07-09', 'Quận 11, Huế', NOW() - interval '29 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user109_khoa@gmail.com' OR phone_number = '0931931511');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'e1b6326c-2b85-4beb-a1a7-ae860845b854', 'Bùi Gia Hân', 'user110_hân@gmail.com', '0917507864', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1987-01-26', 'Quận 6, Vũng Tàu', NOW() - interval '35 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user110_hân@gmail.com' OR phone_number = '0917507864');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'cfdb6d58-4470-43e0-ab32-4f05354a5018', 'Lê Đức Trinh', 'user111_thắng@gmail.com', '0952235350', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1986-11-16', 'Quận 7, Buôn Ma Thuột', NOW() - interval '59 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user111_thắng@gmail.com' OR phone_number = '0952235350');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '30d55bb8-57e9-4022-8ad0-01c950975e83', 'Hoàng Hoàng Em', 'user112_khoa@gmail.com', '0985345555', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1997-05-24', 'Quận 10, Vũng Tàu', NOW() - interval '75 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user112_khoa@gmail.com' OR phone_number = '0985345555');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'ad4d67a1-473b-44d6-b041-e714e75b451f', 'Hồ Minh Khoa', 'user113_em@gmail.com', '0978387461', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1995-02-25', 'Quận 1, Hà Nội', NOW() - interval '20 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user113_em@gmail.com' OR phone_number = '0978387461');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '1cdd8069-13a6-49c3-af0f-1418bb9a5534', 'Huỳnh Hồng Thịnh', 'user114_sơn@gmail.com', '0990048665', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1982-07-13', 'Quận 10, Hải Phòng', NOW() - interval '68 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user114_sơn@gmail.com' OR phone_number = '0990048665');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '55820997-b8e6-4794-8bdc-8b3285c23c16', 'Võ Thảo An', 'user115_thịnh@gmail.com', '0925374874', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2001-09-25', 'Quận 5, Buôn Ma Thuột', NOW() - interval '44 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user115_thịnh@gmail.com' OR phone_number = '0925374874');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'e5dd4365-9f31-4749-b6c8-8b7e42485c83', 'Phạm Hoàng Sơn', 'user116_giang@gmail.com', '0970897765', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1980-12-24', 'Quận 5, Huế', NOW() - interval '23 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user116_giang@gmail.com' OR phone_number = '0970897765');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'a7b2d979-ef6a-49dc-928d-2f6b6fd5cded', 'Phạm Gia Trí', 'user117_mai@gmail.com', '0995758349', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1996-10-07', 'Quận 3, Đà Lạt', NOW() - interval '21 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user117_mai@gmail.com' OR phone_number = '0995758349');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '9ff1255c-8f67-4986-8e7c-8187a9c6ebbb', 'Nguyễn Hải Nam', 'user118_uyên@gmail.com', '0912614124', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1983-06-27', 'Quận 5, Cần Thơ', NOW() - interval '8 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user118_uyên@gmail.com' OR phone_number = '0912614124');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '994310ec-cc07-49dd-a41c-e22d8107331f', 'Vũ Đình Trinh', 'user119_châu@gmail.com', '0921496211', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2003-08-27', 'Quận 2, Huế', NOW() - interval '17 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user119_châu@gmail.com' OR phone_number = '0921496211');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '3b55696b-4ddf-4220-8129-a3a96cb041d8', 'Hoàng Tuấn Uyên', 'user120_yến@gmail.com', '0932162965', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1988-09-28', 'Quận 10, Vũng Tàu', NOW() - interval '28 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user120_yến@gmail.com' OR phone_number = '0932162965');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '623ea108-d06d-4883-a8e3-6d353f67a68c', 'Phan Xuân Mai', 'user121_quân@gmail.com', '0997225156', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1991-08-17', 'Quận 8, Hà Nội', NOW() - interval '32 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user121_quân@gmail.com' OR phone_number = '0997225156');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '0db948b5-2032-4df7-9579-cff0ee46847e', 'Vũ Thị Nam', 'user122_an@gmail.com', '0988961459', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1997-04-19', 'Quận 4, TP.HCM', NOW() - interval '10 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user122_an@gmail.com' OR phone_number = '0988961459');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '0e691da1-c4bb-45d7-b6f6-d06967c20f29', 'Trần Đức Châu', 'user123_bình@gmail.com', '0954349361', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1982-09-08', 'Quận 5, Buôn Ma Thuột', NOW() - interval '63 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user123_bình@gmail.com' OR phone_number = '0954349361');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '41451dae-e2bc-4469-8e0f-7b6acf745d8d', 'Phan Thảo Em', 'user124_vy@gmail.com', '0986644106', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1998-08-08', 'Quận 8, Vũng Tàu', NOW() - interval '25 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user124_vy@gmail.com' OR phone_number = '0986644106');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '0ec95d81-b89d-4d8f-a684-fd7636b5e054', 'Phạm Thị Thịnh', 'user125_sơn@gmail.com', '0957553014', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1993-07-15', 'Quận 12, TP.HCM', NOW() - interval '87 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user125_sơn@gmail.com' OR phone_number = '0957553014');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '557e52d9-bc9b-4b3f-ac50-eb772585fa3e', 'Phạm Văn Quân', 'user126_vy@gmail.com', '0955540424', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1983-04-07', 'Quận 4, Huế', NOW() - interval '58 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user126_vy@gmail.com' OR phone_number = '0955540424');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'd72e34f0-1e04-4396-8dec-5d3c7fc57fd0', 'Hoàng Thanh Giang', 'user127_linh@gmail.com', '0972092888', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1987-02-15', 'Quận 9, Hà Nội', NOW() - interval '7 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user127_linh@gmail.com' OR phone_number = '0972092888');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'cacce60d-955f-4341-a0ae-989192aef4fb', 'Nguyễn Thị Hân', 'user128_khoa@gmail.com', '0932321899', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1993-08-16', 'Quận 4, Vũng Tàu', NOW() - interval '8 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user128_khoa@gmail.com' OR phone_number = '0932321899');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'edc4faba-16f5-4c93-ad6a-6195ae4c07de', 'Huỳnh Thanh An', 'user129_quân@gmail.com', '0945594951', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1994-05-14', 'Quận 12, Huế', NOW() - interval '85 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user129_quân@gmail.com' OR phone_number = '0945594951');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '3d790c7b-97dd-4a6b-a504-8299376b13e4', 'Lý Quốc Huy', 'user130_mai@gmail.com', '0939219319', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1981-10-24', 'Quận 9, TP.HCM', NOW() - interval '41 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user130_mai@gmail.com' OR phone_number = '0939219319');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '42fcbc61-63a5-4056-83b1-ba4f36ab12ec', 'Trần Văn Trinh', 'user131_uyên@gmail.com', '0977491435', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1996-03-02', 'Quận 9, Hà Nội', NOW() - interval '24 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user131_uyên@gmail.com' OR phone_number = '0977491435');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'cbd64e8e-b543-4235-b628-51400c30977e', 'Lê Hải Châu', 'user132_thịnh@gmail.com', '0941568532', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1992-02-19', 'Quận 4, Quy Nhơn', NOW() - interval '77 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user132_thịnh@gmail.com' OR phone_number = '0941568532');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'ee58eb9d-1b5e-40ff-9043-bef3528bfc53', 'Trần Hải Châu', 'user133_sơn@gmail.com', '0998231132', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1998-10-17', 'Quận 6, Nha Trang', NOW() - interval '27 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user133_sơn@gmail.com' OR phone_number = '0998231132');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '765e1ff6-12a1-464e-a404-63b26a0889ac', 'Bùi Đức Linh', 'user134_quân@gmail.com', '0927566185', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2001-11-10', 'Quận 8, Đà Lạt', NOW() - interval '10 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user134_quân@gmail.com' OR phone_number = '0927566185');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'e848bd18-1f43-4e3d-9025-707f29d73d38', 'Nguyễn Ngọc Kiệt', 'user135_trinh@gmail.com', '0923419256', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1982-09-07', 'Quận 9, Nha Trang', NOW() - interval '17 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user135_trinh@gmail.com' OR phone_number = '0923419256');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '8a4651d4-5425-440f-8733-619ebd83dbb2', 'Đỗ Đình Châu', 'user136_khoa@gmail.com', '0959597086', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1989-03-15', 'Quận 9, Nha Trang', NOW() - interval '79 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user136_khoa@gmail.com' OR phone_number = '0959597086');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '4c39bcca-76f4-445c-8562-415dd58167d3', 'Nguyễn Tuấn Yến', 'user137_mai@gmail.com', '0999038526', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1983-03-09', 'Quận 2, Hà Nội', NOW() - interval '71 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user137_mai@gmail.com' OR phone_number = '0999038526');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '53888546-b4b1-4463-8cf5-d7795383d78f', 'Hoàng Hoàng Mai', 'user138_kiệt@gmail.com', '0938270233', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2002-06-07', 'Quận 11, Buôn Ma Thuột', NOW() - interval '34 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user138_kiệt@gmail.com' OR phone_number = '0938270233');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'fc229f6c-395e-4b88-a48d-261952ddcafa', 'Lý Hoàng Bình', 'user139_châu@gmail.com', '0995132217', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1993-05-02', 'Quận 1, Đà Lạt', NOW() - interval '17 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user139_châu@gmail.com' OR phone_number = '0995132217');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'ae23ca11-b421-4e64-a568-9bb437652031', 'Võ Quốc Vy', 'user140_tâm@gmail.com', '0984045292', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2002-07-18', 'Quận 1, Hà Nội', NOW() - interval '10 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user140_tâm@gmail.com' OR phone_number = '0984045292');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '2c607b9b-4479-47d4-8e15-21df97ca4336', 'Hoàng Thảo Bình', 'user141_phúc@gmail.com', '0988183110', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1997-03-14', 'Quận 3, TP.HCM', NOW() - interval '40 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user141_phúc@gmail.com' OR phone_number = '0988183110');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'bb17d5d9-f874-4138-8ba2-aa55232ca5a4', 'Đỗ Đình Bình', 'user142_phúc@gmail.com', '0938195995', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2001-04-22', 'Quận 2, Đà Lạt', NOW() - interval '72 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user142_phúc@gmail.com' OR phone_number = '0938195995');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '866cda95-de37-4c2e-9342-dc9f54848549', 'Ngô Hải Vy', 'user143_em@gmail.com', '0941774346', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1985-03-14', 'Quận 1, Đà Nẵng', NOW() - interval '43 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user143_em@gmail.com' OR phone_number = '0941774346');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '88a5f227-c03c-4f3f-af4e-34115edb7a96', 'Ngô Hồng Thịnh', 'user144_vy@gmail.com', '0943308443', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1988-03-26', 'Quận 12, Hà Nội', NOW() - interval '49 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user144_vy@gmail.com' OR phone_number = '0943308443');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'bc9e6e95-bd86-4c7d-b4a1-7c7acac2137b', 'Trần Gia Uyên', 'user145_khoa@gmail.com', '0936786211', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1994-06-10', 'Quận 4, Cần Thơ', NOW() - interval '4 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user145_khoa@gmail.com' OR phone_number = '0936786211');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'aba4becf-934c-4f81-9aa2-bf818e6fa279', 'Phan Thanh Nam', 'user146_linh@gmail.com', '0919317495', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2004-05-12', 'Quận 11, Huế', NOW() - interval '52 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user146_linh@gmail.com' OR phone_number = '0919317495');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '67ef14ac-7ab4-45a5-9dfd-81e8d68c6f4f', 'Bùi Văn Dũng', 'user147_linh@gmail.com', '0933966966', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1998-05-02', 'Quận 2, Quy Nhơn', NOW() - interval '56 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user147_linh@gmail.com' OR phone_number = '0933966966');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'fb1e2029-c93a-465f-bf53-bf5fdbae703b', 'Đỗ Xuân Nam', 'user148_sơn@gmail.com', '0991363974', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1996-02-13', 'Quận 10, Cần Thơ', NOW() - interval '33 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user148_sơn@gmail.com' OR phone_number = '0991363974');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'bc326482-3af4-489d-bc06-fe7ddf54675a', 'Trần Xuân Sơn', 'user149_an@gmail.com', '0979782527', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1997-11-24', 'Quận 12, Buôn Ma Thuột', NOW() - interval '26 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user149_an@gmail.com' OR phone_number = '0979782527');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '34684288-1f75-4dd9-85f9-bfeb926fbb79', 'Đỗ Thanh Châu', 'user150_thịnh@gmail.com', '0954318698', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1999-06-22', 'Quận 2, Nha Trang', NOW() - interval '65 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user150_thịnh@gmail.com' OR phone_number = '0954318698');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '0cd0db71-f965-4b65-aa12-4dd2bb439ecd', 'Đặng Tuấn Sơn', 'user151_nam@gmail.com', '0964009265', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2002-05-18', 'Quận 3, Cần Thơ', NOW() - interval '54 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user151_nam@gmail.com' OR phone_number = '0964009265');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '571102c5-b9aa-417d-b14b-7da0fb4771bb', 'Hồ Tuấn Vy', 'user152_giang@gmail.com', '0992613013', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1998-05-13', 'Quận 9, TP.HCM', NOW() - interval '39 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user152_giang@gmail.com' OR phone_number = '0992613013');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '07cdd1a1-a12d-42cb-b5d2-5f9cff12c030', 'Đặng Đức Sơn', 'user153_trinh@gmail.com', '0991424737', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2000-06-15', 'Quận 8, Hải Phòng', NOW() - interval '87 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user153_trinh@gmail.com' OR phone_number = '0991424737');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '9b677d83-1926-4c0c-bfe6-5b0543b388d4', 'Phan Thảo Uyên', 'user154_vy@gmail.com', '0932775593', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2001-02-10', 'Quận 9, Buôn Ma Thuột', NOW() - interval '82 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user154_vy@gmail.com' OR phone_number = '0932775593');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '4808bfd3-3152-454f-b495-fad4cdef5f12', 'Bùi Thị Hân', 'user155_khoa@gmail.com', '0951663795', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1987-04-05', 'Quận 1, TP.HCM', NOW() - interval '32 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user155_khoa@gmail.com' OR phone_number = '0951663795');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '29870dde-eb63-4b83-aa4d-d982e7057a8f', 'Lý Hải Hân', 'user156_châu@gmail.com', '0971124923', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1993-11-19', 'Quận 4, Vũng Tàu', NOW() - interval '64 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user156_châu@gmail.com' OR phone_number = '0971124923');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '0290fc2e-244d-4f94-ba1c-6dcaec82f6eb', 'Hồ Đức Em', 'user157_trí@gmail.com', '0910744212', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2004-02-25', 'Quận 7, Cần Thơ', NOW() - interval '23 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user157_trí@gmail.com' OR phone_number = '0910744212');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '05272cfb-291a-4c3f-855d-b83089354989', 'Dương Văn Yến', 'user158_khoa@gmail.com', '0926288475', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1994-03-26', 'Quận 8, Buôn Ma Thuột', NOW() - interval '68 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user158_khoa@gmail.com' OR phone_number = '0926288475');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '5298bb57-92f4-4594-9130-b3d0d824c26f', 'Bùi Hồng Tâm', 'user159_kiệt@gmail.com', '0977750178', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1993-09-15', 'Quận 3, Hải Phòng', NOW() - interval '58 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user159_kiệt@gmail.com' OR phone_number = '0977750178');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'fcf639f7-5fc6-4c9d-a959-29b7239838ee', 'Võ Hồng Khoa', 'user160_trí@gmail.com', '0947220099', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2004-09-16', 'Quận 11, Cần Thơ', NOW() - interval '36 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user160_trí@gmail.com' OR phone_number = '0947220099');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'cdb25c30-9824-48a2-9cc9-cb1d65f41aaf', 'Dương Thị Thắng', 'user161_mai@gmail.com', '0941473195', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1988-06-11', 'Quận 9, Hà Nội', NOW() - interval '18 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user161_mai@gmail.com' OR phone_number = '0941473195');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '2d30c144-19d4-4c48-8a7d-aadd559d88c9', 'Hoàng Đức Quân', 'user162_thắng@gmail.com', '0930509175', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2002-04-03', 'Quận 7, Vũng Tàu', NOW() - interval '43 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user162_thắng@gmail.com' OR phone_number = '0930509175');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'b5d3b111-77a4-47fd-bf4d-e04fde80e441', 'Dương Thanh Bình', 'user163_huy@gmail.com', '0966390708', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1992-10-23', 'Quận 1, Quy Nhơn', NOW() - interval '49 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user163_huy@gmail.com' OR phone_number = '0966390708');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '088103d2-02b2-4092-9fb9-73475279bb38', 'Lý Văn Phúc', 'user164_mai@gmail.com', '0962343121', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1993-09-24', 'Quận 12, Huế', NOW() - interval '78 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user164_mai@gmail.com' OR phone_number = '0962343121');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'b4cc3d04-2698-4a5d-94bf-5da3f94aa58f', 'Vũ Ngọc Khoa', 'user165_linh@gmail.com', '0968496914', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1995-01-13', 'Quận 6, Buôn Ma Thuột', NOW() - interval '87 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user165_linh@gmail.com' OR phone_number = '0968496914');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'd34c89b9-e8b9-4532-b6e8-33efd6da59cf', 'Hồ Xuân Giang', 'user166_tâm@gmail.com', '0927129912', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1999-09-01', 'Quận 7, Quy Nhơn', NOW() - interval '73 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user166_tâm@gmail.com' OR phone_number = '0927129912');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '3341ceee-428c-4dfd-82d6-e369552aea6a', 'Nguyễn Thị Trí', 'user167_sơn@gmail.com', '0928213276', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1994-03-02', 'Quận 5, Vũng Tàu', NOW() - interval '42 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user167_sơn@gmail.com' OR phone_number = '0928213276');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'aaab45a6-ebe9-4374-a588-5e0d7fa304af', 'Phan Ngọc Nam', 'user168_nam@gmail.com', '0960885459', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1988-07-09', 'Quận 2, Hải Phòng', NOW() - interval '3 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user168_nam@gmail.com' OR phone_number = '0960885459');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '6160e89d-9213-47f9-9968-d9d3e8432194', 'Trần Minh Khoa', 'user169_trí@gmail.com', '0919209801', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2004-11-02', 'Quận 1, Cần Thơ', NOW() - interval '26 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user169_trí@gmail.com' OR phone_number = '0919209801');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '5cf9c10f-af16-4c3c-ba5d-db4d4f17e5ec', 'Nguyễn Hải Em', 'user170_khoa@gmail.com', '0926941092', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1995-11-04', 'Quận 10, Cần Thơ', NOW() - interval '60 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user170_khoa@gmail.com' OR phone_number = '0926941092');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'e96f0268-5f10-4a65-a2ba-36a9ec049b7d', 'Võ Hồng Phúc', 'user171_giang@gmail.com', '0991321540', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1999-12-23', 'Quận 2, Đà Nẵng', NOW() - interval '40 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user171_giang@gmail.com' OR phone_number = '0991321540');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '8932c09e-c7c3-4d2f-b7ba-3b87aed3e9be', 'Phạm Hải An', 'user172_mai@gmail.com', '0987282128', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2001-07-13', 'Quận 12, Cần Thơ', NOW() - interval '10 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user172_mai@gmail.com' OR phone_number = '0987282128');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '7ab1cd52-f3de-4046-800d-c4ac30b8d4f2', 'Vũ Thị Thắng', 'user173_hân@gmail.com', '0950477742', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2001-10-26', 'Quận 2, Quy Nhơn', NOW() - interval '6 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user173_hân@gmail.com' OR phone_number = '0950477742');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'cfc581f6-3441-484b-a4c4-f02dc164a26e', 'Đỗ Thảo Sơn', 'user174_thịnh@gmail.com', '0959737181', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1982-09-21', 'Quận 6, TP.HCM', NOW() - interval '54 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user174_thịnh@gmail.com' OR phone_number = '0959737181');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'eaf5f19d-2b77-4f11-b4af-d92edeea443c', 'Lý Thị Sơn', 'user175_phúc@gmail.com', '0995306788', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1994-12-05', 'Quận 7, Đà Nẵng', NOW() - interval '67 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user175_phúc@gmail.com' OR phone_number = '0995306788');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '5db96f63-949f-477d-8c89-5bbf67e77a40', 'Võ Hải Yến', 'user176_hân@gmail.com', '0974893936', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1994-07-27', 'Quận 12, Quy Nhơn', NOW() - interval '35 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user176_hân@gmail.com' OR phone_number = '0974893936');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '9e28168b-8289-48d4-820e-3c2572a3aa55', 'Bùi Gia Khoa', 'user177_châu@gmail.com', '0947437115', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1994-04-25', 'Quận 8, Quy Nhơn', NOW() - interval '79 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user177_châu@gmail.com' OR phone_number = '0947437115');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '3e7e139c-7ba6-4918-923f-7824e6921fe0', 'Hồ Minh An', 'user178_uyên@gmail.com', '0953622663', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1985-08-07', 'Quận 6, Nha Trang', NOW() - interval '44 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user178_uyên@gmail.com' OR phone_number = '0953622663');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '18ad9318-708f-4da5-933e-cf2e82df65d2', 'Võ Đình Kiệt', 'user179_thắng@gmail.com', '0947080140', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1997-01-17', 'Quận 4, Hà Nội', NOW() - interval '31 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user179_thắng@gmail.com' OR phone_number = '0947080140');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'e9031e4f-dad1-4fc2-9cd4-687740f81665', 'Ngô Ngọc Yến', 'user180_hân@gmail.com', '0942255732', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2002-08-21', 'Quận 12, Hải Phòng', NOW() - interval '58 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user180_hân@gmail.com' OR phone_number = '0942255732');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '62aecc8e-fe5a-4e5d-ad86-922ca0b4063c', 'Nguyễn Thị Mai', 'user181_khoa@gmail.com', '0964277800', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2002-04-10', 'Quận 11, Quy Nhơn', NOW() - interval '48 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user181_khoa@gmail.com' OR phone_number = '0964277800');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '62b2acea-7c48-48d5-b3f7-e0365a97a507', 'Lý Thảo Vinh', 'user182_phúc@gmail.com', '0967110154', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2003-09-11', 'Quận 6, Hải Phòng', NOW() - interval '35 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user182_phúc@gmail.com' OR phone_number = '0967110154');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '69f5dd32-de16-430b-99ea-270311cc9b71', 'Đặng Hoàng Khoa', 'user183_dũng@gmail.com', '0935848226', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1990-02-24', 'Quận 9, Đà Nẵng', NOW() - interval '25 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user183_dũng@gmail.com' OR phone_number = '0935848226');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'd21e2c3e-531a-43a8-a87d-59cf3d79f3a6', 'Phan Xuân Uyên', 'user184_linh@gmail.com', '0989133014', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2004-09-20', 'Quận 5, Hà Nội', NOW() - interval '25 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user184_linh@gmail.com' OR phone_number = '0989133014');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'b7c5ee56-53cb-4bf7-88c9-e97c38c77d16', 'Đặng Đức Phúc', 'user185_giang@gmail.com', '0950569669', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1980-12-18', 'Quận 3, Nha Trang', NOW() - interval '6 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user185_giang@gmail.com' OR phone_number = '0950569669');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '46416fb0-09fd-44d9-894f-ac7e22553145', 'Trần Thảo Mai', 'user186_thắng@gmail.com', '0926948946', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2000-08-04', 'Quận 1, Quy Nhơn', NOW() - interval '37 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user186_thắng@gmail.com' OR phone_number = '0926948946');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '40c5c452-3921-49cc-83a9-afcbe305d65a', 'Lý Ngọc Tâm', 'user187_nam@gmail.com', '0934744872', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1981-05-28', 'Quận 8, Hà Nội', NOW() - interval '9 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user187_nam@gmail.com' OR phone_number = '0934744872');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '71f632da-7e0b-4087-8f90-17303c3b6c04', 'Hồ Ngọc Châu', 'user188_trinh@gmail.com', '0994482772', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2001-01-05', 'Quận 3, Quy Nhơn', NOW() - interval '39 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user188_trinh@gmail.com' OR phone_number = '0994482772');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '6c59d4b7-8545-4332-9ef6-51fcb1fa6e61', 'Lê Đức Dũng', 'user189_yến@gmail.com', '0965857931', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1999-10-26', 'Quận 10, Cần Thơ', NOW() - interval '67 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user189_yến@gmail.com' OR phone_number = '0965857931');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '53b002f3-f0f8-4cef-afdc-0534eea9a609', 'Hồ Ngọc Tâm', 'user190_mai@gmail.com', '0988990506', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1993-05-19', 'Quận 10, TP.HCM', NOW() - interval '79 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user190_mai@gmail.com' OR phone_number = '0988990506');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '73692c2b-b23a-428c-838d-51e612fbf155', 'Phạm Hồng Huy', 'user191_trí@gmail.com', '0938323322', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1988-11-03', 'Quận 3, Cần Thơ', NOW() - interval '23 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user191_trí@gmail.com' OR phone_number = '0938323322');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'a48081b1-3774-4da8-8e17-c05ad04db02b', 'Lê Quốc An', 'user192_sơn@gmail.com', '0970465583', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2002-10-16', 'Quận 5, TP.HCM', NOW() - interval '30 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user192_sơn@gmail.com' OR phone_number = '0970465583');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '064c7b3f-8928-4831-904c-4ddaa0c4c6b3', 'Đặng Xuân Mai', 'user193_thắng@gmail.com', '0970939053', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1982-11-08', 'Quận 5, Buôn Ma Thuột', NOW() - interval '76 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user193_thắng@gmail.com' OR phone_number = '0970939053');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '0ec1a1d1-f728-43de-a0ff-fdd1f4530f79', 'Phan Thanh Dũng', 'user194_yến@gmail.com', '0940173413', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2000-03-09', 'Quận 3, Hà Nội', NOW() - interval '8 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user194_yến@gmail.com' OR phone_number = '0940173413');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '44128ca5-2e35-446d-8ed1-5718da9d1419', 'Huỳnh Hồng Mai', 'user195_kiệt@gmail.com', '0986397676', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1989-08-04', 'Quận 8, Nha Trang', NOW() - interval '90 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user195_kiệt@gmail.com' OR phone_number = '0986397676');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'bf4c1945-3813-4428-b098-bc7c5dfcb0ae', 'Hồ Hoàng Vinh', 'user196_yến@gmail.com', '0976276072', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1994-02-20', 'Quận 1, Vũng Tàu', NOW() - interval '42 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user196_yến@gmail.com' OR phone_number = '0976276072');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'a4c405ba-0b8f-4daa-bf99-43a710f023ca', 'Võ Văn Châu', 'user197_khoa@gmail.com', '0987201917', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1998-01-25', 'Quận 11, Nha Trang', NOW() - interval '74 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user197_khoa@gmail.com' OR phone_number = '0987201917');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'c5d8554b-f28f-42e0-b047-06c0ac7f00a9', 'Trần Hồng Hân', 'user198_giang@gmail.com', '0973149551', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1996-11-15', 'Quận 5, Đà Nẵng', NOW() - interval '75 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user198_giang@gmail.com' OR phone_number = '0973149551');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'e2f4c15c-d8d7-45dc-b098-153547f8ec8e', 'Ngô Tuấn Uyên', 'user199_châu@gmail.com', '0973083727', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1991-07-11', 'Quận 6, Buôn Ma Thuột', NOW() - interval '14 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user199_châu@gmail.com' OR phone_number = '0973083727');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'cfe64c60-cd00-4810-a719-a2e69e1ed6a8', 'Huỳnh Minh Sơn', 'user200_thắng@gmail.com', '0976500726', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1989-11-13', 'Quận 9, TP.HCM', NOW() - interval '59 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user200_thắng@gmail.com' OR phone_number = '0976500726');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '0e695e07-963d-4ff7-8fbc-4fa428a1c930', 'Lê Minh Linh', 'user201_nam@gmail.com', '0925558733', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2004-07-28', 'Quận 9, TP.HCM', NOW() - interval '85 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user201_nam@gmail.com' OR phone_number = '0925558733');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '27a4155d-4e05-4130-97ed-21d437bf06e3', 'Dương Thanh Bình', 'user202_huy@gmail.com', '0979583757', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1991-10-25', 'Quận 8, Buôn Ma Thuột', NOW() - interval '57 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user202_huy@gmail.com' OR phone_number = '0979583757');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'ad4306f8-e173-4d31-954a-64fc45b909a8', 'Trần Đức Linh', 'user203_yến@gmail.com', '0927584297', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1989-08-23', 'Quận 8, Hà Nội', NOW() - interval '4 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user203_yến@gmail.com' OR phone_number = '0927584297');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '16b7d610-f749-41bb-97c0-548348f4ec18', 'Vũ Xuân Giang', 'user204_mai@gmail.com', '0983932360', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1980-09-14', 'Quận 2, Cần Thơ', NOW() - interval '15 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user204_mai@gmail.com' OR phone_number = '0983932360');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '80aaa9fb-2c78-4c33-8a3d-26dbab558e74', 'Dương Thị Trí', 'user205_em@gmail.com', '0976890827', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2002-05-17', 'Quận 12, Nha Trang', NOW() - interval '54 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user205_em@gmail.com' OR phone_number = '0976890827');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '7099a267-069a-4e71-a62a-0a5f029944ba', 'Lý Ngọc Khoa', 'user206_tâm@gmail.com', '0983989546', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1984-07-07', 'Quận 10, Huế', NOW() - interval '18 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user206_tâm@gmail.com' OR phone_number = '0983989546');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '3d760ba9-1d3b-4526-ab53-9a035a23b1b3', 'Lê Hoàng Hân', 'user207_sơn@gmail.com', '0955617282', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1996-05-27', 'Quận 1, Nha Trang', NOW() - interval '39 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user207_sơn@gmail.com' OR phone_number = '0955617282');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '838aef54-14d9-4df8-a6a0-7fadabbcc9b4', 'Lý Gia Em', 'user208_tâm@gmail.com', '0982287061', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1995-06-11', 'Quận 9, Huế', NOW() - interval '49 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user208_tâm@gmail.com' OR phone_number = '0982287061');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '9b213ea1-e73e-428d-b4e9-9f7e28dd9acb', 'Dương Đình Nam', 'user209_huy@gmail.com', '0942051937', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1998-07-08', 'Quận 7, TP.HCM', NOW() - interval '41 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user209_huy@gmail.com' OR phone_number = '0942051937');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'e95672c1-f7f5-4f99-97d6-b933292850aa', 'Lý Xuân Quân', 'user210_quân@gmail.com', '0999078806', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2000-03-16', 'Quận 1, Đà Nẵng', NOW() - interval '65 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user210_quân@gmail.com' OR phone_number = '0999078806');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '86e9c7ea-f143-4feb-a3c3-0473b3babf3b', 'Bùi Gia Dũng', 'user211_tâm@gmail.com', '0923383077', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1996-08-01', 'Quận 12, Đà Nẵng', NOW() - interval '53 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user211_tâm@gmail.com' OR phone_number = '0923383077');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'd9b49f71-8ad8-4705-91b6-967bf1fb52c5', 'Hoàng Thị Uyên', 'user212_linh@gmail.com', '0955440919', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1999-12-13', 'Quận 11, Hà Nội', NOW() - interval '43 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user212_linh@gmail.com' OR phone_number = '0955440919');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '4fc5cb15-5d6e-4435-af1a-590e3bbaf356', 'Hồ Minh Trí', 'user213_thắng@gmail.com', '0975491202', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1997-01-20', 'Quận 2, Cần Thơ', NOW() - interval '81 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user213_thắng@gmail.com' OR phone_number = '0975491202');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'dc92446a-9e33-4fc5-bbbd-b63c00f33b19', 'Đặng Đức Vy', 'user214_châu@gmail.com', '0968246664', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1983-11-23', 'Quận 2, Hải Phòng', NOW() - interval '22 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user214_châu@gmail.com' OR phone_number = '0968246664');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '67dc6c51-7ce0-48d6-927d-0274f7650ad0', 'Đặng Đình An', 'user215_bình@gmail.com', '0953536830', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1981-05-12', 'Quận 6, Vũng Tàu', NOW() - interval '19 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user215_bình@gmail.com' OR phone_number = '0953536830');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT 'a49d0404-0f6d-48f7-a467-3300440f6977', 'Vũ Thảo Sơn', 'user216_trinh@gmail.com', '0934166826', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1985-03-03', 'Quận 10, Vũng Tàu', NOW() - interval '80 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user216_trinh@gmail.com' OR phone_number = '0934166826');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '4806371a-4127-4b34-864b-dd03cbbe6702', 'Vũ Ngọc Trinh', 'user217_em@gmail.com', '0941163805', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1994-11-09', 'Quận 8, Nha Trang', NOW() - interval '86 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user217_em@gmail.com' OR phone_number = '0941163805');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '84847766-f0ce-49fa-ba43-2b5fe248cf79', 'Nguyễn Đình Tâm', 'user218_mai@gmail.com', '0983363160', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1985-02-15', 'Quận 6, Quy Nhơn', NOW() - interval '39 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user218_mai@gmail.com' OR phone_number = '0983363160');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '1a081ff2-07cf-47d8-9655-bcb6335edb03', 'Ngô Xuân Linh', 'user219_tâm@gmail.com', '0950550084', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '1986-07-28', 'Quận 8, Hà Nội', NOW() - interval '31 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user219_tâm@gmail.com' OR phone_number = '0950550084');
        

            INSERT INTO customers (id, full_name, email, phone_number, password, status, date_of_birth, address, created_at, updated_at, version)
            SELECT '8a378dea-fddc-441a-b7c2-dd2d663c6a1e', 'Hồ Hải Phúc', 'user220_trinh@gmail.com', '0949711319', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', '2002-05-01', 'Quận 11, Vũng Tàu', NOW() - interval '36 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'user220_trinh@gmail.com' OR phone_number = '0949711319');
        

            INSERT INTO staffs (id, full_name, email, phone_number, password, status, staff_code, role_id, created_at, updated_at, version)
            SELECT 'a36c878b-aa51-47fa-820f-17f4e036a21e', 'Trần Soát Vé 1', 'soatve1@vexe.vn', '0908111001', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', 'STAFF_SV1', 2, NOW() - interval '60 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM staffs WHERE email = 'soatve1@vexe.vn' OR staff_code = 'STAFF_SV1');
        

            INSERT INTO staffs (id, full_name, email, phone_number, password, status, staff_code, role_id, created_at, updated_at, version)
            SELECT '65217991-af13-4be8-86f5-edc211021e03', 'Lê Soát Vé 2', 'soatve2@vexe.vn', '0908111002', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', 'STAFF_SV2', 2, NOW() - interval '60 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM staffs WHERE email = 'soatve2@vexe.vn' OR staff_code = 'STAFF_SV2');
        

            INSERT INTO staffs (id, full_name, email, phone_number, password, status, staff_code, role_id, created_at, updated_at, version)
            SELECT 'bb7a8d73-0f63-49bc-8a11-d034430a9e72', 'Phạm Bác Tài 1', 'taixe1@vexe.vn', '0908111003', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', 'STAFF_TX1', 2, NOW() - interval '60 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM staffs WHERE email = 'taixe1@vexe.vn' OR staff_code = 'STAFF_TX1');
        

            INSERT INTO staffs (id, full_name, email, phone_number, password, status, staff_code, role_id, created_at, updated_at, version)
            SELECT '7dce3961-6fb4-4b0a-b611-41a2edffb2f0', 'Ngô Bác Tài 2', 'taixe2@vexe.vn', '0908111004', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', 'STAFF_TX2', 2, NOW() - interval '60 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM staffs WHERE email = 'taixe2@vexe.vn' OR staff_code = 'STAFF_TX2');
        

            INSERT INTO staffs (id, full_name, email, phone_number, password, status, staff_code, role_id, created_at, updated_at, version)
            SELECT '36721ae4-6698-4add-a122-fc669967d2db', 'Vũ Chăm Sóc Khách Hàng', 'cskh@vexe.vn', '0908111005', '$2a$10$FapijEXrFsZWd9UjRSTxWO5p5oP8VpDWc2ZrwcQ2KN3q38XgyanIu', 'ACTIVE', 'STAFF_CSKH', 2, NOW() - interval '60 days', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM staffs WHERE email = 'cskh@vexe.vn' OR staff_code = 'STAFF_CSKH');
        

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
    

        SELECT setval('routes_id_seq', COALESCE((SELECT max(id) FROM routes), 1));
        SELECT setval('vehicles_id_seq', COALESCE((SELECT max(id) FROM vehicles), 1));
        SELECT setval('seats_id_seq', COALESCE((SELECT max(id) FROM seats), 1));
        SELECT setval('trips_id_seq', COALESCE((SELECT max(id) FROM trips), 1));
        SELECT setval('bookings_id_seq', COALESCE((SELECT max(id) FROM bookings), 1));
        SELECT setval('tickets_id_seq', COALESCE((SELECT max(id) FROM tickets), 1));
        SELECT setval('payments_id_seq', COALESCE((SELECT max(id) FROM payments), 1));
        SELECT setval('cancel_requests_id_seq', COALESCE((SELECT max(id) FROM cancel_requests), 1));
        SELECT setval('table_metadata_id_seq', COALESCE((SELECT max(id) FROM table_metadata), 1));
    
COMMIT;