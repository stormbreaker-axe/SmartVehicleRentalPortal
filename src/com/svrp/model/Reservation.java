package com.svrp.model;
import java.time.LocalDate;

public class Reservation {
    private int id, customerId, vehicleId;
    private LocalDate startDate, endDate, actualReturnDate;
    private double totalCost, fineAmount, fineRatePerDay;
    private String status;
    private boolean overdueFlagged;

    public Reservation(int id, int customerId, int vehicleId,
                       LocalDate startDate, LocalDate endDate,
                       double totalCost, String status,
                       LocalDate actualReturnDate, double fineAmount,
                       boolean overdueFlagged, double fineRatePerDay) {
        this.id = id; this.customerId = customerId; this.vehicleId = vehicleId;
        this.startDate = startDate; this.endDate = endDate;
        this.totalCost = totalCost; this.status = status;
        this.actualReturnDate = actualReturnDate; this.fineAmount = fineAmount;
        this.overdueFlagged = overdueFlagged; this.fineRatePerDay = fineRatePerDay;
    }

    // Legacy constructor
    public Reservation(int id, int c, int v, LocalDate s, LocalDate e, double cost, String status) {
        this(id, c, v, s, e, cost, status, null, 0, false, 0);
    }

    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public int getVehicleId() { return vehicleId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public double getTotalCost() { return totalCost; }
    public String getStatus() { return status; }
    public LocalDate getActualReturnDate() { return actualReturnDate; }
    public double getFineAmount() { return fineAmount; }
    public boolean isOverdueFlagged() { return overdueFlagged; }
    public double getFineRatePerDay() { return fineRatePerDay; }

    @Override
    public String toString() {
        return String.format("#%d | Customer %d | Vehicle %d | %s to %s | Rs.%.2f | %s",
                id, customerId, vehicleId, startDate, endDate, totalCost, status);
    }
}
