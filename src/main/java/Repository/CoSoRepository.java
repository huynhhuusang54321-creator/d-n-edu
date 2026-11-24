package Repository;

import Data.DataConnection;
import Model.CoSoModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class CoSoRepository {
    private static final String COLUMNS = "MaCoSo, MaCoSoHienThi, TenCoSo, LoaiCoSo, DiaChi, SoDienThoai, Logo, TrangThai";
    private static final TableCache<CoSoModel> CACHE = new TableCache<>("CoSo", COLUMNS, "MaCoSo", false, CoSoRepository::map, CoSoRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public CoSoModel getByMaCoSo(int ma) {
        if (!(ma > 0)) return null;
        return CACHE.byId(0, ma);
    }
    public List<CoSoModel> getAll() {
        
        return CACHE.all(0);
    }
    public List<CoSoModel> reload() { return CACHE.reload(0); }
    public TableCache.Snapshot selectRaw() { return CACHE.raw(0); }
    public TableCache.Snapshot reloadRaw() { return CACHE.reloadRaw(0); }
    public boolean isCacheLoaded() { return CACHE.loaded(0); }
    public void invalidateBranch() { CACHE.invalidate(0); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(CoSoModel x) {
        if (x == null) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.CoSo (MaCoSoHienThi, TenCoSo, LoaiCoSo, DiaChi, SoDienThoai, Logo, TrangThai) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSoHienThi(), Types.VARCHAR);
            ps.setObject(2, x.getTenCoSo(), Types.NVARCHAR);
            ps.setObject(3, x.getLoaiCoSo(), Types.VARCHAR);
            ps.setObject(4, x.getDiaChi(), Types.NVARCHAR);
            ps.setObject(5, x.getSoDienThoai(), Types.VARCHAR);
            ps.setObject(6, x.getLogo(), Types.NVARCHAR);
            ps.setObject(7, x.getTrangThai(), Types.BIT);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(0); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaCoSo(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo());
                return true;
            } catch (SQLException e) { CACHE.invalidate(0); logError(e); return false; }
        }
    }
    public boolean update(CoSoModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "UPDATE dbo.CoSo SET MaCoSoHienThi = ?, TenCoSo = ?, LoaiCoSo = ?, DiaChi = ?, SoDienThoai = ?, Logo = ?, TrangThai = ? WHERE MaCoSo = ?";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql))) {
            ps.setObject(1, x.getMaCoSoHienThi(), Types.VARCHAR);
            ps.setObject(2, x.getTenCoSo(), Types.NVARCHAR);
            ps.setObject(3, x.getLoaiCoSo(), Types.VARCHAR);
            ps.setObject(4, x.getDiaChi(), Types.NVARCHAR);
            ps.setObject(5, x.getSoDienThoai(), Types.VARCHAR);
            ps.setObject(6, x.getLogo(), Types.NVARCHAR);
            ps.setObject(7, x.getTrangThai(), Types.BIT);
            ps.setInt(8, x.getMaCoSo());
                if (ps.executeUpdate() == 1) {
                    refreshAfterWrite(conn, x.getMaCoSo());
                    return true;
                }
                CACHE.invalidate(0);
            } catch (SQLException e) { CACHE.invalidate(0); logError(e); }
            return false;
        }
    }

    public boolean delete(int ma) {
        if (!(ma > 0)) return false;
        synchronized (LOCK) {
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, "DELETE FROM dbo.CoSo WHERE MaCoSo = ?"))) {
                ps.setInt(1, ma);
                if (ps.executeUpdate() == 1) { CACHE.deleted(0, ma); return true; }
                CACHE.invalidate(0);
            } catch (SQLException e) { CACHE.invalidate(0); logError(e); }
            return false;
        }
    }

    /** Đọc-sửa-ghi trên bản mới từ DB; khóa chỉ có hiệu lực trong JVM này. */
    public CoSoModel modify(int ma, Predicate<CoSoModel> change) {
        if (change == null || !(ma > 0)) return null;
        synchronized (LOCK) {
            CoSoModel x = CACHE.fresh(0, ma);
            if (x == null) return null;
            if (!change.test(x)) return CACHE.fresh(0, ma);
            if (x.getMaCoSo() != ma) return null;
            return update(x) ? (CACHE.loaded(0) ? getByMaCoSo(ma) : CACHE.fresh(0, ma)) : null;
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int ma) { CACHE.afterWrite(conn, 0, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int ma) { CACHE.invalidate(0); }

    private static CoSoModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new CoSoModel(
                rs.getInt(columns.get("MaCoSo")),
                rs.getString(columns.get("MaCoSoHienThi")),
                rs.getString(columns.get("TenCoSo")),
                rs.getString(columns.get("LoaiCoSo")),
                rs.getString(columns.get("DiaChi")),
                rs.getString(columns.get("SoDienThoai")),
                rs.getString(columns.get("Logo")),
                rs.getBoolean(columns.get("TrangThai")));
    }
    private static CoSoModel copy(CoSoModel x) { return new CoSoModel(x.getMaCoSo(), x.getMaCoSoHienThi(), x.getTenCoSo(), x.getLoaiCoSo(), x.getDiaChi(), x.getSoDienThoai(), x.getLogo(), x.getTrangThai()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[CoSoRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
