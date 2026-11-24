package Repository;

import Data.DataConnection;
import Model.DiemDanhModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class DiemDanhRepository {
    private static final String COLUMNS = "MaDiemDanh, MaCoSo, MaHocVien, MaBuoiHoc, MaDangKy, LoaiThamGia, TrangThai, TinhBuoi, MaDiemDanhNguon, MaNguoiCapNhat, NgayCapNhat, GhiChu";
    private static final TableCache<DiemDanhModel> CACHE = new TableCache<>("DiemDanh", COLUMNS, "MaDiemDanh", true, DiemDanhRepository::map, DiemDanhRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public DiemDanhModel getByMaDiemDanh(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return null;
        return CACHE.byId(maCoSo, ma);
    }
    public List<DiemDanhModel> getAll(int maCoSo) {
        if (maCoSo <= 0) return new ArrayList<>();
        return CACHE.all(maCoSo);
    }
    public List<DiemDanhModel> getByMaHocVien(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaHocVien(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DiemDanhModel> getByMaBuoiHoc(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaBuoiHoc(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DiemDanhModel> getByMaDangKy(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaDangKy(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DiemDanhModel> getByMaDiemDanhNguon(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaDiemDanhNguon(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DiemDanhModel> getByMaNguoiCapNhat(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaNguoiCapNhat(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DiemDanhModel> reload(int maCoSo) { return CACHE.reload(maCoSo); }
    public TableCache.Snapshot selectRaw(int maCoSo) { return CACHE.raw(maCoSo); }
    public TableCache.Snapshot reloadRaw(int maCoSo) { return CACHE.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return CACHE.loaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { CACHE.invalidate(maCoSo); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(DiemDanhModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.DiemDanh (MaCoSo, MaHocVien, MaBuoiHoc, MaDangKy, LoaiThamGia, TrangThai, TinhBuoi, MaDiemDanhNguon, MaNguoiCapNhat, NgayCapNhat, GhiChu) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSo(), Types.INTEGER);
            ps.setObject(2, x.getMaHocVien(), Types.INTEGER);
            ps.setObject(3, x.getMaBuoiHoc(), Types.INTEGER);
            ps.setObject(4, x.getMaDangKy(), Types.INTEGER);
            ps.setObject(5, x.getLoaiThamGia(), Types.VARCHAR);
            ps.setObject(6, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(7, x.getTinhBuoi(), Types.BIT);
            ps.setObject(8, x.getMaDiemDanhNguon(), Types.INTEGER);
            ps.setObject(9, x.getMaNguoiCapNhat(), Types.INTEGER);
            ps.setObject(10, x.getNgayCapNhat() == null ? null : Timestamp.valueOf(x.getNgayCapNhat()), Types.TIMESTAMP);
            ps.setObject(11, x.getGhiChu(), Types.NVARCHAR);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(x.getMaCoSo()); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaDiemDanh(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo(), x.getMaDiemDanh());
                return true;
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); return false; }
        }
    }
    public boolean update(DiemDanhModel x) {
        if (x == null || x.getMaDiemDanh() <= 0 || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "UPDATE dbo.DiemDanh SET MaHocVien = ?, MaBuoiHoc = ?, MaDangKy = ?, LoaiThamGia = ?, TrangThai = ?, TinhBuoi = ?, MaDiemDanhNguon = ?, MaNguoiCapNhat = ?, NgayCapNhat = ?, GhiChu = ? WHERE MaCoSo = ? AND MaDiemDanh = ?";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql))) {
            ps.setObject(1, x.getMaHocVien(), Types.INTEGER);
            ps.setObject(2, x.getMaBuoiHoc(), Types.INTEGER);
            ps.setObject(3, x.getMaDangKy(), Types.INTEGER);
            ps.setObject(4, x.getLoaiThamGia(), Types.VARCHAR);
            ps.setObject(5, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(6, x.getTinhBuoi(), Types.BIT);
            ps.setObject(7, x.getMaDiemDanhNguon(), Types.INTEGER);
            ps.setObject(8, x.getMaNguoiCapNhat(), Types.INTEGER);
            ps.setObject(9, x.getNgayCapNhat() == null ? null : Timestamp.valueOf(x.getNgayCapNhat()), Types.TIMESTAMP);
            ps.setObject(10, x.getGhiChu(), Types.NVARCHAR);
            ps.setInt(11, x.getMaCoSo());
            ps.setInt(12, x.getMaDiemDanh());
                if (ps.executeUpdate() == 1) {
                    refreshAfterWrite(conn, x.getMaCoSo(), x.getMaDiemDanh());
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
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, "DELETE FROM dbo.DiemDanh WHERE MaCoSo = ? AND MaDiemDanh = ?"))) {
                ps.setInt(1, maCoSo); ps.setInt(2, ma);
                if (ps.executeUpdate() == 1) { CACHE.deleted(maCoSo, ma); return true; }
                CACHE.invalidate(maCoSo);
            } catch (SQLException e) { CACHE.invalidate(maCoSo); logError(e); }
            return false;
        }
    }

    /** Đọc-sửa-ghi trên bản mới từ DB; khóa chỉ có hiệu lực trong JVM này. */
    public DiemDanhModel modify(int maCoSo, int ma, Predicate<DiemDanhModel> change) {
        if (change == null || !(maCoSo > 0 && ma > 0)) return null;
        synchronized (LOCK) {
            DiemDanhModel x = CACHE.fresh(maCoSo, ma);
            if (x == null) return null;
            if (!change.test(x)) return CACHE.fresh(maCoSo, ma);
            if (x.getMaDiemDanh() != ma || x.getMaCoSo() != maCoSo) return null;
            return update(x) ? (CACHE.loaded(maCoSo) ? getByMaDiemDanh(maCoSo, ma) : CACHE.fresh(maCoSo, ma)) : null;
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int maCoSo, int ma) { CACHE.afterWrite(conn, maCoSo, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int maCoSo, int ma) { CACHE.invalidate(maCoSo); }

    private static DiemDanhModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new DiemDanhModel(
                rs.getInt(columns.get("MaDiemDanh")),
                rs.getInt(columns.get("MaCoSo")),
                rs.getInt(columns.get("MaHocVien")),
                rs.getInt(columns.get("MaBuoiHoc")),
                nullableInt(rs, columns.get("MaDangKy")),
                rs.getString(columns.get("LoaiThamGia")),
                rs.getString(columns.get("TrangThai")),
                rs.getBoolean(columns.get("TinhBuoi")),
                nullableInt(rs, columns.get("MaDiemDanhNguon")),
                rs.getInt(columns.get("MaNguoiCapNhat")),
                localDateTime(rs, columns.get("NgayCapNhat")),
                rs.getString(columns.get("GhiChu")));
    }
    private static DiemDanhModel copy(DiemDanhModel x) { return new DiemDanhModel(x.getMaDiemDanh(), x.getMaCoSo(), x.getMaHocVien(), x.getMaBuoiHoc(), x.getMaDangKy(), x.getLoaiThamGia(), x.getTrangThai(), x.getTinhBuoi(), x.getMaDiemDanhNguon(), x.getMaNguoiCapNhat(), x.getNgayCapNhat(), x.getGhiChu()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[DiemDanhRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
