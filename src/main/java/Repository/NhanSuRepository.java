package Repository;

import Data.DataConnection;
import Model.NhanSuModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class NhanSuRepository {
    private static final String COLUMNS = "MaNhanSu, MaCoSo, HoTen, NgaySinh, GioiTinh, SoDienThoai, Email, DiaChi, AnhDaiDien, TenDangNhap, MatKhauBam, VaiTro, ChoPhepDangNhap, TrangThai, NgayTao";
    private static final TableCache<NhanSuModel> CACHE = new TableCache<>("NhanSu", COLUMNS, "MaNhanSu", true, NhanSuRepository::map, NhanSuRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public NhanSuModel getByMaNhanSu(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return null;
        return CACHE.byId(maCoSo, ma);
    }
    public List<NhanSuModel> getAll(int maCoSo) {
        if (maCoSo <= 0) return new ArrayList<>();
        return CACHE.all(maCoSo);
    }
    public List<NhanSuModel> getByTenDangNhap(int maCoSo, String value) {
        return getAll(maCoSo).stream().filter(x -> x.getTenDangNhap() != null && value != null && x.getTenDangNhap().trim().equalsIgnoreCase(value.trim())).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<NhanSuModel> reload(int maCoSo) { return CACHE.reload(maCoSo); }
    public TableCache.Snapshot selectRaw(int maCoSo) { return CACHE.raw(maCoSo); }
    public TableCache.Snapshot reloadRaw(int maCoSo) { return CACHE.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return CACHE.loaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { CACHE.invalidate(maCoSo); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(NhanSuModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.NhanSu (MaCoSo, HoTen, NgaySinh, GioiTinh, SoDienThoai, Email, DiaChi, AnhDaiDien, TenDangNhap, MatKhauBam, VaiTro, ChoPhepDangNhap, TrangThai, NgayTao) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSo(), Types.INTEGER);
            ps.setObject(2, x.getHoTen(), Types.NVARCHAR);
            ps.setObject(3, x.getNgaySinh() == null ? null : java.sql.Date.valueOf(x.getNgaySinh()), Types.DATE);
            ps.setObject(4, x.getGioiTinh(), Types.VARCHAR);
            ps.setObject(5, x.getSoDienThoai(), Types.VARCHAR);
            ps.setObject(6, x.getEmail(), Types.NVARCHAR);
            ps.setObject(7, x.getDiaChi(), Types.NVARCHAR);
            ps.setObject(8, x.getAnhDaiDien(), Types.NVARCHAR);
            ps.setObject(9, x.getTenDangNhap(), Types.NVARCHAR);
            ps.setObject(10, x.getMatKhauBam(), Types.NVARCHAR);
            ps.setObject(11, x.getVaiTro(), Types.VARCHAR);
            ps.setObject(12, x.getChoPhepDangNhap(), Types.BIT);
            ps.setObject(13, x.getTrangThai(), Types.BIT);
            ps.setObject(14, x.getNgayTao() == null ? null : Timestamp.valueOf(x.getNgayTao()), Types.TIMESTAMP);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(x.getMaCoSo()); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaNhanSu(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo(), x.getMaNhanSu());
                return true;
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); return false; }
        }
    }
    public boolean update(NhanSuModel x) {
        if (x == null || x.getMaNhanSu() <= 0 || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "UPDATE dbo.NhanSu SET HoTen = ?, NgaySinh = ?, GioiTinh = ?, SoDienThoai = ?, Email = ?, DiaChi = ?, AnhDaiDien = ?, TenDangNhap = ?, MatKhauBam = ?, VaiTro = ?, ChoPhepDangNhap = ?, TrangThai = ? WHERE MaCoSo = ? AND MaNhanSu = ?";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql))) {
            ps.setObject(1, x.getHoTen(), Types.NVARCHAR);
            ps.setObject(2, x.getNgaySinh() == null ? null : java.sql.Date.valueOf(x.getNgaySinh()), Types.DATE);
            ps.setObject(3, x.getGioiTinh(), Types.VARCHAR);
            ps.setObject(4, x.getSoDienThoai(), Types.VARCHAR);
            ps.setObject(5, x.getEmail(), Types.NVARCHAR);
            ps.setObject(6, x.getDiaChi(), Types.NVARCHAR);
            ps.setObject(7, x.getAnhDaiDien(), Types.NVARCHAR);
            ps.setObject(8, x.getTenDangNhap(), Types.NVARCHAR);
            ps.setObject(9, x.getMatKhauBam(), Types.NVARCHAR);
            ps.setObject(10, x.getVaiTro(), Types.VARCHAR);
            ps.setObject(11, x.getChoPhepDangNhap(), Types.BIT);
            ps.setObject(12, x.getTrangThai(), Types.BIT);
            ps.setInt(13, x.getMaCoSo());
            ps.setInt(14, x.getMaNhanSu());
                if (ps.executeUpdate() == 1) {
                    refreshAfterWrite(conn, x.getMaCoSo(), x.getMaNhanSu());
                    return true;
                }
                CACHE.invalidate(x.getMaCoSo());
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); }
            return false;
        }
    }

    public boolean delete(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return false;
        synchronized (LOCK) {
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, "DELETE FROM dbo.NhanSu WHERE MaCoSo = ? AND MaNhanSu = ?"))) {
                ps.setInt(1, maCoSo); ps.setInt(2, ma);
                if (ps.executeUpdate() == 1) { CACHE.deleted(maCoSo, ma); return true; }
                CACHE.invalidate(maCoSo);
            } catch (SQLException e) { CACHE.invalidate(maCoSo); logError(e); }
            return false;
        }
    }

    /** Đọc-sửa-ghi trên bản mới từ DB; khóa chỉ có hiệu lực trong JVM này. */
    public NhanSuModel modify(int maCoSo, int ma, Predicate<NhanSuModel> change) {
        if (change == null || !(maCoSo > 0 && ma > 0)) return null;
        synchronized (LOCK) {
            NhanSuModel x = CACHE.fresh(maCoSo, ma);
            if (x == null) return null;
            if (!change.test(x)) return CACHE.fresh(maCoSo, ma);
            if (x.getMaNhanSu() != ma || x.getMaCoSo() != maCoSo) return null;
            return update(x) ? (CACHE.loaded(maCoSo) ? getByMaNhanSu(maCoSo, ma) : CACHE.fresh(maCoSo, ma)) : null;
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int maCoSo, int ma) { CACHE.afterWrite(conn, maCoSo, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int maCoSo, int ma) { CACHE.invalidate(maCoSo); }

    private static NhanSuModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new NhanSuModel(
                rs.getInt(columns.get("MaNhanSu")),
                rs.getInt(columns.get("MaCoSo")),
                rs.getString(columns.get("HoTen")),
                localDate(rs, columns.get("NgaySinh")),
                rs.getString(columns.get("GioiTinh")),
                rs.getString(columns.get("SoDienThoai")),
                rs.getString(columns.get("Email")),
                rs.getString(columns.get("DiaChi")),
                rs.getString(columns.get("AnhDaiDien")),
                rs.getString(columns.get("TenDangNhap")),
                rs.getString(columns.get("MatKhauBam")),
                rs.getString(columns.get("VaiTro")),
                rs.getBoolean(columns.get("ChoPhepDangNhap")),
                rs.getBoolean(columns.get("TrangThai")),
                localDateTime(rs, columns.get("NgayTao")));
    }
    private static NhanSuModel copy(NhanSuModel x) { return new NhanSuModel(x.getMaNhanSu(), x.getMaCoSo(), x.getHoTen(), x.getNgaySinh(), x.getGioiTinh(), x.getSoDienThoai(), x.getEmail(), x.getDiaChi(), x.getAnhDaiDien(), x.getTenDangNhap(), x.getMatKhauBam(), x.getVaiTro(), x.getChoPhepDangNhap(), x.getTrangThai(), x.getNgayTao()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[NhanSuRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
