
package vn.vinabacus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import vn.vinabacus.config.Database;

public class KiemTraKetNoi {

    public static void main(String[] args) {

        System.out.println("Dang kiem tra ket noi SQL Server...");

        String sql = "SELECT MaCoSo, LoaiCoSo, TenCoSo FROM dbo.CoSo";

        try (Connection conn = Database.moKetNoi();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("KET NOI SQL SERVER THANH CONG!");
            System.out.println("-----------------------------");

            while (rs.next()) {
                System.out.println(
                    rs.getInt("MaCoSo") + " | " +
                    rs.getString("LoaiCoSo") + " | " +
                    rs.getString("TenCoSo")
                );
            }

        } catch (Exception e) {
            System.out.println("KET NOI HOAC TRUY VAN THAT BAI!");
            e.printStackTrace();
        }
    }
}
