Smart Vehicle Rental & Reservation Portal — Plain Java + JDBC

No Maven or Gradle is required.

Requirements: JDK 17+, MySQL 8+, Eclipse/IntelliJ/NetBeans, MySQL Connector/J.

1. Download the official MySQL Connector/J JAR and put it in lib/mysql-connector-j.jar.
2. Run database/schema.sql in MySQL.
3. Edit src/resources/db.properties with your MySQL username/password.
4. Eclipse: import this folder as an existing Eclipse project. The included .classpath references lib/mysql-connector-j.jar. Run com.svrp.Main.
5. IntelliJ: open the folder, add lib/mysql-connector-j.jar as a library, mark src as Sources Root and src/resources as Resources Root, then run com.svrp.Main.

Windows command line:
javac -cp "lib\\mysql-connector-j.jar" -d out src\\com\\svrp\\*.java src\\com\\svrp\\model\\*.java src\\com\\svrp\\dao\\*.java src\\com\\svrp\\service\\*.java src\\com\\svrp\\util\\*.java
java -cp "out;lib\\mysql-connector-j.jar;src\\resources" com.svrp.Main

Linux/macOS:
javac -cp "lib/mysql-connector-j.jar" -d out src/com/svrp/*.java src/com/svrp/model/*.java src/com/svrp/dao/*.java src/com/svrp/service/*.java src/com/svrp/util/*.java
java -cp "out:lib/mysql-connector-j.jar:src/resources" com.svrp.Main
