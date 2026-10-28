DROP DATABASE IF EXISTS smart_vehicle_rental;
CREATE DATABASE smart_vehicle_rental;
USE smart_vehicle_rental;
CREATE TABLE vehicles (
 id INT AUTO_INCREMENT PRIMARY KEY, vehicle_type VARCHAR(30) NOT NULL, make VARCHAR(80) NOT NULL,
 model VARCHAR(80) NOT NULL, license_plate VARCHAR(30) NOT NULL UNIQUE, rental_rate DECIMAL(10,2) NOT NULL,
 available BOOLEAN NOT NULL DEFAULT TRUE, battery_range_km INT NULL, engine_cc INT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE customers (
 id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(120) NOT NULL, email VARCHAR(160) NOT NULL UNIQUE,
 phone VARCHAR(30) NOT NULL, license_number VARCHAR(60) NOT NULL UNIQUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE reservations (
 id INT AUTO_INCREMENT PRIMARY KEY, customer_id INT NOT NULL, vehicle_id INT NOT NULL,
 start_date DATE NOT NULL, end_date DATE NOT NULL, total_cost DECIMAL(10,2) NOT NULL,
 status ENUM('CONFIRMED','CANCELLED','COMPLETED') DEFAULT 'CONFIRMED', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(customer_id) REFERENCES customers(id), FOREIGN KEY(vehicle_id) REFERENCES vehicles(id),
 CHECK(end_date >= start_date)
);
CREATE TABLE payments (
 id INT AUTO_INCREMENT PRIMARY KEY, reservation_id INT NOT NULL UNIQUE, amount DECIMAL(10,2) NOT NULL,
 payment_method ENUM('CASH','CARD','UPI','NET_BANKING') NOT NULL, status ENUM('PAID','PENDING','REFUNDED') DEFAULT 'PAID',
 paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(reservation_id) REFERENCES reservations(id)
);
CREATE INDEX idx_vehicle_dates ON reservations(vehicle_id,start_date,end_date,status);
INSERT INTO vehicles(vehicle_type,make,model,license_plate,rental_rate,available,battery_range_km,engine_cc) VALUES
('CAR','Toyota','Etios','KL01AB1234',1800,TRUE,NULL,1496),
('CAR','Hyundai','i20','KL01CD5678',1600,TRUE,NULL,1197),
('BIKE','Honda','Hornet 2.0','KL02EF1111',700,TRUE,NULL,184),
('BIKE','Yamaha','MT-15','KL02GH2222',850,TRUE,NULL,155),
('ELECTRIC_SCOOTER','Ather','450X','KL03IJ3333',600,TRUE,150,NULL),
('ELECTRIC_SCOOTER','Ola','S1 Pro','KL03KL4444',650,TRUE,176,NULL);
