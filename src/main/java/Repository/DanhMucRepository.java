package Repository;

import Data.DataConnection;
import Model.DanhMucModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class DanhMucRepository {
    private static final String COLUMNS = "MaDanhMuc, MaCoSo, NhomDanhMuc, MaKyHieu, TenDanhMuc, MaCha, ThuTu, CauHinh, TrangThai";
    private static final TableCache<DanhMucModel> CACHE = new TableCache<>("DanhMuc", COLUMNS, "MaDanhMuc", true, DanhMucRepository::map, DanhMucRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public DanhMucModel getByMaDanhMuc(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return null;
        return CACHE.byId(maCoSo, ma);
    }
    public List<DanhMucModel> getAll(int maCoSo) {
        if (maCoSo <= 0) return new ArrayList<>();
        return CACHE.all(maCoSo);
    }
    public List<DanhMucModel> getByMaCha(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaCha(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DanhMucModel> getByNhomDanhMuc(int maCoSo, String value) {
        return getAll(maCoSo).stream().filter(x -> x.getNhomDanhMuc() != null && value != null && x.getNhomDanhMuc().trim().equalsIgnoreCase(value.trim())).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DanhMucModel> reload(int maCoSo) { return CACHE.reload(maCoSo); }
    public TableCache.Snapshot selectRaw(int maCoSo) { return CACHE.raw(maCoSo); }
    public TableCache.Snapshot reloadRaw(int maCoSo) { return CACHE.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return CACHE.loaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { CACHE.invalidate(maCoSo); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(DanhMucModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.DanhMuc (MaCoSo, NhomDanhMuc, MaKyHieu, TenDanhMuc, MaCha, ThuTu, CauHinh, TrangThai) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSo(), Types.INTEGER);
            ps.setObject(2, x.getNhomDanhMuc(), Types.VARCHAR);
            ps.setObject(3, x.getMaKyHieu(), Types.VARCHAR);
            ps.setObject(4, x.getTenDanhMuc(), Types.NVARCHAR);
            ps.setObject(5, x.getMaCha(), Types.INTEGER);
            ps.setObject(6, x.getThuTu(), Types.INTEGER);
            ps.setObject(7, x.getCauHinh(), Types.NVARCHAR);
            ps.setObject(8, x.getTrangThai(), Types.BIT);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(x.getMaCoSo()); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaDanhMuc(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo(), x.getMaDanhMuc());
                return true;
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); return false; }
        }
    }
    public boolean update(DanhMucModel x) {
        if (x == null || x.getMaDanhMuc() <= 0 || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "UPDATE dbo.DanhMuc SET NhomDanhMuc = ?, MaKyHieu = ?, TenDanhMuc = ?, MaCha = ?, ThuTu = ?, CauHinh = ?, TrangThai = ? WHERE MaCoSo = ? AND MaDanhMuc = ?";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql))) {
            ps.setObject(1, x.getNhomDanhMuc(), Types.VARCHAR);
            ps.setObject(2, x.getMaKyHieu(), Types.VARCHAR);
            ps.setObject(3, x.getTenDanhMuc(), Types.NVARCHAR);
            ps.setObject(4, x.getMaCha(), Types.INTEGER);
            ps.setObject(5, x.getThuTu(), Types.INTEGER);
            ps.setObject(6, x.getCauHinh(), Types.NVARCHAR);
            ps.setObject(7, x.getTrangThai(), Types.BIT);
            ps.setInt(8, x.getMaCoSo());
            ps.setInt(9, x.getMaDanhMuc());
                if (ps.executeUpdate() == 1) {
                    refreshAfterWrite(conn, x.getMaCoSo(), x.getMaDanhMuc());
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
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, "DELETE FROM dbo.DanhMuc WHERE MaCoSo = ? AND MaDanhMuc = ?"))) {
                ps.setInt(1, maCoSo); ps.setInt(2, ma);
                if (ps.executeUpdate() == 1) { CACHE.deleted(maCoSo, ma); return true; }
                CACHE.invalidate(maCoSo);
            } catch (SQLException e) { CACHE.invalidate(maCoSo); logError(e); }
            return false;
        }
    }

    /** Đọc-sửa-ghi trên bản mới từ DB; khóa chỉ có hiệu lực trong JVM này. */
    public DanhMucModel modify(int maCoSo, int ma, Predicate<DanhMucModel> change) {
        if (change == null || !(maCoSo > 0 && ma > 0)) return null;
        synchronized (LOCK) {
            DanhMucModel x = CACHE.fresh(maCoSo, ma);
            if (x == null) return null;
            if (!change.test(x)) return CACHE.fresh(maCoSo, ma);
            if (x.getMaDanhMuc() != ma || x.getMaCoSo() != maCoSo) return null;
            return update(x) ? (CACHE.loaded(maCoSo) ? getByMaDanhMuc(maCoSo, ma) : CACHE.fresh(maCoSo, ma)) : null;
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int maCoSo, int ma) { CACHE.afterWrite(conn, maCoSo, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int maCoSo, int ma) { CACHE.invalidate(maCoSo); }

    private static DanhMucModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new DanhMucModel(
                rs.getInt(columns.get("MaDanhMuc")),
                rs.getInt(columns.get("MaCoSo")),
                rs.getString(columns.get("NhomDanhMuc")),
                rs.getString(columns.get("MaKyHieu")),
                rs.getString(columns.get("TenDanhMuc")),
                nullableInt(rs, columns.get("MaCha")),
                rs.getInt(columns.get("ThuTu")),
                rs.getString(columns.get("CauHinh")),
                rs.getBoolean(columns.get("TrangThai")));
    }
    private static DanhMucModel copy(DanhMucModel x) { return new DanhMucModel(x.getMaDanhMuc(), x.getMaCoSo(), x.getNhomDanhMuc(), x.getMaKyHieu(), x.getTenDanhMuc(), x.getMaCha(), x.getThuTu(), x.getCauHinh(), x.getTrangThai()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[DanhMucRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
