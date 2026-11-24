package Repository;

import Data.DataConnection;
import Model.GiaoDichTaiChinhModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class GiaoDichTaiChinhRepository {
    private static final String COLUMNS = "MaGiaoDich, MaCoSo, MaHocVien, MaDangKy, MaGiaoDichNguon, MaNhomChungTu, LoaiGiaoDich, MaLoaiPhi, MaHinhThuc, KyPhi, SoLuong, DonGia, SoTien, NgayGiaoDich, HanThanhToan, MaNhanSu, TrangThai, DienGiai";
    private static final TableCache<GiaoDichTaiChinhModel> CACHE = new TableCache<>("GiaoDichTaiChinh", COLUMNS, "MaGiaoDich", true, GiaoDichTaiChinhRepository::map, GiaoDichTaiChinhRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public GiaoDichTaiChinhModel getByMaGiaoDich(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return null;
        return CACHE.byId(maCoSo, ma);
    }
    public List<GiaoDichTaiChinhModel> getAll(int maCoSo) {
        if (maCoSo <= 0) return new ArrayList<>();
        return CACHE.all(maCoSo);
    }
    public List<GiaoDichTaiChinhModel> getByMaHocVien(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaHocVien(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<GiaoDichTaiChinhModel> getByMaDangKy(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaDangKy(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<GiaoDichTaiChinhModel> getByMaGiaoDichNguon(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaGiaoDichNguon(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<GiaoDichTaiChinhModel> getByMaLoaiPhi(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaLoaiPhi(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<GiaoDichTaiChinhModel> getByMaHinhThuc(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaHinhThuc(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<GiaoDichTaiChinhModel> getByMaNhanSu(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaNhanSu(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<GiaoDichTaiChinhModel> reload(int maCoSo) { return CACHE.reload(maCoSo); }
    public TableCache.Snapshot selectRaw(int maCoSo) { return CACHE.raw(maCoSo); }
    public TableCache.Snapshot reloadRaw(int maCoSo) { return CACHE.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return CACHE.loaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { CACHE.invalidate(maCoSo); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(GiaoDichTaiChinhModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.GiaoDichTaiChinh (MaCoSo, MaHocVien, MaDangKy, MaGiaoDichNguon, MaNhomChungTu, LoaiGiaoDich, MaLoaiPhi, MaHinhThuc, KyPhi, SoLuong, DonGia, SoTien, NgayGiaoDich, HanThanhToan, MaNhanSu, TrangThai, DienGiai) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSo(), Types.INTEGER);
            ps.setObject(2, x.getMaHocVien(), Types.INTEGER);
            ps.setObject(3, x.getMaDangKy(), Types.INTEGER);
            ps.setObject(4, x.getMaGiaoDichNguon(), Types.INTEGER);
            ps.setObject(5, x.getMaNhomChungTu(), Types.VARCHAR);
            ps.setObject(6, x.getLoaiGiaoDich(), Types.VARCHAR);
            ps.setObject(7, x.getMaLoaiPhi(), Types.INTEGER);
            ps.setObject(8, x.getMaHinhThuc(), Types.INTEGER);
            ps.setObject(9, x.getKyPhi() == null ? null : java.sql.Date.valueOf(x.getKyPhi()), Types.DATE);
            ps.setObject(10, x.getSoLuong(), Types.DECIMAL);
            ps.setObject(11, x.getDonGia(), Types.DECIMAL);
            ps.setObject(12, x.getSoTien(), Types.DECIMAL);
            ps.setObject(13, x.getNgayGiaoDich() == null ? null : Timestamp.valueOf(x.getNgayGiaoDich()), Types.TIMESTAMP);
            ps.setObject(14, x.getHanThanhToan() == null ? null : java.sql.Date.valueOf(x.getHanThanhToan()), Types.DATE);
            ps.setObject(15, x.getMaNhanSu(), Types.INTEGER);
            ps.setObject(16, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(17, x.getDienGiai(), Types.NVARCHAR);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(x.getMaCoSo()); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaGiaoDich(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo(), x.getMaGiaoDich());
                return true;
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); return false; }
        }
    }
    public boolean update(GiaoDichTaiChinhModel x) {
        if (x == null || x.getMaGiaoDich() <= 0 || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "UPDATE dbo.GiaoDichTaiChinh SET MaHocVien = ?, MaDangKy = ?, MaGiaoDichNguon = ?, MaNhomChungTu = ?, LoaiGiaoDich = ?, MaLoaiPhi = ?, MaHinhThuc = ?, KyPhi = ?, SoLuong = ?, DonGia = ?, SoTien = ?, NgayGiaoDich = ?, HanThanhToan = ?, MaNhanSu = ?, TrangThai = ?, DienGiai = ? WHERE MaCoSo = ? AND MaGiaoDich = ? AND TrangThai = 'NHAP'";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql))) {
            ps.setObject(1, x.getMaHocVien(), Types.INTEGER);
            ps.setObject(2, x.getMaDangKy(), Types.INTEGER);
            ps.setObject(3, x.getMaGiaoDichNguon(), Types.INTEGER);
            ps.setObject(4, x.getMaNhomChungTu(), Types.VARCHAR);
            ps.setObject(5, x.getLoaiGiaoDich(), Types.VARCHAR);
            ps.setObject(6, x.getMaLoaiPhi(), Types.INTEGER);
            ps.setObject(7, x.getMaHinhThuc(), Types.INTEGER);
            ps.setObject(8, x.getKyPhi() == null ? null : java.sql.Date.valueOf(x.getKyPhi()), Types.DATE);
            ps.setObject(9, x.getSoLuong(), Types.DECIMAL);
            ps.setObject(10, x.getDonGia(), Types.DECIMAL);
            ps.setObject(11, x.getSoTien(), Types.DECIMAL);
            ps.setObject(12, x.getNgayGiaoDich() == null ? null : Timestamp.valueOf(x.getNgayGiaoDich()), Types.TIMESTAMP);
            ps.setObject(13, x.getHanThanhToan() == null ? null : java.sql.Date.valueOf(x.getHanThanhToan()), Types.DATE);
            ps.setObject(14, x.getMaNhanSu(), Types.INTEGER);
            ps.setObject(15, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(16, x.getDienGiai(), Types.NVARCHAR);
            ps.setInt(17, x.getMaCoSo());
            ps.setInt(18, x.getMaGiaoDich());
                if (ps.executeUpdate() == 1) {
                    refreshAfterWrite(conn, x.getMaCoSo(), x.getMaGiaoDich());
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
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, "DELETE FROM dbo.GiaoDichTaiChinh WHERE MaCoSo = ? AND MaGiaoDich = ? AND TrangThai = 'NHAP'"))) {
                ps.setInt(1, maCoSo); ps.setInt(2, ma);
                if (ps.executeUpdate() == 1) { CACHE.deleted(maCoSo, ma); return true; }
                CACHE.invalidate(maCoSo);
            } catch (SQLException e) { CACHE.invalidate(maCoSo); logError(e); }
            return false;
        }
    }

    /** Đọc-sửa-ghi trên bản mới từ DB; khóa chỉ có hiệu lực trong JVM này. */
    public GiaoDichTaiChinhModel modify(int maCoSo, int ma, Predicate<GiaoDichTaiChinhModel> change) {
        if (change == null || !(maCoSo > 0 && ma > 0)) return null;
        synchronized (LOCK) {
            GiaoDichTaiChinhModel x = CACHE.fresh(maCoSo, ma);
            if (x == null) return null;
            if (!change.test(x)) return CACHE.fresh(maCoSo, ma);
            if (x.getMaGiaoDich() != ma || x.getMaCoSo() != maCoSo) return null;
            return update(x) ? (CACHE.loaded(maCoSo) ? getByMaGiaoDich(maCoSo, ma) : CACHE.fresh(maCoSo, ma)) : null;
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int maCoSo, int ma) { CACHE.afterWrite(conn, maCoSo, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int maCoSo, int ma) { CACHE.invalidate(maCoSo); }

    private static GiaoDichTaiChinhModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new GiaoDichTaiChinhModel(
                rs.getInt(columns.get("MaGiaoDich")),
                rs.getInt(columns.get("MaCoSo")),
                nullableInt(rs, columns.get("MaHocVien")),
                nullableInt(rs, columns.get("MaDangKy")),
                nullableInt(rs, columns.get("MaGiaoDichNguon")),
                rs.getString(columns.get("MaNhomChungTu")),
                rs.getString(columns.get("LoaiGiaoDich")),
                rs.getInt(columns.get("MaLoaiPhi")),
                nullableInt(rs, columns.get("MaHinhThuc")),
                localDate(rs, columns.get("KyPhi")),
                rs.getBigDecimal(columns.get("SoLuong")),
                rs.getBigDecimal(columns.get("DonGia")),
                rs.getBigDecimal(columns.get("SoTien")),
                localDateTime(rs, columns.get("NgayGiaoDich")),
                localDate(rs, columns.get("HanThanhToan")),
                rs.getInt(columns.get("MaNhanSu")),
                rs.getString(columns.get("TrangThai")),
                rs.getString(columns.get("DienGiai")));
    }
    private static GiaoDichTaiChinhModel copy(GiaoDichTaiChinhModel x) { return new GiaoDichTaiChinhModel(x.getMaGiaoDich(), x.getMaCoSo(), x.getMaHocVien(), x.getMaDangKy(), x.getMaGiaoDichNguon(), x.getMaNhomChungTu(), x.getLoaiGiaoDich(), x.getMaLoaiPhi(), x.getMaHinhThuc(), x.getKyPhi(), x.getSoLuong(), x.getDonGia(), x.getSoTien(), x.getNgayGiaoDich(), x.getHanThanhToan(), x.getMaNhanSu(), x.getTrangThai(), x.getDienGiai()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[GiaoDichTaiChinhRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
