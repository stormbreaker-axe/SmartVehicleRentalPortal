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
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String license) { this.licenseNumber = license; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isOtpVerified() { return otpVerified; }
    public void setOtpVerified(boolean otpVerified) { this.otpVerified = otpVerified; }

    @Override
    public String toString() {
        return String.format("#%d | %s | %s | %s | License: %s", id, name, email, phone, licenseNumber);
    }
}
