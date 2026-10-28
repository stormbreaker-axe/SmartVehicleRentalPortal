# Smart Vehicle Rental & Reservation Portal

Complete Java 17 + Core OOP + JDBC + MySQL Maven project based on the supplied course specification.

## Included
- Vehicle abstract class
- Car, Bike, ElectricScooter inheritance
- Polymorphic rental pricing
- Customer registration
- Vehicle catalog
- Date-based availability and double-booking prevention
- Reservations and cancellation
- Payments
- Admin fleet availability management
- MySQL persistence through JDBC
- SQL schema + sample fleet
- JUnit pricing tests

## Run
1. Install JDK 17+, Maven 3.8+, MySQL 8+.
2. Run `database/schema.sql` in MySQL.
3. Edit `src/main/resources/db.properties` and set your MySQL password.
4. From the project folder run:
   `mvn clean test`
   `mvn exec:java`
5. Main class: `com.svrp.Main`

## Architecture
Console UI -> Service/OOP layer -> DAO/JDBC -> MySQL.

## Folder structure
SmartVehicleRentalPortal/
- database/schema.sql
- src/main/java/com/svrp/Main.java
- src/main/java/com/svrp/model/
- src/main/java/com/svrp/dao/
- src/main/java/com/svrp/service/
- src/main/java/com/svrp/util/
- src/main/resources/db.properties
- src/test/java/com/svrp/model/VehiclePricingTest.java
- pom.xml
