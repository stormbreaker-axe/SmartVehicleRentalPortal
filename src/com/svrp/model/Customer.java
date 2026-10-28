package com.svrp.model;

public class Customer {
    private int id;
    private String name, email, phone, licenseNumber, passwordHash;
    private boolean otpVerified;

    public Customer(int id, String name, String email, String phone, String license,
                    String passwordHash, boolean otpVerified) {
        this.id = id; this.name = name; this.email = email;
        this.phone = phone; this.licenseNumber = license;
        this.passwordHash = passwordHash; this.otpVerified = otpVerified;
    }

    // Legacy constructor (backward compat)
    public Customer(int id, String name, String email, String phone, String license) {
        this(id, name, email, phone, license, null, false);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getLicenseNumber() { return licenseNumber; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isOtpVerified() { return otpVerified; }

    @Override
    public String toString() {
        return String.format("#%d | %s | %s | %s | License: %s", id, name, email, phone, licenseNumber);
    }
}
