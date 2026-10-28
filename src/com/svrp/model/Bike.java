package com.svrp.model;

/** Legacy stub — kept for backward compatibility. Use Vehicle directly. */
public class Bike extends Vehicle {
    public Bike(int id, String make, String model, String plate, double rate, boolean av, int cc) {
        super(id, "BIKE", make, model, plate, rate, av, null, cc, "Unknown", "PETROL", 2, 0, null);
    }
}
