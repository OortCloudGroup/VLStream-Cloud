import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * DbHelper utility to query users from the database
 */
public class DbHelper {
    public static void main(String[] args) {
        String url = "jdbc:mysql://192.168.60.77:32443/blade?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8";
        String username = "root";
        String password = "mysql@pass";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, client_id, client_secret, status, is_deleted FROM blade_client")) {
                
                System.out.println("Querying blade_client table:");
                while (rs.next()) {
                    System.out.println(String.format("ID: %s | ClientID: %s | ClientSecret: %s | Status: %d | IsDeleted: %d",
                        rs.getString("id"),
                        rs.getString("client_id"),
                        rs.getString("client_secret"),
                        rs.getInt("status"),
                        rs.getInt("is_deleted")
                    ));
                }
            }
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, tenant_id, tenant_name, status, is_deleted FROM blade_tenant")) {
                
                System.out.println("Querying blade_tenant table:");
                while (rs.next()) {
                    System.out.println(String.format("ID: %s | TenantID: %s | TenantName: %s | Status: %d | IsDeleted: %d",
                        rs.getString("id"),
                        rs.getString("tenant_id"),
                        rs.getString("tenant_name"),
                        rs.getInt("status"),
                        rs.getInt("is_deleted")
                    ));
                }
            }
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, tenant_id, account, password, status, is_deleted FROM blade_user")) {
                
                System.out.println("Querying blade_user table:");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.println(String.format("ID: %s | TenantID: %s | Account: %s | PasswordHash: %s | Status: %d | IsDeleted: %d",
                        rs.getString("id"),
                        rs.getString("tenant_id"),
                        rs.getString("account"),
                        rs.getString("password"),
                        rs.getInt("status"),
                        rs.getInt("is_deleted")
                    ));
                }
                if (!found) {
                    System.out.println("No users found in blade_user table.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
