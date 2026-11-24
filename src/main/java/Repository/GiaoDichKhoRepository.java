package Repository;

import Data.DataConnection;
import Model.GiaoDichKhoModel;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** JDBC và cache danh sách theo cơ sở; đọc từ RAM sau lần nạp đầu. */
public class GiaoDichKhoRepository {
    private static final String COLUMNS = "MaGiaoDichKho, MaCoSo, MaNhomChungTu, MaHangHoa, MaHocVien, MaGiaoDichTaiChinh, LoaiGiaoDich, SoLuong, TonThucTe, DonGia, NgayGiaoDich, MaNhanSu, TrangThai, GhiChu";
    private static final TableCache<GiaoDichKhoModel> CACHE = new TableCache<>("GiaoDichKho", COLUMNS, "MaGiaoDichKho", true, GiaoDichKhoRepository::map, GiaoDichKhoRepository::copy);
    private static final Object LOCK = CACHE;

    // ==================== ĐỌC CACHE FIRST ====================
    public GiaoDichKhoModel getByMaGiaoDichKho(int maCoSo, int ma) {
        if (!(maCoSo > 0 && ma > 0)) return null;
        return CACHE.byId(maCoSo, ma);
    }
    public List<GiaoDichKhoModel> getAll(int maCoSo) {
        if (maCoSo <= 0) return new ArrayList<>();
        return CACHE.all(maCoSo);
    }
    public List<GiaoDichKhoModel> getByMaHangHoa(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaHangHoa(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<GiaoDichKhoModel> getByMaHocVien(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaHocVien(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<GiaoDichKhoModel> getByMaGiaoDichTaiChinh(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaGiaoDichTaiChinh(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<GiaoDichKhoModel> getByMaNhanSu(int maCoSo, int value) {
        return getAll(maCoSo).stream().filter(x -> Objects.equals(x.getMaNhanSu(), value)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public List<GiaoDichKhoModel> reload(int maCoSo) { return CACHE.reload(maCoSo); }
    public TableCache.Snapshot selectRaw(int maCoSo) { return CACHE.raw(maCoSo); }
    public TableCache.Snapshot reloadRaw(int maCoSo) { return CACHE.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return CACHE.loaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { CACHE.invalidate(maCoSo); }
    public void mapColumn(String oldName, String actualName) { CACHE.mapColumn(oldName, actualName); }

    // ==================== GHI ====================
    public boolean insert(GiaoDichKhoModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "INSERT INTO dbo.GiaoDichKho (MaCoSo, MaNhomChungTu, MaHangHoa, MaHocVien, MaGiaoDichTaiChinh, LoaiGiaoDich, SoLuong, TonThucTe, DonGia, NgayGiaoDich, MaNhanSu, TrangThai, GhiChu) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql), Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, x.getMaCoSo(), Types.INTEGER);
            ps.setObject(2, x.getMaNhomChungTu(), Types.VARCHAR);
            ps.setObject(3, x.getMaHangHoa(), Types.INTEGER);
            ps.setObject(4, x.getMaHocVien(), Types.INTEGER);
            ps.setObject(5, x.getMaGiaoDichTaiChinh(), Types.INTEGER);
            ps.setObject(6, x.getLoaiGiaoDich(), Types.VARCHAR);
            ps.setObject(7, x.getSoLuong(), Types.DECIMAL);
            ps.setObject(8, x.getTonThucTe(), Types.DECIMAL);
            ps.setObject(9, x.getDonGia(), Types.DECIMAL);
            ps.setObject(10, x.getNgayGiaoDich() == null ? null : Timestamp.valueOf(x.getNgayGiaoDich()), Types.TIMESTAMP);
            ps.setObject(11, x.getMaNhanSu(), Types.INTEGER);
            ps.setObject(12, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(13, x.getGhiChu(), Types.NVARCHAR);
                if (ps.executeUpdate() != 1) { CACHE.invalidate(x.getMaCoSo()); return false; }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã tự sinh; kiểm tra DB trước khi thử lại");
                    x.setMaGiaoDichKho(keys.getInt(1));
                }
                refreshAfterWrite(conn, x.getMaCoSo(), x.getMaGiaoDichKho());
                return true;
            } catch (SQLException e) { CACHE.invalidate(x.getMaCoSo()); logError(e); return false; }
        }
    }
    public boolean update(GiaoDichKhoModel x) {
        if (x == null || x.getMaGiaoDichKho() <= 0 || x.getMaCoSo() <= 0) return false;
        synchronized (LOCK) {
            String sql = "UPDATE dbo.GiaoDichKho SET MaNhomChungTu = ?, MaHangHoa = ?, MaHocVien = ?, MaGiaoDichTaiChinh = ?, LoaiGiaoDich = ?, SoLuong = ?, TonThucTe = ?, DonGia = ?, NgayGiaoDich = ?, MaNhanSu = ?, TrangThai = ?, GhiChu = ? WHERE MaCoSo = ? AND MaGiaoDichKho = ? AND TrangThai = 'NHAP'";
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, sql))) {
            ps.setObject(1, x.getMaNhomChungTu(), Types.VARCHAR);
            ps.setObject(2, x.getMaHangHoa(), Types.INTEGER);
            ps.setObject(3, x.getMaHocVien(), Types.INTEGER);
            ps.setObject(4, x.getMaGiaoDichTaiChinh(), Types.INTEGER);
            ps.setObject(5, x.getLoaiGiaoDich(), Types.VARCHAR);
            ps.setObject(6, x.getSoLuong(), Types.DECIMAL);
            ps.setObject(7, x.getTonThucTe(), Types.DECIMAL);
            ps.setObject(8, x.getDonGia(), Types.DECIMAL);
            ps.setObject(9, x.getNgayGiaoDich() == null ? null : Timestamp.valueOf(x.getNgayGiaoDich()), Types.TIMESTAMP);
            ps.setObject(10, x.getMaNhanSu(), Types.INTEGER);
            ps.setObject(11, x.getTrangThai(), Types.VARCHAR);
            ps.setObject(12, x.getGhiChu(), Types.NVARCHAR);
            ps.setInt(13, x.getMaCoSo());
            ps.setInt(14, x.getMaGiaoDichKho());
                if (ps.executeUpdate() == 1) {
                    refreshAfterWrite(conn, x.getMaCoSo(), x.getMaGiaoDichKho());
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
            try (Connection conn = DataConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(CACHE.writeSql(conn, "DELETE FROM dbo.GiaoDichKho WHERE MaCoSo = ? AND MaGiaoDichKho = ? AND TrangThai = 'NHAP'"))) {
                ps.setInt(1, maCoSo); ps.setInt(2, ma);
                if (ps.executeUpdate() == 1) { CACHE.deleted(maCoSo, ma); return true; }
                CACHE.invalidate(maCoSo);
            } catch (SQLException e) { CACHE.invalidate(maCoSo); logError(e); }
            return false;
        }
    }

    /** Đọc-sửa-ghi trên bản mới từ DB; khóa chỉ có hiệu lực trong JVM này. */
    public GiaoDichKhoModel modify(int maCoSo, int ma, Predicate<GiaoDichKhoModel> change) {
        if (change == null || !(maCoSo > 0 && ma > 0)) return null;
        synchronized (LOCK) {
            GiaoDichKhoModel x = CACHE.fresh(maCoSo, ma);
            if (x == null) return null;
            if (!change.test(x)) return CACHE.fresh(maCoSo, ma);
            if (x.getMaGiaoDichKho() != ma || x.getMaCoSo() != maCoSo) return null;
            return update(x) ? (CACHE.loaded(maCoSo) ? getByMaGiaoDichKho(maCoSo, ma) : CACHE.fresh(maCoSo, ma)) : null;
        }
    }

    // ==================== CACHE ====================
    private void refreshAfterWrite(Connection conn, int maCoSo, int ma) { CACHE.afterWrite(conn, maCoSo, ma); }
    public void resetCache() { CACHE.clear(); }
    public void evict(int maCoSo, int ma) { CACHE.invalidate(maCoSo); }

    private static GiaoDichKhoModel map(ResultSet rs, Map<String,String> columns) throws SQLException {
        return new GiaoDichKhoModel(
                rs.getInt(columns.get("MaGiaoDichKho")),
                rs.getInt(columns.get("MaCoSo")),
                rs.getString(columns.get("MaNhomChungTu")),
                rs.getInt(columns.get("MaHangHoa")),
                nullableInt(rs, columns.get("MaHocVien")),
                nullableInt(rs, columns.get("MaGiaoDichTaiChinh")),
                rs.getString(columns.get("LoaiGiaoDich")),
                rs.getBigDecimal(columns.get("SoLuong")),
                rs.getBigDecimal(columns.get("TonThucTe")),
                rs.getBigDecimal(columns.get("DonGia")),
                localDateTime(rs, columns.get("NgayGiaoDich")),
                rs.getInt(columns.get("MaNhanSu")),
                rs.getString(columns.get("TrangThai")),
                rs.getString(columns.get("GhiChu")));
    }
    private static GiaoDichKhoModel copy(GiaoDichKhoModel x) { return new GiaoDichKhoModel(x.getMaGiaoDichKho(), x.getMaCoSo(), x.getMaNhomChungTu(), x.getMaHangHoa(), x.getMaHocVien(), x.getMaGiaoDichTaiChinh(), x.getLoaiGiaoDich(), x.getSoLuong(), x.getTonThucTe(), x.getDonGia(), x.getNgayGiaoDich(), x.getMaNhanSu(), x.getTrangThai(), x.getGhiChu()); }
    private static Integer nullableInt(ResultSet rs, String c) throws SQLException { int v = rs.getInt(c); return rs.wasNull() ? null : v; }
    private static LocalDate localDate(ResultSet rs, String c) throws SQLException { java.sql.Date v = rs.getDate(c); return v == null ? null : v.toLocalDate(); }
    private static LocalDateTime localDateTime(ResultSet rs, String c) throws SQLException { Timestamp v = rs.getTimestamp(c); return v == null ? null : v.toLocalDateTime(); }
    private static LocalTime localTime(ResultSet rs, String c) throws SQLException { Time v = rs.getTime(c); return v == null ? null : v.toLocalTime(); }
    private static void logError(SQLException e) { System.err.println("[GiaoDichKhoRepository] SQL error: " + e.getErrorCode() + " / " + e.getSQLState()); }
}
