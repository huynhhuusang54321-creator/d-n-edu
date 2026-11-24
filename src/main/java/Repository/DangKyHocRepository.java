package Repository;

import Data.DataConnection;
import Model.DangKyHocModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class DangKyHocRepository {
    private static final String COLUMNS = "MaDangKy, MaCoSo, MaHocVien, MaKhoaHoc, MaLop, MaDangKyGoc, MaDangKyTruoc, NgayDangKy, NgayBatDau, NgayKetThucDuKien, NgayKetThucThucTe, TongSoBuoi, HocPhiThoaThuan, SoTienGiam, MaNguoiDangKy, TrangThai, GhiChu";
    private static final TableCache<DangKyHocModel> CACHE = new TableCache<>("DangKyHoc", COLUMNS, "MaDangKy", true, DangKyHocRepository::map, DangKyHocRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public DangKyHocModel getByMaDangKy(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return null;
        return CACHE.byId(maCoSo, ma);
    }
    public List<DangKyHocModel> getAll(int maCoSo) {
        if (maCoSo <= 0) return new ArrayList<>();
        return CACHE.all(maCoSo);
    }
    public List<DangKyHocModel> getByMaHocVien(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaHocVien(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DangKyHocModel> getByMaKhoaHoc(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaKhoaHoc(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DangKyHocModel> getByMaLop(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaLop(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DangKyHocModel> getByMaDangKyGoc(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaDangKyGoc(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DangKyHocModel> getByMaDangKyTruoc(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaDangKyTruoc(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DangKyHocModel> getByMaNguoiDangKy(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaNguoiDangKy(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<DangKyHocModel> reload(int maCoSo) { return CACHE.reload(maCoSo); }
    public TableCache.Snapshot selectRaw(int maCoSo) { return CACHE.raw(maCoSo); }
    public TableCache.Snapshot reloadRaw(int maCoSo) { return CACHE.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return CACHE.loaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { CACHE.invalidate(maCoSo); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(DangKyHocModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.DangKyHoc (MaCoSo, MaHocVien, MaKhoaHoc, MaLop, MaDangKyGoc, MaDangKyTruoc, NgayDangKy, NgayBatDau, NgayKetThucDuKien, NgayKetThucThucTe, TongSoBuoi, HocPhiThoaThuan, SoTienGiam, MaNguoiDangKy, TrangThai, GhiChu) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSo(), Types.INTEGER);
            ps.setObject(2, x.getMaHocVien(), Types.INTEGER);
            ps.setObject(3, x.getMaKhoaHoc(), Types.INTEGER);
            ps.setObject(4, x.getMaLop(), Types.INTEGER);
            ps.setObject(5, x.getMaDangKyGoc(), Types.INTEGER);
            ps.setObject(6, x.getMaDangKyTruoc(), Types.INTEGER);
            ps.setObject(7, x.getNgayDangKy() == null ? null : java.sql.Date.valueOf(x.getNgayDangKy()), Types.DATE);
            ps.setObject(8, x.getNgayBatDau() == null ? null : java.sql.Date.valueOf(x.getNgayBatDau()), Types.DATE);
            ps.setObject(9, x.getNgayKetThucDuKien() == null ? null : java.sql.Date.valueOf(x.getNgayKetThucDuKien()), Types.DATE);
            ps.setObject(10, x.getNgayKetThucThucTe() == null ? null : java.sql.Date.valueOf(x.getNgayKetThucThucTe()), Types.DATE);
            ps.setObject(11, x.getTongSoBuoi(), Types.INTEGER);
            ps.setObject(12, x.getHocPhiThoaThuan(), Types.DECIMAL);
            ps.setObject(13, x.getSoTienGiam(), Types.DECIMAL);
            ps.setObject(14, x.getMaNguoiDangKy(), Types.INTEGER);
            ps.setObject(15, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(16, x.getGhiChu(), Types.NVARCHAR);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(x.getMaCoSo()); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaDangKy(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo(), x.getMaDangKy());
                return true;
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); return false; }
        }
    }
    public boolean update(DangKyHocModel x) {
        if (x == null || x.getMaDangKy() <= 0 || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "UPDATE dbo.DangKyHoc SET MaHocVien = ?, MaKhoaHoc = ?, MaLop = ?, MaDangKyGoc = ?, MaDangKyTruoc = ?, NgayDangKy = ?, NgayBatDau = ?, NgayKetThucDuKien = ?, NgayKetThucThucTe = ?, TongSoBuoi = ?, HocPhiThoaThuan = ?, SoTienGiam = ?, MaNguoiDangKy = ?, TrangThai = ?, GhiChu = ? WHERE MaCoSo = ? AND MaDangKy = ?";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql))) {
            ps.setObject(1, x.getMaHocVien(), Types.INTEGER);
            ps.setObject(2, x.getMaKhoaHoc(), Types.INTEGER);
            ps.setObject(3, x.getMaLop(), Types.INTEGER);
            ps.setObject(4, x.getMaDangKyGoc(), Types.INTEGER);
            ps.setObject(5, x.getMaDangKyTruoc(), Types.INTEGER);
            ps.setObject(6, x.getNgayDangKy() == null ? null : java.sql.Date.valueOf(x.getNgayDangKy()), Types.DATE);
            ps.setObject(7, x.getNgayBatDau() == null ? null : java.sql.Date.valueOf(x.getNgayBatDau()), Types.DATE);
            ps.setObject(8, x.getNgayKetThucDuKien() == null ? null : java.sql.Date.valueOf(x.getNgayKetThucDuKien()), Types.DATE);
            ps.setObject(9, x.getNgayKetThucThucTe() == null ? null : java.sql.Date.valueOf(x.getNgayKetThucThucTe()), Types.DATE);
            ps.setObject(10, x.getTongSoBuoi(), Types.INTEGER);
            ps.setObject(11, x.getHocPhiThoaThuan(), Types.DECIMAL);
            ps.setObject(12, x.getSoTienGiam(), Types.DECIMAL);
            ps.setObject(13, x.getMaNguoiDangKy(), Types.INTEGER);
            ps.setObject(14, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(15, x.getGhiChu(), Types.NVARCHAR);
            ps.setInt(16, x.getMaCoSo());
            ps.setInt(17, x.getMaDangKy());
                if (ps.executeUpdate() == 1) {
                    refreshAfterWrite(conn, x.getMaCoSo(), x.getMaDangKy());
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
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, "DELETE FROM dbo.DangKyHoc WHERE MaCoSo = ? AND MaDangKy = ?"))) {
                ps.setInt(1, maCoSo); ps.setInt(2, ma);
                if (ps.executeUpdate() == 1) { CACHE.deleted(maCoSo, ma); return true; }
                CACHE.invalidate(maCoSo);
            } catch (SQLException e) { CACHE.invalidate(maCoSo); logError(e); }
            return false;
        }
    }

    /** Đọc-sửa-ghi trên bản mới từ DB; khóa chỉ có hiệu lực trong JVM này. */
    public DangKyHocModel modify(int maCoSo, int ma, Predicate<DangKyHocModel> change) {
        if (change == null || !(maCoSo > 0 && ma > 0)) return null;
        synchronized (LOCK) {
            DangKyHocModel x = CACHE.fresh(maCoSo, ma);
            if (x == null) return null;
            if (!change.test(x)) return CACHE.fresh(maCoSo, ma);
            if (x.getMaDangKy() != ma || x.getMaCoSo() != maCoSo) return null;
            return update(x) ? (CACHE.loaded(maCoSo) ? getByMaDangKy(maCoSo, ma) : CACHE.fresh(maCoSo, ma)) : null;
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int maCoSo, int ma) { CACHE.afterWrite(conn, maCoSo, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int maCoSo, int ma) { CACHE.invalidate(maCoSo); }

    private static DangKyHocModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new DangKyHocModel(
                rs.getInt(columns.get("MaDangKy")),
                rs.getInt(columns.get("MaCoSo")),
                rs.getInt(columns.get("MaHocVien")),
                nullableInt(rs, columns.get("MaKhoaHoc")),
                nullableInt(rs, columns.get("MaLop")),
                nullableInt(rs, columns.get("MaDangKyGoc")),
                nullableInt(rs, columns.get("MaDangKyTruoc")),
                localDate(rs, columns.get("NgayDangKy")),
                localDate(rs, columns.get("NgayBatDau")),
                localDate(rs, columns.get("NgayKetThucDuKien")),
                localDate(rs, columns.get("NgayKetThucThucTe")),
                nullableInt(rs, columns.get("TongSoBuoi")),
                rs.getBigDecimal(columns.get("HocPhiThoaThuan")),
                rs.getBigDecimal(columns.get("SoTienGiam")),
                rs.getInt(columns.get("MaNguoiDangKy")),
                rs.getString(columns.get("TrangThai")),
                rs.getString(columns.get("GhiChu")));
    }
    private static DangKyHocModel copy(DangKyHocModel x) { return new DangKyHocModel(x.getMaDangKy(), x.getMaCoSo(), x.getMaHocVien(), x.getMaKhoaHoc(), x.getMaLop(), x.getMaDangKyGoc(), x.getMaDangKyTruoc(), x.getNgayDangKy(), x.getNgayBatDau(), x.getNgayKetThucDuKien(), x.getNgayKetThucThucTe(), x.getTongSoBuoi(), x.getHocPhiThoaThuan(), x.getSoTienGiam(), x.getMaNguoiDangKy(), x.getTrangThai(), x.getGhiChu()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[DangKyHocRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
