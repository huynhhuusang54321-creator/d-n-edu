package Repository;

import Data.DataConnection;
import Model.LopHocModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class LopHocRepository {
    private static final String COLUMNS = "MaLop, MaCoSo, MaLopHienThi, TenLop, MaKhoaHoc, MaNamHoc, MaGiaoVien, SiSoToiDa, NgayBatDau, NgayKetThuc, HocPhiMacDinh, TienAnMacDinh, TrangThai, GhiChu";
    private static final TableCache<LopHocModel> CACHE = new TableCache<>("LopHoc", COLUMNS, "MaLop", true, LopHocRepository::map, LopHocRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public LopHocModel getByMaLop(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return null;
        return CACHE.byId(maCoSo, ma);
    }
    public List<LopHocModel> getAll(int maCoSo) {
        if (maCoSo <= 0) return new ArrayList<>();
        return CACHE.all(maCoSo);
    }
    public List<LopHocModel> getByMaKhoaHoc(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaKhoaHoc(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<LopHocModel> getByMaNamHoc(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaNamHoc(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<LopHocModel> getByMaGiaoVien(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaGiaoVien(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<LopHocModel> reload(int maCoSo) { return CACHE.reload(maCoSo); }
    public TableCache.Snapshot selectRaw(int maCoSo) { return CACHE.raw(maCoSo); }
    public TableCache.Snapshot reloadRaw(int maCoSo) { return CACHE.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return CACHE.loaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { CACHE.invalidate(maCoSo); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(LopHocModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.LopHoc (MaCoSo, MaLopHienThi, TenLop, MaKhoaHoc, MaNamHoc, MaGiaoVien, SiSoToiDa, NgayBatDau, NgayKetThuc, HocPhiMacDinh, TienAnMacDinh, TrangThai, GhiChu) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSo(), Types.INTEGER);
            ps.setObject(2, x.getMaLopHienThi(), Types.VARCHAR);
            ps.setObject(3, x.getTenLop(), Types.NVARCHAR);
            ps.setObject(4, x.getMaKhoaHoc(), Types.INTEGER);
            ps.setObject(5, x.getMaNamHoc(), Types.INTEGER);
            ps.setObject(6, x.getMaGiaoVien(), Types.INTEGER);
            ps.setObject(7, x.getSiSoToiDa(), Types.SMALLINT);
            ps.setObject(8, x.getNgayBatDau() == null ? null : java.sql.Date.valueOf(x.getNgayBatDau()), Types.DATE);
            ps.setObject(9, x.getNgayKetThuc() == null ? null : java.sql.Date.valueOf(x.getNgayKetThuc()), Types.DATE);
            ps.setObject(10, x.getHocPhiMacDinh(), Types.DECIMAL);
            ps.setObject(11, x.getTienAnMacDinh(), Types.DECIMAL);
            ps.setObject(12, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(13, x.getGhiChu(), Types.NVARCHAR);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(x.getMaCoSo()); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaLop(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo(), x.getMaLop());
                return true;
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); return false; }
        }
    }
    public boolean update(LopHocModel x) {
        if (x == null || x.getMaLop() <= 0 || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "UPDATE dbo.LopHoc SET MaLopHienThi = ?, TenLop = ?, MaKhoaHoc = ?, MaNamHoc = ?, MaGiaoVien = ?, SiSoToiDa = ?, NgayBatDau = ?, NgayKetThuc = ?, HocPhiMacDinh = ?, TienAnMacDinh = ?, TrangThai = ?, GhiChu = ? WHERE MaCoSo = ? AND MaLop = ?";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql))) {
            ps.setObject(1, x.getMaLopHienThi(), Types.VARCHAR);
            ps.setObject(2, x.getTenLop(), Types.NVARCHAR);
            ps.setObject(3, x.getMaKhoaHoc(), Types.INTEGER);
            ps.setObject(4, x.getMaNamHoc(), Types.INTEGER);
            ps.setObject(5, x.getMaGiaoVien(), Types.INTEGER);
            ps.setObject(6, x.getSiSoToiDa(), Types.SMALLINT);
            ps.setObject(7, x.getNgayBatDau() == null ? null : java.sql.Date.valueOf(x.getNgayBatDau()), Types.DATE);
            ps.setObject(8, x.getNgayKetThuc() == null ? null : java.sql.Date.valueOf(x.getNgayKetThuc()), Types.DATE);
            ps.setObject(9, x.getHocPhiMacDinh(), Types.DECIMAL);
            ps.setObject(10, x.getTienAnMacDinh(), Types.DECIMAL);
            ps.setObject(11, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(12, x.getGhiChu(), Types.NVARCHAR);
            ps.setInt(13, x.getMaCoSo());
            ps.setInt(14, x.getMaLop());
                if (ps.executeUpdate() == 1) {
                    refreshAfterWrite(conn, x.getMaCoSo(), x.getMaLop());
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
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, "DELETE FROM dbo.LopHoc WHERE MaCoSo = ? AND MaLop = ?"))) {
                ps.setInt(1, maCoSo); ps.setInt(2, ma);
                if (ps.executeUpdate() == 1) { CACHE.deleted(maCoSo, ma); return true; }
                CACHE.invalidate(maCoSo);
            } catch (SQLException e) { CACHE.invalidate(maCoSo); logError(e); }
            return false;
        }
    }

    /** Đọc-sửa-ghi trên bản mới từ DB; khóa chỉ có hiệu lực trong JVM này. */
    public LopHocModel modify(int maCoSo, int ma, Predicate<LopHocModel> change) {
        if (change == null || !(maCoSo > 0 && ma > 0)) return null;
        synchronized (LOCK) {
            LopHocModel x = CACHE.fresh(maCoSo, ma);
            if (x == null) return null;
            if (!change.test(x)) return CACHE.fresh(maCoSo, ma);
            if (x.getMaLop() != ma || x.getMaCoSo() != maCoSo) return null;
            return update(x) ? (CACHE.loaded(maCoSo) ? getByMaLop(maCoSo, ma) : CACHE.fresh(maCoSo, ma)) : null;
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int maCoSo, int ma) { CACHE.afterWrite(conn, maCoSo, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int maCoSo, int ma) { CACHE.invalidate(maCoSo); }

    private static LopHocModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new LopHocModel(
                rs.getInt(columns.get("MaLop")),
                rs.getInt(columns.get("MaCoSo")),
                rs.getString(columns.get("MaLopHienThi")),
                rs.getString(columns.get("TenLop")),
                nullableInt(rs, columns.get("MaKhoaHoc")),
                nullableInt(rs, columns.get("MaNamHoc")),
                nullableInt(rs, columns.get("MaGiaoVien")),
                nullableInt(rs, columns.get("SiSoToiDa")),
                localDate(rs, columns.get("NgayBatDau")),
                localDate(rs, columns.get("NgayKetThuc")),
                rs.getBigDecimal(columns.get("HocPhiMacDinh")),
                rs.getBigDecimal(columns.get("TienAnMacDinh")),
                rs.getString(columns.get("TrangThai")),
                rs.getString(columns.get("GhiChu")));
    }
    private static LopHocModel copy(LopHocModel x) { return new LopHocModel(x.getMaLop(), x.getMaCoSo(), x.getMaLopHienThi(), x.getTenLop(), x.getMaKhoaHoc(), x.getMaNamHoc(), x.getMaGiaoVien(), x.getSiSoToiDa(), x.getNgayBatDau(), x.getNgayKetThuc(), x.getHocPhiMacDinh(), x.getTienAnMacDinh(), x.getTrangThai(), x.getGhiChu()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[LopHocRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
