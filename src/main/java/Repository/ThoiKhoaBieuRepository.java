package Repository;

import Data.DataConnection;
import Model.ThoiKhoaBieuModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class ThoiKhoaBieuRepository {
    private static final String COLUMNS = "MaThoiKhoaBieu, MaCoSo, MaLop, ThuTrongTuan, MaCaHoc, TuNgay, DenNgay, TrangThai";
    private static final TableCache<ThoiKhoaBieuModel> CACHE = new TableCache<>("ThoiKhoaBieu", COLUMNS, "MaThoiKhoaBieu", true, ThoiKhoaBieuRepository::map, ThoiKhoaBieuRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public ThoiKhoaBieuModel getByMaThoiKhoaBieu(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return null;
        return CACHE.byId(maCoSo, ma);
    }
    public List<ThoiKhoaBieuModel> getAll(int maCoSo) {
        if (maCoSo <= 0) return new ArrayList<>();
        return CACHE.all(maCoSo);
    }
    public List<ThoiKhoaBieuModel> getByMaLop(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaLop(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<ThoiKhoaBieuModel> getByMaCaHoc(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaCaHoc(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<ThoiKhoaBieuModel> reload(int maCoSo) { return CACHE.reload(maCoSo); }
    public TableCache.Snapshot selectRaw(int maCoSo) { return CACHE.raw(maCoSo); }
    public TableCache.Snapshot reloadRaw(int maCoSo) { return CACHE.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return CACHE.loaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { CACHE.invalidate(maCoSo); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(ThoiKhoaBieuModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.ThoiKhoaBieu (MaCoSo, MaLop, ThuTrongTuan, MaCaHoc, TuNgay, DenNgay, TrangThai) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSo(), Types.INTEGER);
            ps.setObject(2, x.getMaLop(), Types.INTEGER);
            ps.setObject(3, x.getThuTrongTuan(), Types.TINYINT);
            ps.setObject(4, x.getMaCaHoc(), Types.INTEGER);
            ps.setObject(5, x.getTuNgay() == null ? null : java.sql.Date.valueOf(x.getTuNgay()), Types.DATE);
            ps.setObject(6, x.getDenNgay() == null ? null : java.sql.Date.valueOf(x.getDenNgay()), Types.DATE);
            ps.setObject(7, x.getTrangThai(), Types.BIT);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(x.getMaCoSo()); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaThoiKhoaBieu(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo(), x.getMaThoiKhoaBieu());
                return true;
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); return false; }
        }
    }
    public boolean update(ThoiKhoaBieuModel x) {
        if (x == null || x.getMaThoiKhoaBieu() <= 0 || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "UPDATE dbo.ThoiKhoaBieu SET MaLop = ?, ThuTrongTuan = ?, MaCaHoc = ?, TuNgay = ?, DenNgay = ?, TrangThai = ? WHERE MaCoSo = ? AND MaThoiKhoaBieu = ?";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql))) {
            ps.setObject(1, x.getMaLop(), Types.INTEGER);
            ps.setObject(2, x.getThuTrongTuan(), Types.TINYINT);
            ps.setObject(3, x.getMaCaHoc(), Types.INTEGER);
            ps.setObject(4, x.getTuNgay() == null ? null : java.sql.Date.valueOf(x.getTuNgay()), Types.DATE);
            ps.setObject(5, x.getDenNgay() == null ? null : java.sql.Date.valueOf(x.getDenNgay()), Types.DATE);
            ps.setObject(6, x.getTrangThai(), Types.BIT);
            ps.setInt(7, x.getMaCoSo());
            ps.setInt(8, x.getMaThoiKhoaBieu());
                if (ps.executeUpdate() == 1) {
                    refreshAfterWrite(conn, x.getMaCoSo(), x.getMaThoiKhoaBieu());
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
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, "DELETE FROM dbo.ThoiKhoaBieu WHERE MaCoSo = ? AND MaThoiKhoaBieu = ?"))) {
                ps.setInt(1, maCoSo); ps.setInt(2, ma);
                if (ps.executeUpdate() == 1) { CACHE.deleted(maCoSo, ma); return true; }
                CACHE.invalidate(maCoSo);
            } catch (SQLException e) { CACHE.invalidate(maCoSo); logError(e); }
            return false;
        }
    }

    /** Đọc-sửa-ghi trên bản mới từ DB; khóa chỉ có hiệu lực trong JVM này. */
    public ThoiKhoaBieuModel modify(int maCoSo, int ma, Predicate<ThoiKhoaBieuModel> change) {
        if (change == null || !(maCoSo > 0 && ma > 0)) return null;
        synchronized (LOCK) {
            ThoiKhoaBieuModel x = CACHE.fresh(maCoSo, ma);
            if (x == null) return null;
            if (!change.test(x)) return CACHE.fresh(maCoSo, ma);
            if (x.getMaThoiKhoaBieu() != ma || x.getMaCoSo() != maCoSo) return null;
            return update(x) ? (CACHE.loaded(maCoSo) ? getByMaThoiKhoaBieu(maCoSo, ma) : CACHE.fresh(maCoSo, ma)) : null;
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int maCoSo, int ma) { CACHE.afterWrite(conn, maCoSo, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int maCoSo, int ma) { CACHE.invalidate(maCoSo); }

    private static ThoiKhoaBieuModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new ThoiKhoaBieuModel(
                rs.getInt(columns.get("MaThoiKhoaBieu")),
                rs.getInt(columns.get("MaCoSo")),
                rs.getInt(columns.get("MaLop")),
                rs.getInt(columns.get("ThuTrongTuan")),
                rs.getInt(columns.get("MaCaHoc")),
                localDate(rs, columns.get("TuNgay")),
                localDate(rs, columns.get("DenNgay")),
                rs.getBoolean(columns.get("TrangThai")));
    }
    private static ThoiKhoaBieuModel copy(ThoiKhoaBieuModel x) { return new ThoiKhoaBieuModel(x.getMaThoiKhoaBieu(), x.getMaCoSo(), x.getMaLop(), x.getThuTrongTuan(), x.getMaCaHoc(), x.getTuNgay(), x.getDenNgay(), x.getTrangThai()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[ThoiKhoaBieuRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
