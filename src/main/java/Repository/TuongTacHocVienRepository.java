package Repository;

import Data.DataConnection;
import Model.TuongTacHocVienModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class TuongTacHocVienRepository {
    private static final String COLUMNS = "MaTuongTac, MaCoSo, MaHocVien, MaNhanSu, LoaiTuongTac, MaKhoaHocMongMuon, ThoiGianMongMuon, NoiDung, KetQua, TrangThaiTruoc, TrangThaiSau, NgayHenTiep, NgayTao";
    private static final TableCache<TuongTacHocVienModel> CACHE = new TableCache<>("TuongTacHocVien", COLUMNS, "MaTuongTac", true, TuongTacHocVienRepository::map, TuongTacHocVienRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public TuongTacHocVienModel getByMaTuongTac(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return null;
        return CACHE.byId(maCoSo, ma);
    }
    public List<TuongTacHocVienModel> getAll(int maCoSo) {
        if (maCoSo <= 0) return new ArrayList<>();
        return CACHE.all(maCoSo);
    }
    public List<TuongTacHocVienModel> getByMaHocVien(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaHocVien(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<TuongTacHocVienModel> getByMaNhanSu(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaNhanSu(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<TuongTacHocVienModel> getByMaKhoaHocMongMuon(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaKhoaHocMongMuon(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<TuongTacHocVienModel> reload(int maCoSo) { return CACHE.reload(maCoSo); }
    public TableCache.Snapshot selectRaw(int maCoSo) { return CACHE.raw(maCoSo); }
    public TableCache.Snapshot reloadRaw(int maCoSo) { return CACHE.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return CACHE.loaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { CACHE.invalidate(maCoSo); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(TuongTacHocVienModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.TuongTacHocVien (MaCoSo, MaHocVien, MaNhanSu, LoaiTuongTac, MaKhoaHocMongMuon, ThoiGianMongMuon, NoiDung, KetQua, TrangThaiTruoc, TrangThaiSau, NgayHenTiep, NgayTao) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSo(), Types.INTEGER);
            ps.setObject(2, x.getMaHocVien(), Types.INTEGER);
            ps.setObject(3, x.getMaNhanSu(), Types.INTEGER);
            ps.setObject(4, x.getLoaiTuongTac(), Types.VARCHAR);
            ps.setObject(5, x.getMaKhoaHocMongMuon(), Types.INTEGER);
            ps.setObject(6, x.getThoiGianMongMuon(), Types.NVARCHAR);
            ps.setObject(7, x.getNoiDung(), Types.NVARCHAR);
            ps.setObject(8, x.getKetQua(), Types.NVARCHAR);
            ps.setObject(9, x.getTrangThaiTruoc(), Types.VARCHAR);
            ps.setObject(10, x.getTrangThaiSau(), Types.VARCHAR);
            ps.setObject(11, x.getNgayHenTiep() == null ? null : Timestamp.valueOf(x.getNgayHenTiep()), Types.TIMESTAMP);
            ps.setObject(12, x.getNgayTao() == null ? null : Timestamp.valueOf(x.getNgayTao()), Types.TIMESTAMP);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(x.getMaCoSo()); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaTuongTac(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo(), x.getMaTuongTac());
                return true;
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); return false; }
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int maCoSo, int ma) { CACHE.afterWrite(conn, maCoSo, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int maCoSo, int ma) { CACHE.invalidate(maCoSo); }

    private static TuongTacHocVienModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new TuongTacHocVienModel(
                rs.getInt(columns.get("MaTuongTac")),
                rs.getInt(columns.get("MaCoSo")),
                rs.getInt(columns.get("MaHocVien")),
                rs.getInt(columns.get("MaNhanSu")),
                rs.getString(columns.get("LoaiTuongTac")),
                nullableInt(rs, columns.get("MaKhoaHocMongMuon")),
                rs.getString(columns.get("ThoiGianMongMuon")),
                rs.getString(columns.get("NoiDung")),
                rs.getString(columns.get("KetQua")),
                rs.getString(columns.get("TrangThaiTruoc")),
                rs.getString(columns.get("TrangThaiSau")),
                localDateTime(rs, columns.get("NgayHenTiep")),
                localDateTime(rs, columns.get("NgayTao")));
    }
    private static TuongTacHocVienModel copy(TuongTacHocVienModel x) { return new TuongTacHocVienModel(x.getMaTuongTac(), x.getMaCoSo(), x.getMaHocVien(), x.getMaNhanSu(), x.getLoaiTuongTac(), x.getMaKhoaHocMongMuon(), x.getThoiGianMongMuon(), x.getNoiDung(), x.getKetQua(), x.getTrangThaiTruoc(), x.getTrangThaiSau(), x.getNgayHenTiep(), x.getNgayTao()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[TuongTacHocVienRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
