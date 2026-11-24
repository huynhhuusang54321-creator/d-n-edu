package Repository;

import Data.DataConnection;
import Model.BuoiHocModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class BuoiHocRepository {
    private static final String COLUMNS = "MaBuoiHoc, MaCoSo, MaLop, MaThoiKhoaBieu, NgayHoc, MaCaHoc, GioBatDau, GioKetThuc, MaGiaoVien, MaNguoiKichHoat, TrangThai, NgayKichHoat, GhiChu";
    private static final TableCache<BuoiHocModel> CACHE = new TableCache<>("BuoiHoc", COLUMNS, "MaBuoiHoc", true, BuoiHocRepository::map, BuoiHocRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public BuoiHocModel getByMaBuoiHoc(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return null;
        return CACHE.byId(maCoSo, ma);
    }
    public List<BuoiHocModel> getAll(int maCoSo) {
        if (maCoSo <= 0) return new ArrayList<>();
        return CACHE.all(maCoSo);
    }
    public List<BuoiHocModel> getByMaLop(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaLop(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<BuoiHocModel> getByMaThoiKhoaBieu(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaThoiKhoaBieu(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<BuoiHocModel> getByMaCaHoc(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaCaHoc(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<BuoiHocModel> getByMaGiaoVien(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaGiaoVien(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<BuoiHocModel> getByMaNguoiKichHoat(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaNguoiKichHoat(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<BuoiHocModel> reload(int maCoSo) { return CACHE.reload(maCoSo); }
    public TableCache.Snapshot selectRaw(int maCoSo) { return CACHE.raw(maCoSo); }
    public TableCache.Snapshot reloadRaw(int maCoSo) { return CACHE.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return CACHE.loaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { CACHE.invalidate(maCoSo); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(BuoiHocModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.BuoiHoc (MaCoSo, MaLop, MaThoiKhoaBieu, NgayHoc, MaCaHoc, GioBatDau, GioKetThuc, MaGiaoVien, MaNguoiKichHoat, TrangThai, NgayKichHoat, GhiChu) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSo(), Types.INTEGER);
            ps.setObject(2, x.getMaLop(), Types.INTEGER);
            ps.setObject(3, x.getMaThoiKhoaBieu(), Types.INTEGER);
            ps.setObject(4, x.getNgayHoc() == null ? null : java.sql.Date.valueOf(x.getNgayHoc()), Types.DATE);
            ps.setObject(5, x.getMaCaHoc(), Types.INTEGER);
            ps.setObject(6, x.getGioBatDau() == null ? null : Time.valueOf(x.getGioBatDau()), Types.TIME);
            ps.setObject(7, x.getGioKetThuc() == null ? null : Time.valueOf(x.getGioKetThuc()), Types.TIME);
            ps.setObject(8, x.getMaGiaoVien(), Types.INTEGER);
            ps.setObject(9, x.getMaNguoiKichHoat(), Types.INTEGER);
            ps.setObject(10, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(11, x.getNgayKichHoat() == null ? null : Timestamp.valueOf(x.getNgayKichHoat()), Types.TIMESTAMP);
            ps.setObject(12, x.getGhiChu(), Types.NVARCHAR);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(x.getMaCoSo()); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaBuoiHoc(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo(), x.getMaBuoiHoc());
                return true;
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); return false; }
        }
    }
    public boolean update(BuoiHocModel x) {
        if (x == null || x.getMaBuoiHoc() <= 0 || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "UPDATE dbo.BuoiHoc SET MaLop = ?, MaThoiKhoaBieu = ?, NgayHoc = ?, MaCaHoc = ?, GioBatDau = ?, GioKetThuc = ?, MaGiaoVien = ?, MaNguoiKichHoat = ?, TrangThai = ?, NgayKichHoat = ?, GhiChu = ? WHERE MaCoSo = ? AND MaBuoiHoc = ?";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql))) {
            ps.setObject(1, x.getMaLop(), Types.INTEGER);
            ps.setObject(2, x.getMaThoiKhoaBieu(), Types.INTEGER);
            ps.setObject(3, x.getNgayHoc() == null ? null : java.sql.Date.valueOf(x.getNgayHoc()), Types.DATE);
            ps.setObject(4, x.getMaCaHoc(), Types.INTEGER);
            ps.setObject(5, x.getGioBatDau() == null ? null : Time.valueOf(x.getGioBatDau()), Types.TIME);
            ps.setObject(6, x.getGioKetThuc() == null ? null : Time.valueOf(x.getGioKetThuc()), Types.TIME);
            ps.setObject(7, x.getMaGiaoVien(), Types.INTEGER);
            ps.setObject(8, x.getMaNguoiKichHoat(), Types.INTEGER);
            ps.setObject(9, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(10, x.getNgayKichHoat() == null ? null : Timestamp.valueOf(x.getNgayKichHoat()), Types.TIMESTAMP);
            ps.setObject(11, x.getGhiChu(), Types.NVARCHAR);
            ps.setInt(12, x.getMaCoSo());
            ps.setInt(13, x.getMaBuoiHoc());
                if (ps.executeUpdate() == 1) {
                    refreshAfterWrite(conn, x.getMaCoSo(), x.getMaBuoiHoc());
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
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, "DELETE FROM dbo.BuoiHoc WHERE MaCoSo = ? AND MaBuoiHoc = ?"))) {
                ps.setInt(1, maCoSo); ps.setInt(2, ma);
                if (ps.executeUpdate() == 1) { CACHE.deleted(maCoSo, ma); return true; }
                CACHE.invalidate(maCoSo);
            } catch (SQLException e) { CACHE.invalidate(maCoSo); logError(e); }
            return false;
        }
    }

    /** Đọc-sửa-ghi trên bản mới từ DB; khóa chỉ có hiệu lực trong JVM này. */
    public BuoiHocModel modify(int maCoSo, int ma, Predicate<BuoiHocModel> change) {
        if (change == null || !(maCoSo > 0 && ma > 0)) return null;
        synchronized (LOCK) {
            BuoiHocModel x = CACHE.fresh(maCoSo, ma);
            if (x == null) return null;
            if (!change.test(x)) return CACHE.fresh(maCoSo, ma);
            if (x.getMaBuoiHoc() != ma || x.getMaCoSo() != maCoSo) return null;
            return update(x) ? (CACHE.loaded(maCoSo) ? getByMaBuoiHoc(maCoSo, ma) : CACHE.fresh(maCoSo, ma)) : null;
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int maCoSo, int ma) { CACHE.afterWrite(conn, maCoSo, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int maCoSo, int ma) { CACHE.invalidate(maCoSo); }

    private static BuoiHocModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new BuoiHocModel(
                rs.getInt(columns.get("MaBuoiHoc")),
                rs.getInt(columns.get("MaCoSo")),
                rs.getInt(columns.get("MaLop")),
                nullableInt(rs, columns.get("MaThoiKhoaBieu")),
                localDate(rs, columns.get("NgayHoc")),
                rs.getInt(columns.get("MaCaHoc")),
                localTime(rs, columns.get("GioBatDau")),
                localTime(rs, columns.get("GioKetThuc")),
                nullableInt(rs, columns.get("MaGiaoVien")),
                nullableInt(rs, columns.get("MaNguoiKichHoat")),
                rs.getString(columns.get("TrangThai")),
                localDateTime(rs, columns.get("NgayKichHoat")),
                rs.getString(columns.get("GhiChu")));
    }
    private static BuoiHocModel copy(BuoiHocModel x) { return new BuoiHocModel(x.getMaBuoiHoc(), x.getMaCoSo(), x.getMaLop(), x.getMaThoiKhoaBieu(), x.getNgayHoc(), x.getMaCaHoc(), x.getGioBatDau(), x.getGioKetThuc(), x.getMaGiaoVien(), x.getMaNguoiKichHoat(), x.getTrangThai(), x.getNgayKichHoat(), x.getGhiChu()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[BuoiHocRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
