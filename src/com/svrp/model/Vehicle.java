package com.svrp.model;

public class Vehicle {
    private int id;
    private String make, model, licensePlate, vehicleType;
    private double rentalRate;
    private boolean available;
    private Integer batterRangeKm, engineCc;
    private String colour, fuelType;
    private int numSeats, kmsTravelled;
    private String imageUrl;

    public Vehicle(int id, String type, String make, String model, String plate,
                   double rate, boolean available, Integer range, Integer cc,
                   String colour, String fuelType, int seats, int kms, String imageUrl) {
        this.id = id; this.vehicleType = type; this.make = make; this.model = model;
        this.licensePlate = plate; this.rentalRate = rate; this.available = available;
        this.batterRangeKm = range; this.engineCc = cc;
        this.colour = colour; this.fuelType = fuelType;
        this.numSeats = seats; this.kmsTravelled = kms; this.imageUrl = imageUrl;
    }

    public double calculateRentalCost(int days) {
        double base = rentalRate * days;
        if ("CAR".equals(vehicleType) && days >= 7) return base * 0.90;
        return base;
    }

    public int getId() { return id; }
    public String getVehicleType() { return vehicleType; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public String getLicensePlate() { return licensePlate; }
    public double getRentalRate() { return rentalRate; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean v) { available = v; }
    public Integer getBatteryRangeKm() { return batterRangeKm; }
    public Integer getEngineCc() { return engineCc; }
    public String getColour() { return colour; }
    public String getFuelType() { return fuelType; }
    public int getNumSeats() { return numSeats; }
    public int getKmsTravelled() { return kmsTravelled; }
    public String getImageUrl() { return imageUrl; }

    @Override
    public String toString() {
        return String.format("#%d | %-17s | %s %s | %s | Rs.%.2f/day | %s",
                id, vehicleType, make, model, licensePlate, rentalRate,
                available ? "AVAILABLE" : "UNAVAILABLE");
    }
}
