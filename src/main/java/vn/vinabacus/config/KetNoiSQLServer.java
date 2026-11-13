
package vn.vinabacus.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class KetNoiSQLServer {

    private static final String URL =
        "jdbc:sqlserver://localhost:1433;"
        + "databaseName=VINABACUS;"
        + "encrypt=true;"
        + "trustServerCertificate=true;";

    private static final String USER = "sa";

    private static final String PASSWORD = "sang123";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                URL, USER, PASSWORD
            );
        } catch (SQLException e) {
            System.out.println("Loi ket noi: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("Ket noi SQL Server thanh cong!");
                System.out.println("Database: " + conn.getCatalog());
            } else {
                System.out.println("Ket noi that bai!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
