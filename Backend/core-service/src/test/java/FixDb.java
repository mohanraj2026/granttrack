import java.sql.*;
public class FixDb {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/granttrack", "root", "root")) {
            c.createStatement().execute("DELETE FROM flyway_schema_history WHERE version = '6'");
            System.out.println("Deleted version 6!");
        }
    }
}
