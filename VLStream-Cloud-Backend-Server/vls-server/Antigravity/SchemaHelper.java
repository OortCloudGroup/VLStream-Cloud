import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SchemaHelper {
    public static void main(String[] args) {
        String url = "jdbc:mysql://192.168.60.77:32443/blade?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8";
        String username = "root";
        String password = "mysql@pass";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("DESCRIBE vls_device_info")) {
                
                System.out.println("vls_device_info schema:");
                while (rs.next()) {
                    System.out.println(String.format("Field: %s | Type: %s | Null: %s | Key: %s | Default: %s | Extra: %s",
                        rs.getString("Field"),
                        rs.getString("Type"),
                        rs.getString("Null"),
                        rs.getString("Key"),
                        rs.getString("Default"),
                        rs.getString("Extra")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
