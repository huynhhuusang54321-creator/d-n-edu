package vn.vinabacus.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Tien ich JDBC dung chung cho cac Repository sau nay.
 * Khong tu ket noi khi khoi dong giao dien va khong luu mat khau trong code.
 * Can driver Microsoft JDBC SQL Server trong Maven/classpath khi su dung.
 */
public final class Database {
    private Database() {}
    public static Connection moKetNoi() throws SQLException {
        Path file = Path.of("db.local.properties");
        if (!Files.isRegularFile(file)) {
            throw new SQLException("Chưa có db.local.properties (sao chép từ db.local.properties.example)");
        }
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            props.load(input);
        } catch (IOException ex) {
            throw new SQLException("Không đọc được cấu hình SQL Server", ex);
        }
        String url = props.getProperty("db.url", "").trim();
        String user = props.getProperty("db.user", "").trim();
        String password = props.getProperty("db.password", "");
        if (url.isBlank()) throw new SQLException("db.url chưa được cấu hình");
        return DriverManager.getConnection(url, user, password);
    }
}
