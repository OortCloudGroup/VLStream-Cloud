import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckAllTables {
    public static void main(String[] args) {
        String url = "jdbc:mysql://192.168.60.77:32443/blade?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8";
        String username = "root";
        String password = "mysql@pass";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE " +
                     "FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = 'blade' AND COLUMN_NAME = 'create_dept'")) {
                
                System.out.println("Tables with create_dept column:");
                while (rs.next()) {
                    System.out.println(String.format("Table: %s | Column: %s | Type: %s",
                        rs.getString("TABLE_NAME"),
                        rs.getString("COLUMN_NAME"),
                        rs.getString("DATA_TYPE")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
