package com.svrp.model;

/** Legacy stub — kept for backward compatibility. Use Vehicle directly. */
public class ElectricScooter extends Vehicle {
    public ElectricScooter(int id, String make, String model, String plate, double rate, boolean av, int range) {
        super(id, "ELECTRIC_SCOOTER", make, model, plate, rate, av, range, null, "Unknown", "ELECTRIC", 2, 0, null);
    }
}
