package com.svrp.service;
import com.svrp.dao.*; import com.svrp.model.*; import java.sql.SQLException; import java.time.*; import java.time.temporal.ChronoUnit; import java.util.*;
public class RentalService {
 private final VehicleDAO vd;private final CustomerDAO cd;private final ReservationDAO rd;private final PaymentDAO pd;
 public RentalService(VehicleDAO v,CustomerDAO c,ReservationDAO r,PaymentDAO p){vd=v;cd=c;rd=r;pd=p;}
 public int registerCustomer(String n,String e,String ph,String lic)throws SQLException{req(n,"Name");req(e,"Email");req(ph,"Phone");req(lic,"License");return cd.insert(new Customer(0,n,e,ph,lic));}
 public void printVehicles()throws SQLException{System.out.println("\nVEHICLE CATALOG");for(Vehicle v:vd.findAll())System.out.println(v);}
 public boolean available(int id,LocalDate s,LocalDate e)throws SQLException{dates(s,e);Vehicle v=vd.findById(id);if(v==null)throw new IllegalArgumentException("Vehicle not found");return v.isAvailable()&&!rd.conflict(id,s,e);}
 public int reserve(int customerId,int vehicleId,LocalDate s,LocalDate e)throws SQLException{dates(s,e);if(cd.findById(customerId)==null)throw new IllegalArgumentException("Customer not found");Vehicle v=vd.findById(vehicleId);if(v==null)throw new IllegalArgumentException("Vehicle not found");if(!v.isAvailable())throw new IllegalStateException("Vehicle unavailable");if(rd.conflict(vehicleId,s,e))throw new IllegalStateException("Vehicle already booked for selected dates");int days=(int)ChronoUnit.DAYS.between(s,e)+1;return rd.insert(new Reservation(0,customerId,vehicleId,s,e,v.calculateRentalCost(days),"CONFIRMED"));}
 public void cancel(int id)throws SQLException{rd.cancel(id);System.out.println("Reservation cancelled.");}
 public int pay(int reservationId,double amount,String method)throws SQLException{Reservation r=rd.findById(reservationId);if(r==null)throw new IllegalArgumentException("Reservation not found");if(!"CONFIRMED".equals(r.getStatus()))throw new IllegalStateException("Reservation is not payable");if(amount<r.getTotalCost())throw new IllegalArgumentException("Amount is below total cost: Rs."+r.getTotalCost());String m=method.toUpperCase();if(!List.of("CASH","CARD","UPI","NET_BANKING").contains(m))throw new IllegalArgumentException("Invalid payment method");return pd.insert(new Payment(0,reservationId,amount,m,"PAID"));}
 public void printCustomerReservations(int id)throws SQLException{if(cd.findById(id)==null)throw new IllegalArgumentException("Customer not found");List<Reservation>a=rd.byCustomer(id);if(a.isEmpty())System.out.println("No reservations.");else a.forEach(System.out::println);}
 public void printAllReservations()throws SQLException{List<Reservation>a=rd.all();if(a.isEmpty())System.out.println("No reservations.");else a.forEach(System.out::println);}
 public void printCustomers()throws SQLException{List<Customer>a=cd.findAll();if(a.isEmpty())System.out.println("No customers.");else a.forEach(System.out::println);}
 public int addVehicle(String t,String make,String model,String plate,double rate,Integer range,Integer cc)throws SQLException{t=t.toUpperCase();if(!List.of("CAR","BIKE","ELECTRIC_SCOOTER").contains(t))throw new IllegalArgumentException("Invalid vehicle type");if(rate<=0)throw new IllegalArgumentException("Rate must be positive");return vd.insert(t,make,model,plate,rate,range,cc);}
 public void setAvailability(int id,boolean x)throws SQLException{vd.updateAvailability(id,x);}
 private void dates(LocalDate s,LocalDate e){if(e.isBefore(s))throw new IllegalArgumentException("End date before start date");if(s.isBefore(LocalDate.now()))throw new IllegalArgumentException("Start date is in the past");}
 private void req(String x,String n){if(x==null||x.isBlank())throw new IllegalArgumentException(n+" is required");}
}
