-- Smart Vehicle Rental Portal v2 Schema Migration
-- Compatible with MySQL 5.7 / 8.0

USE smart_vehicle_rental;

-- ─────────────────────────────────────────────
-- vehicles: extra columns (run safely - ignore duplicate column errors)
-- ─────────────────────────────────────────────
ALTER TABLE vehicles ADD COLUMN colour        VARCHAR(50)  DEFAULT 'Unknown';
ALTER TABLE vehicles ADD COLUMN fuel_type     VARCHAR(20)  DEFAULT 'PETROL';
ALTER TABLE vehicles ADD COLUMN num_seats     INT          DEFAULT 2;
ALTER TABLE vehicles ADD COLUMN kms_travelled INT          DEFAULT 0;
ALTER TABLE vehicles ADD COLUMN image_url     VARCHAR(300) DEFAULT NULL;

UPDATE vehicles SET colour='White',  fuel_type='PETROL',   num_seats=5, kms_travelled=24000 WHERE license_plate='KL01AB1234';
UPDATE vehicles SET colour='Red',    fuel_type='PETROL',   num_seats=5, kms_travelled=18000 WHERE license_plate='KL01CD5678';
UPDATE vehicles SET colour='Black',  fuel_type='PETROL',   num_seats=2, kms_travelled=12000 WHERE license_plate='KL02EF1111';
UPDATE vehicles SET colour='Blue',   fuel_type='PETROL',   num_seats=2, kms_travelled=8000  WHERE license_plate='KL02GH2222';
UPDATE vehicles SET colour='Grey',   fuel_type='ELECTRIC', num_seats=2, kms_travelled=5000  WHERE license_plate='KL03IJ3333';
UPDATE vehicles SET colour='Silver', fuel_type='ELECTRIC', num_seats=2, kms_travelled=3000  WHERE license_plate='KL03KL4444';

-- ─────────────────────────────────────────────
-- reservations: fine + overdue tracking
-- ─────────────────────────────────────────────
ALTER TABLE reservations ADD COLUMN actual_return_date DATE          DEFAULT NULL;
ALTER TABLE reservations ADD COLUMN fine_amount        DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE reservations ADD COLUMN overdue_flagged    TINYINT(1)    DEFAULT 0;
ALTER TABLE reservations ADD COLUMN fine_rate_per_day  DECIMAL(10,2) DEFAULT 0.00;

-- ─────────────────────────────────────────────
-- OTP store
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS otp_store (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  phone      VARCHAR(30) NOT NULL,
  otp        VARCHAR(10) NOT NULL,
  expires_at TIMESTAMP   NOT NULL,
  verified   TINYINT(1)  DEFAULT 0,
  created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────
-- Admins table
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admins (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  username   VARCHAR(80)  NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,
  created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO admins(username, password) VALUES ('admin', 'admin123');

-- ─────────────────────────────────────────────
-- Customer login credentials
-- ─────────────────────────────────────────────
ALTER TABLE customers ADD COLUMN password_hash VARCHAR(255) DEFAULT NULL;
ALTER TABLE customers ADD COLUMN otp_verified  TINYINT(1)   DEFAULT 0;

-- ─────────────────────────────────────────────
-- Overdue / legal flag reports
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS overdue_reports (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  reservation_id   INT NOT NULL,
  customer_id      INT NOT NULL,
  vehicle_id       INT NOT NULL,
  flagged_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  notes            TEXT,
  report_generated TINYINT(1) DEFAULT 0,
  FOREIGN KEY(reservation_id) REFERENCES reservations(id),
  FOREIGN KEY(customer_id)    REFERENCES customers(id),
  FOREIGN KEY(vehicle_id)     REFERENCES vehicles(id)
);
