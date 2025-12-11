-- ============================================
-- SuiteSpot Hotel Management System
-- Database Setup Script
-- ============================================

-- Create database
CREATE DATABASE IF NOT EXISTS suitespot;
USE suitespot;

-- ============================================
-- Users Table
-- ============================================
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  full_name VARCHAR(255),
  role VARCHAR(50) NOT NULL,
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_username (username),
  INDEX idx_email (email),
  INDEX idx_role (role)
);

-- ============================================
-- Guests Table
-- ============================================
CREATE TABLE IF NOT EXISTS guests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  email VARCHAR(255),
  phone VARCHAR(20) NOT NULL,
  id_number VARCHAR(100) NOT NULL,
  id_type VARCHAR(50),
  date_of_birth DATE,
  address VARCHAR(255),
  city VARCHAR(100),
  country VARCHAR(100),
  preferences VARCHAR(500),
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_id_number (id_number),
  INDEX idx_email (email),
  INDEX idx_phone (phone),
  INDEX idx_name (first_name, last_name)
);

-- ============================================
-- Rooms Table
-- ============================================
CREATE TABLE IF NOT EXISTS rooms (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_number VARCHAR(50) NOT NULL UNIQUE,
  type VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL,
  price_per_night DECIMAL(10, 2) NOT NULL,
  capacity INT,
  amenities VARCHAR(500),
  description TEXT,
  floor INT,
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_room_number (room_number),
  INDEX idx_type (type),
  INDEX idx_status (status)
);

-- ============================================
-- Bookings Table
-- ============================================
CREATE TABLE IF NOT EXISTS bookings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  guest_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  check_in_date DATE NOT NULL,
  check_out_date DATE NOT NULL,
  status VARCHAR(50) NOT NULL,
  total_amount DECIMAL(10, 2),
  discount DECIMAL(10, 2) DEFAULT 0.00,
  special_requests TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE RESTRICT,
  FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT,
  INDEX idx_guest_id (guest_id),
  INDEX idx_room_id (room_id),
  INDEX idx_status (status),
  INDEX idx_dates (check_in_date, check_out_date)
);

-- ============================================
-- Bills Table
-- ============================================
CREATE TABLE IF NOT EXISTS bills (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL UNIQUE,
  room_charges DECIMAL(10, 2),
  service_charges DECIMAL(10, 2),
  taxes DECIMAL(10, 2),
  discount DECIMAL(10, 2) DEFAULT 0.00,
  total_amount DECIMAL(10, 2),
  payment_status VARCHAR(50),
  generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  paid_at TIMESTAMP NULL,
  FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
  INDEX idx_booking_id (booking_id),
  INDEX idx_payment_status (payment_status)
);

-- ============================================
-- Taxi Requests Table
-- ============================================
CREATE TABLE IF NOT EXISTS taxi_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  pickup_location VARCHAR(255) NOT NULL DEFAULT 'Hotel',
  destination VARCHAR(255) NOT NULL,
  status VARCHAR(50) NOT NULL,
  requested_time TIMESTAMP NULL,
  estimated_arrival_time TIMESTAMP NULL,
  driver_name VARCHAR(100),
  vehicle_number VARCHAR(50),
  phone_number VARCHAR(20),
  estimated_cost DOUBLE,
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL,
  FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
  INDEX idx_booking_id (booking_id),
  INDEX idx_status (status)
);

-- ============================================
-- System Settings Table
-- ============================================
CREATE TABLE IF NOT EXISTS system_settings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  hotel_name VARCHAR(255),
  hotel_address VARCHAR(255),
  hotel_phone VARCHAR(20),
  hotel_email VARCHAR(255),
  tax_rate DECIMAL(5, 2) DEFAULT 10.00,
  service_charge_rate DECIMAL(5, 2) DEFAULT 5.00,
  currency VARCHAR(10) DEFAULT 'USD',
  checkin_time TIME DEFAULT '14:00:00',
  checkout_time TIME DEFAULT '11:00:00',
  cancellation_policy TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================
-- Insert Default Admin User
-- Username: admin
-- Password: admin123 (BCrypt hash)
-- ============================================
INSERT INTO users (username, password, email, full_name, role, active) 
VALUES ('admin', '$2a$10$slYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy73B12', 'admin@suitespot.com', 'Administrator', 'ADMIN', TRUE)
ON DUPLICATE KEY UPDATE username=username;

-- ============================================
-- Insert Default System Settings
-- ============================================
INSERT INTO system_settings (hotel_name, hotel_address, hotel_phone, hotel_email, tax_rate, service_charge_rate, currency, checkin_time, checkout_time, cancellation_policy)
VALUES (
    'SuiteSpot Hotel',
    '123 Main Street, City',
    '+1-234-567-8900',
    'info@suitespot.com',
    10.00,
    5.00,
    'USD',
    '14:00:00',
    '11:00:00',
    'Free cancellation up to 24 hours before check-in. Cancellations made less than 24 hours before check-in will be charged one night\'s rate.'
)
ON DUPLICATE KEY UPDATE id=id;

-- ============================================
-- Insert Sample Rooms (Optional)
-- ============================================
INSERT INTO rooms (room_number, type, status, price_per_night, capacity, amenities, description, floor, active) VALUES
('101', 'SINGLE', 'AVAILABLE', 100.00, 1, 'WiFi, AC, TV, Bathroom', 'Cozy single room with city view', 1, TRUE),
('102', 'DOUBLE', 'AVAILABLE', 150.00, 2, 'WiFi, AC, TV, Bathroom, King Bed', 'Comfortable double room with modern amenities', 1, TRUE),
('103', 'SUITE', 'AVAILABLE', 250.00, 4, 'WiFi, AC, TV, Bathroom, Living Area, Kitchen', 'Spacious suite perfect for families', 1, TRUE),
('201', 'DELUXE', 'AVAILABLE', 200.00, 2, 'WiFi, AC, TV, Bathroom, Mini Bar, Balcony', 'Luxury deluxe room with balcony view', 2, TRUE),
('202', 'PRESIDENTIAL', 'AVAILABLE', 500.00, 6, 'WiFi, AC, TV, Bathroom, Living Area, Kitchen, Spa, Jacuzzi', 'Presidential suite with premium amenities and panoramic views', 2, TRUE)
ON DUPLICATE KEY UPDATE room_number=room_number;

-- ============================================
-- End of Database Setup
-- ============================================
