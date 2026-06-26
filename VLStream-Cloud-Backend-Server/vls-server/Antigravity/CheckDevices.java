import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDevices {
    public static void main(String[] args) {
        String url = "jdbc:mysql://192.168.60.77:32443/blade?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8";
        String username = "root";
        String password = "mysql@pass";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, device_name, create_dept FROM vls_device_info")) {
                
                System.out.println("vls_device_info records:");
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.println(String.format("ID: %d | Name: %s | CreateDept: %s",
                        rs.getLong("id"),
                        rs.getString("device_name"),
                        rs.getString("create_dept")
                    ));
                }
                System.out.println("Total records: " + count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
