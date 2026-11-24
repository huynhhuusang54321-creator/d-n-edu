package Repository;

import Data.DataConnection;
import Model.HocVienModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class HocVienRepository {
    private static final String COLUMNS = "MaHocVien, MaCoSo, MaHoSo, HoTen, NgaySinh, GioiTinh, AnhDaiDien, TenPhuHuynh, SoDienThoai, Email, DiaChi, MaNguonTiepNhan, MaNguoiTuVan, MaNguoiTao, NgayTao, NgayChinhThuc, TrangThai, GhiChu";
    private static final TableCache<HocVienModel> CACHE = new TableCache<>("HocVien", COLUMNS, "MaHocVien", true, HocVienRepository::map, HocVienRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public HocVienModel getByMaHocVien(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return null;
        return CACHE.byId(maCoSo, ma);
    }
    public List<HocVienModel> getAll(int maCoSo) {
        if (maCoSo <= 0) return new ArrayList<>();
        return CACHE.all(maCoSo);
    }
    public List<HocVienModel> getByMaNguonTiepNhan(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaNguonTiepNhan(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<HocVienModel> getByMaNguoiTuVan(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaNguoiTuVan(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<HocVienModel> getByMaNguoiTao(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaNguoiTao(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<HocVienModel> reload(int maCoSo) { return CACHE.reload(maCoSo); }
    public TableCache.Snapshot selectRaw(int maCoSo) { return CACHE.raw(maCoSo); }
    public TableCache.Snapshot reloadRaw(int maCoSo) { return CACHE.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return CACHE.loaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { CACHE.invalidate(maCoSo); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(HocVienModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.HocVien (MaCoSo, MaHoSo, HoTen, NgaySinh, GioiTinh, AnhDaiDien, TenPhuHuynh, SoDienThoai, Email, DiaChi, MaNguonTiepNhan, MaNguoiTuVan, MaNguoiTao, NgayTao, NgayChinhThuc, TrangThai, GhiChu) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSo(), Types.INTEGER);
            ps.setObject(2, x.getMaHoSo(), Types.VARCHAR);
            ps.setObject(3, x.getHoTen(), Types.NVARCHAR);
            ps.setObject(4, x.getNgaySinh() == null ? null : java.sql.Date.valueOf(x.getNgaySinh()), Types.DATE);
            ps.setObject(5, x.getGioiTinh(), Types.VARCHAR);
            ps.setObject(6, x.getAnhDaiDien(), Types.NVARCHAR);
            ps.setObject(7, x.getTenPhuHuynh(), Types.NVARCHAR);
            ps.setObject(8, x.getSoDienThoai(), Types.VARCHAR);
            ps.setObject(9, x.getEmail(), Types.NVARCHAR);
            ps.setObject(10, x.getDiaChi(), Types.NVARCHAR);
            ps.setObject(11, x.getMaNguonTiepNhan(), Types.INTEGER);
            ps.setObject(12, x.getMaNguoiTuVan(), Types.INTEGER);
            ps.setObject(13, x.getMaNguoiTao(), Types.INTEGER);
            ps.setObject(14, x.getNgayTao() == null ? null : Timestamp.valueOf(x.getNgayTao()), Types.TIMESTAMP);
            ps.setObject(15, x.getNgayChinhThuc() == null ? null : java.sql.Date.valueOf(x.getNgayChinhThuc()), Types.DATE);
            ps.setObject(16, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(17, x.getGhiChu(), Types.NVARCHAR);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(x.getMaCoSo()); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaHocVien(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo(), x.getMaHocVien());
                return true;
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); return false; }
        }
    }
    public boolean update(HocVienModel x) {
        if (x == null || x.getMaHocVien() <= 0 || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "UPDATE dbo.HocVien SET MaHoSo = ?, HoTen = ?, NgaySinh = ?, GioiTinh = ?, AnhDaiDien = ?, TenPhuHuynh = ?, SoDienThoai = ?, Email = ?, DiaChi = ?, MaNguonTiepNhan = ?, MaNguoiTuVan = ?, NgayChinhThuc = ?, TrangThai = ?, GhiChu = ? WHERE MaCoSo = ? AND MaHocVien = ?";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql))) {
            ps.setObject(1, x.getMaHoSo(), Types.VARCHAR);
            ps.setObject(2, x.getHoTen(), Types.NVARCHAR);
            ps.setObject(3, x.getNgaySinh() == null ? null : java.sql.Date.valueOf(x.getNgaySinh()), Types.DATE);
            ps.setObject(4, x.getGioiTinh(), Types.VARCHAR);
            ps.setObject(5, x.getAnhDaiDien(), Types.NVARCHAR);
            ps.setObject(6, x.getTenPhuHuynh(), Types.NVARCHAR);
            ps.setObject(7, x.getSoDienThoai(), Types.VARCHAR);
            ps.setObject(8, x.getEmail(), Types.NVARCHAR);
            ps.setObject(9, x.getDiaChi(), Types.NVARCHAR);
            ps.setObject(10, x.getMaNguonTiepNhan(), Types.INTEGER);
            ps.setObject(11, x.getMaNguoiTuVan(), Types.INTEGER);
            ps.setObject(12, x.getNgayChinhThuc() == null ? null : java.sql.Date.valueOf(x.getNgayChinhThuc()), Types.DATE);
            ps.setObject(13, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(14, x.getGhiChu(), Types.NVARCHAR);
            ps.setInt(15, x.getMaCoSo());
            ps.setInt(16, x.getMaHocVien());
                if (ps.executeUpdate() == 1) {
                    refreshAfterWrite(conn, x.getMaCoSo(), x.getMaHocVien());
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
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, "DELETE FROM dbo.HocVien WHERE MaCoSo = ? AND MaHocVien = ?"))) {
                ps.setInt(1, maCoSo); ps.setInt(2, ma);
                if (ps.executeUpdate() == 1) { CACHE.deleted(maCoSo, ma); return true; }
                CACHE.invalidate(maCoSo);
            } catch (SQLException e) { CACHE.invalidate(maCoSo); logError(e); }
            return false;
        }
    }

    /** Đọc-sửa-ghi trên bản mới từ DB; khóa chỉ có hiệu lực trong JVM này. */
    public HocVienModel modify(int maCoSo, int ma, Predicate<HocVienModel> change) {
        if (change == null || !(maCoSo > 0 && ma > 0)) return null;
        synchronized (LOCK) {
            HocVienModel x = CACHE.fresh(maCoSo, ma);
            if (x == null) return null;
            if (!change.test(x)) return CACHE.fresh(maCoSo, ma);
            if (x.getMaHocVien() != ma || x.getMaCoSo() != maCoSo) return null;
            return update(x) ? (CACHE.loaded(maCoSo) ? getByMaHocVien(maCoSo, ma) : CACHE.fresh(maCoSo, ma)) : null;
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int maCoSo, int ma) { CACHE.afterWrite(conn, maCoSo, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int maCoSo, int ma) { CACHE.invalidate(maCoSo); }

    private static HocVienModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new HocVienModel(
                rs.getInt(columns.get("MaHocVien")),
                rs.getInt(columns.get("MaCoSo")),
                rs.getString(columns.get("MaHoSo")),
                rs.getString(columns.get("HoTen")),
                localDate(rs, columns.get("NgaySinh")),
                rs.getString(columns.get("GioiTinh")),
                rs.getString(columns.get("AnhDaiDien")),
                rs.getString(columns.get("TenPhuHuynh")),
                rs.getString(columns.get("SoDienThoai")),
                rs.getString(columns.get("Email")),
                rs.getString(columns.get("DiaChi")),
                nullableInt(rs, columns.get("MaNguonTiepNhan")),
                nullableInt(rs, columns.get("MaNguoiTuVan")),
                rs.getInt(columns.get("MaNguoiTao")),
                localDateTime(rs, columns.get("NgayTao")),
                localDate(rs, columns.get("NgayChinhThuc")),
                rs.getString(columns.get("TrangThai")),
                rs.getString(columns.get("GhiChu")));
    }
    private static HocVienModel copy(HocVienModel x) { return new HocVienModel(x.getMaHocVien(), x.getMaCoSo(), x.getMaHoSo(), x.getHoTen(), x.getNgaySinh(), x.getGioiTinh(), x.getAnhDaiDien(), x.getTenPhuHuynh(), x.getSoDienThoai(), x.getEmail(), x.getDiaChi(), x.getMaNguonTiepNhan(), x.getMaNguoiTuVan(), x.getMaNguoiTao(), x.getNgayTao(), x.getNgayChinhThuc(), x.getTrangThai(), x.getGhiChu()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[HocVienRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
