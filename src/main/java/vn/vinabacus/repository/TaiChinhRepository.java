package vn.vinabacus.repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.TaiChinhDTO.*;

/** JDBC TV5: mọi truy vấn và phiếu thu đều kiểm tra MaCoSo. Không đặt mật khẩu trong mã. */
public final class TaiChinhRepository {
    @FunctionalInterface public interface KetNoi { Connection mo() throws SQLException; }
    private final KetNoi ketNoi;
    public TaiChinhRepository() { this(Database::moKetNoi); }
    public TaiChinhRepository(KetNoi ketNoi) {
        this.ketNoi=java.util.Objects.requireNonNull(ketNoi);
    }
    private int idCoSo(Connection c, CoSo coSo) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("SELECT MaCoSo FROM dbo.CoSo WHERE LoaiCoSo=? AND TrangThai=1")) {
            s.setString(1,coSo.name());
            try(ResultSet r=s.executeQuery()) { if(r.next()) return r.getInt(1); }
        }
        throw new SQLException("Không tìm thấy cơ sở đang hoạt động: " + coSo);
    }
    private int idMamNon(Connection c) throws SQLException { return idCoSo(c,CoSo.MAM_NON); }
    private static BigDecimal so(ResultSet r,String col) throws SQLException {
        BigDecimal n=r.getBigDecimal(col);return n==null?BigDecimal.ZERO:n;
    }
    private static String maGiaoDich() { return UUID.randomUUID().toString(); }
    /** NgayThu được lưu bằng SYSUTCDATETIME(), còn kỳ báo cáo tính theo giờ Việt Nam. */
    private static final ZoneId GIO_VIET_NAM=ZoneId.of("Asia/Ho_Chi_Minh");
    private static Timestamp dauThangUtc(YearMonth month) {
        LocalDateTime utc=month.atDay(1).atStartOfDay(GIO_VIET_NAM)
            .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return Timestamp.valueOf(utc);
    }
    private static Timestamp dauNgayUtc(LocalDate day) {
        LocalDateTime utc=day.atStartOfDay(GIO_VIET_NAM)
            .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return Timestamp.valueOf(utc);
    }
    private static LocalDateTime gioVietNam(Timestamp utc) {
        return utc.toLocalDateTime().atZone(ZoneOffset.UTC)
            .withZoneSameInstant(GIO_VIET_NAM).toLocalDateTime();
    }

    /** Tổng phân bổ được xác nhận trừ các bút toán hoàn tác phân bổ; chưa bao gồm tín dụng/bù trừ khác. */
    private static final String SQL_KHOAN = """
        SELECT k.MaKhoanThu,k.MaHocVien,h.HoTen,h.SoDienThoaiLienHe,k.LoaiKhoan,k.MaDangKy,k.MaBangTinh,
               k.SoTienGoc,k.HanThanhToan,k.TrangThai,
               COALESCE(dc.SoTien,0) AS DieuChinh,COALESCE(pt.SoTien,0) AS DaThu
        FROM dbo.KhoanPhaiThu k
        JOIN dbo.HocVien h ON h.MaHocVien=k.MaHocVien AND h.MaCoSo=k.MaCoSo
        OUTER APPLY (
            SELECT SUM(d.SoTienDieuChinh) AS SoTien FROM dbo.DieuChinhKhoanThu d
            WHERE d.MaKhoanThu=k.MaKhoanThu AND d.TrangThai='XAC_NHAN'
        ) dc
        OUTER APPLY (
            SELECT SUM(CASE WHEN ct.LoaiBienDong='PHAN_BO' THEN ct.SoTienPhanBo ELSE -ct.SoTienPhanBo END) AS SoTien
            FROM dbo.ChiTietPhieuThu ct
            JOIN dbo.PhieuThu p ON p.MaPhieuThu=ct.MaPhieuThu AND p.MaCoSo=ct.MaCoSo
            WHERE ct.MaKhoanThu=k.MaKhoanThu AND ct.MaCoSo=k.MaCoSo AND p.TrangThai='XAC_NHAN'
        ) pt
        WHERE k.MaCoSo=? ORDER BY k.MaKhoanThu DESC
        """;

    public List<KhoanThu> khoanThu(CoSo coSo) throws SQLException {
        try(Connection c=ketNoi.mo()) { return khoanThu(c,idCoSo(c,coSo)); }
    }
    private List<KhoanThu> khoanThu(Connection c,int maCoSo) throws SQLException {
        List<KhoanThu> result=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement(SQL_KHOAN)) {
            s.setInt(1,maCoSo);
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) {
                    Date han=r.getDate("HanThanhToan");
                    result.add(new KhoanThu(r.getInt("MaKhoanThu"),r.getInt("MaHocVien"),
                        r.getString("HoTen"),r.getString("LoaiKhoan"),so(r,"SoTienGoc"),
                        so(r,"DieuChinh"),so(r,"DaThu"),han==null?null:han.toLocalDate(),r.getString("TrangThai"),
                        (Integer)r.getObject("MaDangKy"),(Integer)r.getObject("MaBangTinh"),
                        r.getString("SoDienThoaiLienHe")));
                }
            }
        }
        return result;
    }

    public List<PhieuThu> phieuThu(CoSo coSo, YearMonth thang) throws SQLException {
        return phieuThu(coSo,thang.atDay(1),thang.plusMonths(1).atDay(1));
    }
    /** Khoảng ngày địa phương Việt Nam: từ ngày đầu, trước ngày kết thúc. */
    public List<PhieuThu> phieuThu(CoSo coSo,LocalDate tuNgay,LocalDate denTruoc) throws SQLException {
        if(tuNgay==null||denTruoc==null||!tuNgay.isBefore(denTruoc))
            throw new IllegalArgumentException("Khoảng ngày thu không hợp lệ.");
        String sql="""
            SELECT p.MaPhieuThu,p.SoPhieuThu,h.HoTen,p.TongTien,p.NgayThu,
                   p.HinhThucThanhToan,p.TrangThai,p.GhiChu,p.NgayHuy,p.LyDoHuy,
                   COALESCE(t.TenDangNhap,CONCAT('#',p.MaTaiKhoan)) AS TaiKhoanThu
            FROM dbo.PhieuThu p JOIN dbo.HocVien h
              ON h.MaHocVien=p.MaHocVien AND h.MaCoSo=p.MaCoSo
            LEFT JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=p.MaTaiKhoan
            WHERE p.MaCoSo=? AND p.NgayThu>=? AND p.NgayThu<?
            ORDER BY p.NgayThu DESC,p.MaPhieuThu DESC
            """;
        try(Connection c=ketNoi.mo()) {
            int maCoSo=idCoSo(c,coSo);
            Timestamp tu=dauNgayUtc(tuNgay);
            Timestamp den=dauNgayUtc(denTruoc);
            Map<Integer,List<String>> noiDung=noiDungPhieuThu(c,maCoSo,tu,den);
            List<PhieuThu> result=new ArrayList<>();
            try(PreparedStatement s=c.prepareStatement(sql)) {
                s.setInt(1,maCoSo);s.setTimestamp(2,tu);s.setTimestamp(3,den);
                try(ResultSet r=s.executeQuery()) {
                    while(r.next()){
                        int maPhieu=r.getInt("MaPhieuThu");
                        result.add(new PhieuThu(maPhieu,r.getString("SoPhieuThu"),
                            r.getString("HoTen"),so(r,"TongTien"),gioVietNam(r.getTimestamp("NgayThu")),
                            r.getString("HinhThucThanhToan"),r.getString("TrangThai"),
                            r.getString("TaiKhoanThu"),
                            String.join("; ",noiDung.getOrDefault(maPhieu,List.of())),
                            r.getString("GhiChu"),r.getTimestamp("NgayHuy")==null?null:
                                gioVietNam(r.getTimestamp("NgayHuy")),r.getString("LyDoHuy")));
                    }
                }
            }
            return result;
        }
    }

    /** Đọc đúng số tiền từng khoản từ chi tiết phiếu, không suy diễn theo tổng phiếu. */
    public List<PhanBoPhieu> phanBoPhieu(CoSo coSo,int maPhieu) throws SQLException {
        try(Connection c=ketNoi.mo()) {
            int maCoSo=idCoSo(c,coSo);
            List<PhanBoPhieu> result=new ArrayList<>();
            try(PreparedStatement s=c.prepareStatement("""
                SELECT ct.MaKhoanThu,ct.SoTienPhanBo,ct.LoaiBienDong,
                       k.LoaiKhoan,kh.TenKhoaHoc
                FROM dbo.PhieuThu p
                JOIN dbo.ChiTietPhieuThu ct ON ct.MaPhieuThu=p.MaPhieuThu AND ct.MaCoSo=p.MaCoSo
                JOIN dbo.KhoanPhaiThu k ON k.MaKhoanThu=ct.MaKhoanThu AND k.MaCoSo=ct.MaCoSo
                LEFT JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=k.MaDangKy AND d.MaCoSo=k.MaCoSo
                LEFT JOIN dbo.KhoaHoc kh ON kh.MaKhoaHoc=d.MaKhoaHoc AND kh.MaCoSo=d.MaCoSo
                WHERE p.MaPhieuThu=? AND p.MaCoSo=?
                ORDER BY ct.MaChiTietPhieuThu
                """)) {
                s.setInt(1,maPhieu);s.setInt(2,maCoSo);
                try(ResultSet r=s.executeQuery()) {while(r.next()) {
                    String noiDung="HOC_PHI_KHOA".equals(r.getString("LoaiKhoan"))
                        ?"Học phí khóa "+(r.getString("TenKhoaHoc")==null
                            ?"#"+r.getInt("MaKhoanThu"):r.getString("TenKhoaHoc"))
                        :"Khoản #"+r.getInt("MaKhoanThu")+" ("+r.getString("LoaiKhoan")+")";
                    result.add(new PhanBoPhieu(r.getInt("MaKhoanThu"),noiDung,
                        r.getBigDecimal("SoTienPhanBo"),r.getString("LoaiBienDong")));
                }}
            }
            return result;
        }
    }

    /** Lịch sử phân bổ theo từng khóa của học viên, gồm cả phiếu đã hủy để đối soát. */
    public List<LichSuThu> lichSuThuHocVien(CoSo coSo,int maHocVien) throws SQLException {
        try(Connection c=ketNoi.mo();PreparedStatement s=c.prepareStatement("""
            SELECT p.MaPhieuThu,p.SoPhieuThu,p.NgayThu,p.TrangThai,
                   kh.TenKhoaHoc,kt.MaKhoanThu,ct.SoTienPhanBo,ct.LoaiBienDong
            FROM dbo.PhieuThu p
            JOIN dbo.ChiTietPhieuThu ct ON ct.MaPhieuThu=p.MaPhieuThu AND ct.MaCoSo=p.MaCoSo
            JOIN dbo.KhoanPhaiThu kt ON kt.MaKhoanThu=ct.MaKhoanThu AND kt.MaCoSo=ct.MaCoSo
            LEFT JOIN dbo.DangKyKhoaHoc dk ON dk.MaDangKy=kt.MaDangKy AND dk.MaCoSo=kt.MaCoSo
            LEFT JOIN dbo.KhoaHoc kh ON kh.MaKhoaHoc=dk.MaKhoaHoc AND kh.MaCoSo=dk.MaCoSo
            WHERE p.MaCoSo=? AND p.MaHocVien=?
            ORDER BY p.NgayThu DESC,p.MaPhieuThu DESC,ct.MaChiTietPhieuThu
            """)) {
            s.setInt(1,idCoSo(c,coSo));s.setInt(2,maHocVien);
            List<LichSuThu> result=new ArrayList<>();
            try(ResultSet r=s.executeQuery()){while(r.next()){
                BigDecimal amount=r.getBigDecimal("SoTienPhanBo");
                if("HOAN_TAC_PHAN_BO".equals(r.getString("LoaiBienDong")))amount=amount.negate();
                String khoa=r.getString("TenKhoaHoc");
                result.add(new LichSuThu(r.getInt("MaPhieuThu"),r.getString("SoPhieuThu"),
                    gioVietNam(r.getTimestamp("NgayThu")),
                    khoa==null?"Khoản #"+r.getInt("MaKhoanThu"):khoa,
                    amount,r.getString("TrangThai")));
            }}
            return result;
        }
    }
    public List<UuDaiDangKy> uuDaiHocVien(int maHocVien) throws SQLException {
        try(Connection c=ketNoi.mo();PreparedStatement s=c.prepareStatement("""
            SELECT ct.MaDangKy,ct.TenChuongTrinhSnapshot,ct.SoTienGiam
            FROM dbo.ChiTietGiamGiaDangKy ct
            JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=ct.MaDangKy AND d.MaCoSo=ct.MaCoSo
            WHERE d.MaHocVien=? AND d.MaCoSo=?
            ORDER BY ct.MaDangKy,ct.ThuTuApDung
            """)) {
            s.setInt(1,maHocVien);s.setInt(2,idCoSo(c,CoSo.TRUNG_TAM));
            List<UuDaiDangKy> result=new ArrayList<>();
            try(ResultSet r=s.executeQuery()){while(r.next())result.add(new UuDaiDangKy(
                r.getInt(1),r.getString(2),r.getBigDecimal(3)));}
            return result;
        }
    }

    /** Phân bổ đã xác nhận trong kỳ, một dòng cho mỗi khoản của phiếu. */
    public List<DongThuBaoCao> baoCaoThu(CoSo coSo,LocalDate tuNgay,
                                          LocalDate denTruoc)throws SQLException {
        if(tuNgay==null||denTruoc==null||!tuNgay.isBefore(denTruoc))
            throw new IllegalArgumentException("Khoảng báo cáo không hợp lệ.");
        try(Connection c=ketNoi.mo();PreparedStatement s=c.prepareStatement("""
            SELECT p.NgayThu,ct.SoTienPhanBo,ct.LoaiBienDong,
                   kt.LoaiKhoan,kh.TenKhoaHoc,
                   COALESCE(t.TenDangNhap,CONCAT('#',p.MaTaiKhoan)) AS NhanVien
            FROM dbo.PhieuThu p
            JOIN dbo.ChiTietPhieuThu ct ON ct.MaPhieuThu=p.MaPhieuThu AND ct.MaCoSo=p.MaCoSo
            JOIN dbo.KhoanPhaiThu kt ON kt.MaKhoanThu=ct.MaKhoanThu AND kt.MaCoSo=ct.MaCoSo
            LEFT JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=kt.MaDangKy AND d.MaCoSo=kt.MaCoSo
            LEFT JOIN dbo.KhoaHoc kh ON kh.MaKhoaHoc=d.MaKhoaHoc AND kh.MaCoSo=d.MaCoSo
            LEFT JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=p.MaTaiKhoan
            WHERE p.MaCoSo=? AND p.TrangThai='XAC_NHAN'
              AND p.NgayThu>=? AND p.NgayThu<?
            ORDER BY p.NgayThu,ct.MaChiTietPhieuThu
            """)) {
            s.setInt(1,idCoSo(c,coSo));s.setTimestamp(2,dauNgayUtc(tuNgay));
            s.setTimestamp(3,dauNgayUtc(denTruoc));
            List<DongThuBaoCao> result=new ArrayList<>();
            try(ResultSet r=s.executeQuery()){while(r.next()){
                BigDecimal amount=r.getBigDecimal("SoTienPhanBo");
                if("HOAN_TAC_PHAN_BO".equals(r.getString("LoaiBienDong")))amount=amount.negate();
                String khoa=r.getString("TenKhoaHoc");
                if(khoa==null)khoa=r.getString("LoaiKhoan");
                result.add(new DongThuBaoCao(gioVietNam(r.getTimestamp("NgayThu"))
                    .toLocalDate(),khoa,r.getString("NhanVien"),amount));
            }}
            return result;
        }
    }

    /** Thu nhiều khóa trong cùng một phiếu; mọi phân bổ dùng chung một transaction. */
    public int lapPhieuNhieuKhoan(CoSo coSo,int maHocVien,BigDecimal soTien,
            List<TaiChinhDangKyRepository.PhanBo> phanBo,String hinhThuc,
            int taiKhoan,String ghiChu,UUID token) throws SQLException {
        if(coSo!=CoSo.TRUNG_TAM)throw new IllegalArgumentException("Chỉ hỗ trợ thu nhiều khóa Trung tâm.");
        try(Connection c=ketNoi.mo()) {
            int[] maPhieu=new int[1];
            giaoDich(c,()-> maPhieu[0]=new TaiChinhDangKyRepository().lapPhieu(c,
                idCoSo(c,coSo),maHocVien,soTien,phanBo,hinhThuc,taiKhoan,ghiChu,token));
            return maPhieu[0];
        }
    }

    /** Hủy mềm phiếu chưa có bút toán hoàn tác phụ thuộc; công nợ sẽ tự tính lại. */
    public void huyPhieu(CoSo coSo,int maPhieu,int taiKhoan,String lyDo) throws SQLException {
        if(lyDo==null||lyDo.isBlank()||lyDo.trim().length()>500)
            throw new IllegalArgumentException("Cần lý do hủy phiếu từ 1 đến 500 ký tự.");
        try(Connection c=ketNoi.mo()) {giaoDich(c,()->{
            int maCoSo=idCoSo(c,coSo);kiemTraTaiKhoan(c,maCoSo,taiKhoan);
            try(PreparedStatement s=c.prepareStatement("""
                SELECT TrangThai FROM dbo.PhieuThu WITH (UPDLOCK,HOLDLOCK)
                WHERE MaPhieuThu=? AND MaCoSo=?
                """)) {
                s.setInt(1,maPhieu);s.setInt(2,maCoSo);
                try(ResultSet r=s.executeQuery()) {
                    if(!r.next()||!"XAC_NHAN".equals(r.getString(1)))
                        throw new SQLException("Phiếu không tồn tại, đã hủy hoặc chưa xác nhận.");
                }
            }
            try(PreparedStatement s=c.prepareStatement("""
                SELECT COUNT(*) FROM dbo.ChiTietPhieuThu ct
                WHERE ct.MaPhieuThu=? AND ct.MaCoSo=? AND (
                    ct.LoaiBienDong<>'PHAN_BO' OR EXISTS(
                        SELECT 1 FROM dbo.ChiTietPhieuThu h
                        WHERE h.MaChiTietNguon=ct.MaChiTietPhieuThu))
                """)) {
                s.setInt(1,maPhieu);s.setInt(2,maCoSo);
                try(ResultSet r=s.executeQuery()) {r.next();if(r.getInt(1)>0)
                    throw new SQLException("Phiếu có bút toán hoàn tác/phụ thuộc; cần đối soát trước khi hủy.");}
            }
            try(PreparedStatement s=c.prepareStatement("""
                UPDATE dbo.PhieuThu SET TrangThai='HUY',NgayHuy=SYSUTCDATETIME(),
                    MaTaiKhoanHuy=?,LyDoHuy=? WHERE MaPhieuThu=? AND MaCoSo=?
                    AND TrangThai='XAC_NHAN'
                """)) {
                s.setInt(1,taiKhoan);s.setString(2,lyDo.trim());
                s.setInt(3,maPhieu);s.setInt(4,maCoSo);
                if(s.executeUpdate()!=1)throw new SQLException("Không hủy được phiếu.");
            }
        });}
    }

    /** Nội dung lấy từ các khoản thực sự được phân bổ, không suy diễn từ ghi chú. */
    private static Map<Integer,List<String>> noiDungPhieuThu(Connection c,int maCoSo,
            Timestamp tuNgay,Timestamp denNgay) throws SQLException {
        String sql="""
            SELECT p.MaPhieuThu,ct.MaKhoanThu,ct.LoaiBienDong,
                   kt.LoaiKhoan,k.TenKhoaHoc
            FROM dbo.PhieuThu p
            JOIN dbo.ChiTietPhieuThu ct ON ct.MaPhieuThu=p.MaPhieuThu
              AND ct.MaCoSo=p.MaCoSo
            JOIN dbo.KhoanPhaiThu kt ON kt.MaKhoanThu=ct.MaKhoanThu
              AND kt.MaCoSo=ct.MaCoSo
            LEFT JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=kt.MaDangKy
              AND d.MaCoSo=kt.MaCoSo
            LEFT JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=d.MaKhoaHoc
              AND k.MaCoSo=d.MaCoSo
            WHERE p.MaCoSo=? AND p.NgayThu>=? AND p.NgayThu<?
            ORDER BY p.MaPhieuThu,ct.MaChiTietPhieuThu
            """;
        Map<Integer,List<String>> result=new HashMap<>();
        try(PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setInt(1,maCoSo);ps.setTimestamp(2,tuNgay);ps.setTimestamp(3,denNgay);
            try(ResultSet rs=ps.executeQuery()) {
                while(rs.next()) {
                    String ten=switch(rs.getString("LoaiKhoan")) {
                        case "HOC_PHI_KHOA" -> "Học phí khóa "
                            +(rs.getString("TenKhoaHoc")==null?"#"+rs.getInt("MaKhoanThu"):
                                rs.getString("TenKhoaHoc"));
                        case "HOC_PHI_THANG" -> "Học phí tháng, khoản #"+rs.getInt("MaKhoanThu");
                        case "TIEN_AN" -> "Tiền ăn, khoản #"+rs.getInt("MaKhoanThu");
                        default -> "Khoản #"+rs.getInt("MaKhoanThu");
                    };
                    if("HOAN_TAC_PHAN_BO".equals(rs.getString("LoaiBienDong")))
                        ten="Hoàn tác " + ten.toLowerCase(java.util.Locale.forLanguageTag("vi-VN"));
                    result.computeIfAbsent(rs.getInt("MaPhieuThu"),ignored->new ArrayList<>())
                        .add(ten);
                }
            }
        }
        return result;
    }

    public List<LuaChon> taiKhoan(CoSo coSo) throws SQLException {
        String sql="""
            SELECT t.MaTaiKhoan,t.TenDangNhap FROM dbo.TaiKhoan t
            JOIN dbo.TaiKhoanCoSo tc ON tc.MaTaiKhoan=t.MaTaiKhoan
            WHERE tc.MaCoSo=? AND tc.TrangThai=1 AND t.TrangThai=1
            ORDER BY t.TenDangNhap
            """;
        try(Connection c=ketNoi.mo(); PreparedStatement s=c.prepareStatement(sql)){
            s.setInt(1,idCoSo(c,coSo));List<LuaChon> result=new ArrayList<>();
            try(ResultSet r=s.executeQuery()) {while(r.next()) result.add(new LuaChon(r.getInt(1),r.getString(2)));}
            return result;
        }
    }

    public List<LuaChon> lopMamNon() throws SQLException {
        String sql="""
            SELECT l.MaLop, l.TenLop + N' – ' + n.TenNamHoc AS Ten
            FROM dbo.LopMamNon l JOIN dbo.NamHoc n ON n.MaNamHoc=l.MaNamHoc AND n.MaCoSo=l.MaCoSo
            WHERE l.MaCoSo=? AND l.TrangThai=1
            ORDER BY n.TuNgay DESC,l.TenLop
            """;
        try(Connection c=ketNoi.mo();PreparedStatement s=c.prepareStatement(sql)) {
            s.setInt(1,idMamNon(c));List<LuaChon> result=new ArrayList<>();
            try(ResultSet r=s.executeQuery()){while(r.next())result.add(new LuaChon(r.getInt(1),r.getString(2)));}
            return result;
        }
    }

    public List<DangKyChuaThu> dangKyChuaThu() throws SQLException {
        String sql="""
            SELECT d.MaDangKy,h.HoTen,k.TenKhoaHoc,d.HocPhiThoaThuan,d.SoTienGiam,d.HocPhiSauGiam
            FROM dbo.DangKyKhoaHoc d
            JOIN dbo.HocVien h ON h.MaHocVien=d.MaHocVien AND h.MaCoSo=d.MaCoSo
            JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo
            WHERE d.MaCoSo=? AND d.TrangThai<>'HUY' AND d.HocPhiSauGiam>0
              AND NOT EXISTS(SELECT 1 FROM dbo.KhoanPhaiThu t WHERE t.MaDangKy=d.MaDangKy)
            ORDER BY d.MaDangKy DESC
            """;
        try(Connection c=ketNoi.mo();PreparedStatement s=c.prepareStatement(sql)) {
            s.setInt(1,idCoSo(c,CoSo.TRUNG_TAM));List<DangKyChuaThu> result=new ArrayList<>();
            try(ResultSet r=s.executeQuery()){
                while(r.next())result.add(new DangKyChuaThu(r.getInt(1),r.getString(2),r.getString(3),
                    so(r,"HocPhiThoaThuan"),so(r,"SoTienGiam"),so(r,"HocPhiSauGiam")));
            }
            return result;
        }
    }

    /**
     * Hiển thị học phí theo học viên, môn, khóa và lớp hiện tại. Không lọc mất
     * đăng ký đã lập khoản thu; khoản chưa lập thu có số đã đóng bằng 0.
     * Dữ liệu thanh toán lấy từ các phiếu đã xác nhận, không lấy tổng phiếu
     * của học viên để tránh đếm trùng giữa các khóa.
     */
    public List<HocPhiTrungTam> hocPhiTrungTam() throws SQLException {
        String sql="""
            SELECT d.MaDangKy,d.MaHocVien,h.HoTen AS HocVien,h.SoDienThoaiLienHe,
                   m.TenMonHoc, k.TenKhoaHoc,
                   COALESCE(l.TenLop, N'Chưa xếp lớp') AS TenLop,
                   d.HocPhiThoaThuan, d.SoTienGiam, d.HocPhiSauGiam,
                   COALESCE(paid.DaDong, 0) AS DaDong,
                   latest.SoPhieuGanNhat,latest.NgayThuGanNhat,latest.SoTienLanThu
            FROM dbo.DangKyKhoaHoc d
            JOIN dbo.HocVien h ON h.MaHocVien=d.MaHocVien AND h.MaCoSo=d.MaCoSo
            JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo
            JOIN dbo.MonHoc m ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=k.MaCoSo
            OUTER APPLY (
                SELECT TOP (1) x.MaLop
                FROM dbo.XepLopTrungTam x
                WHERE x.MaDangKy=d.MaDangKy AND x.MaCoSo=d.MaCoSo
                  AND x.TrangThai='HIEU_LUC'
                ORDER BY x.TuNgay DESC, x.MaXepLop DESC
            ) x
            LEFT JOIN dbo.LopTrungTam l ON l.MaLop=x.MaLop AND l.MaCoSo=d.MaCoSo
            OUTER APPLY (
                SELECT SUM(CASE WHEN ct.LoaiBienDong='PHAN_BO'
                                THEN ct.SoTienPhanBo ELSE -ct.SoTienPhanBo END) AS DaDong
                FROM dbo.KhoanPhaiThu t
                JOIN dbo.ChiTietPhieuThu ct ON ct.MaKhoanThu=t.MaKhoanThu
                  AND ct.MaCoSo=t.MaCoSo
                JOIN dbo.PhieuThu p ON p.MaPhieuThu=ct.MaPhieuThu
                  AND p.MaCoSo=ct.MaCoSo AND p.TrangThai='XAC_NHAN'
                WHERE t.MaDangKy=d.MaDangKy AND t.MaCoSo=d.MaCoSo
                  AND t.TrangThai='HIEU_LUC'
            ) paid
            OUTER APPLY (
                SELECT TOP (1) p.SoPhieuThu AS SoPhieuGanNhat,
                       p.NgayThu AS NgayThuGanNhat,
                       SUM(CASE WHEN ct.LoaiBienDong='PHAN_BO'
                                THEN ct.SoTienPhanBo ELSE -ct.SoTienPhanBo END) AS SoTienLanThu
                FROM dbo.KhoanPhaiThu t
                JOIN dbo.ChiTietPhieuThu ct ON ct.MaKhoanThu=t.MaKhoanThu
                  AND ct.MaCoSo=t.MaCoSo
                JOIN dbo.PhieuThu p ON p.MaPhieuThu=ct.MaPhieuThu
                  AND p.MaCoSo=ct.MaCoSo AND p.TrangThai='XAC_NHAN'
                WHERE t.MaDangKy=d.MaDangKy AND t.MaCoSo=d.MaCoSo
                  AND t.TrangThai='HIEU_LUC'
                GROUP BY p.MaPhieuThu,p.SoPhieuThu,p.NgayThu
                HAVING SUM(CASE WHEN ct.LoaiBienDong='PHAN_BO'
                                THEN ct.SoTienPhanBo ELSE -ct.SoTienPhanBo END)>0
                ORDER BY p.NgayThu DESC,p.MaPhieuThu DESC
            ) latest
            WHERE d.MaCoSo=? AND d.TrangThai<>'HUY'
            ORDER BY d.MaDangKy DESC
            """;
        try(Connection c=ketNoi.mo();PreparedStatement s=c.prepareStatement(sql)) {
            s.setInt(1,idCoSo(c,CoSo.TRUNG_TAM));
            List<HocPhiTrungTam> result=new ArrayList<>();
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) {
                    BigDecimal due=so(r,"HocPhiSauGiam"), paid=so(r,"DaDong");
                    result.add(new HocPhiTrungTam(r.getInt("MaDangKy"),r.getString("HocVien"),
                        r.getString("TenMonHoc"),r.getString("TenKhoaHoc"),r.getString("TenLop"),
                        so(r,"HocPhiThoaThuan"),so(r,"SoTienGiam"),due,paid,
                        due.subtract(paid),r.getString("SoPhieuGanNhat"),
                        r.getTimestamp("NgayThuGanNhat")==null?null:
                            gioVietNam(r.getTimestamp("NgayThuGanNhat")),
                        r.getBigDecimal("SoTienLanThu"),r.getInt("MaHocVien"),
                        r.getString("SoDienThoaiLienHe")));
                }
            }
            return result;
        }
    }

    public List<BangTinhChot> bangTinhChot() throws SQLException {
        String sql="""
            SELECT b.MaBangTinh,h.HoTen,CONCAT(t.Nam,'-',RIGHT('0'+CAST(t.Thang AS VARCHAR(2)),2)) AS Ky,
                   b.TongHocPhi,b.TongTienAn
            FROM dbo.BangTinhPhiMamNon b
            JOIN dbo.HocVien h ON h.MaHocVien=b.MaHocVien AND h.MaCoSo=b.MaCoSo
            JOIN dbo.ThangMamNon t ON t.MaThang=b.MaThang AND t.MaCoSo=b.MaCoSo
            WHERE b.MaCoSo=? AND b.TrangThai='CHOT'
            ORDER BY t.Nam DESC,t.Thang DESC,b.MaBangTinh DESC
            """;
        try(Connection c=ketNoi.mo();PreparedStatement s=c.prepareStatement(sql)) {
            s.setInt(1,idMamNon(c));List<BangTinhChot> result=new ArrayList<>();
            try(ResultSet r=s.executeQuery()) {
                while(r.next())result.add(new BangTinhChot(r.getInt(1),r.getString(2),r.getString(3),
                    so(r,"TongHocPhi"),so(r,"TongTienAn")));
            }
            return result;
        }
    }

    public List<MucPhi> mucPhiMamNon() throws SQLException {
        String sql="""
            SELECT m.MaMucHocPhi AS Id,l.TenLop AS Lop,N'Học phí tháng' AS Loai,
                   m.ThangBatDau AS ApDung,m.HocPhiThang AS DonGia
            FROM dbo.MucHocPhiLop m JOIN dbo.LopMamNon l ON l.MaLop=m.MaLop
            WHERE l.MaCoSo=?
            UNION ALL
            SELECT a.MaDonGiaTienAn AS Id,l.TenLop AS Lop,N'Tiền ăn/ngày' AS Loai,
                   a.ThangBatDau AS ApDung,a.DonGiaNgay AS DonGia
            FROM dbo.DonGiaTienAn a JOIN dbo.LopMamNon l ON l.MaLop=a.MaLop
            WHERE l.MaCoSo=?
            ORDER BY ApDung DESC,Lop
            """;
        try(Connection c=ketNoi.mo();PreparedStatement s=c.prepareStatement(sql)) {
            int id=idMamNon(c);s.setInt(1,id);s.setInt(2,id);
            List<MucPhi> result=new ArrayList<>();
            try(ResultSet r=s.executeQuery()) {
                while(r.next())result.add(new MucPhi(r.getInt("Id"),r.getString("Lop"),r.getString("Loai"),
                    r.getDate("ApDung").toLocalDate(),so(r,"DonGia")));
            }
            return result;
        }
    }

    private static void thietLapHan(PreparedStatement s,int i,LocalDate han) throws SQLException {
        if(han==null)s.setNull(i,Types.DATE);else s.setDate(i,Date.valueOf(han));
    }
    private static void giaoDich(Connection c, SQLOperation work) throws SQLException {
        boolean auto=c.getAutoCommit();int isolate=c.getTransactionIsolation();
        try {
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            work.run();c.commit();
        } catch(Exception ex) {
            try{c.rollback();}catch(SQLException rollback){ex.addSuppressed(rollback);}
            if(ex instanceof SQLException e)throw e;
            throw new SQLException(ex.getMessage(),ex);
        } finally {
            c.setTransactionIsolation(isolate);c.setAutoCommit(auto);
        }
    }
    @FunctionalInterface private interface SQLOperation { void run() throws Exception; }

    public void lapKhoanHocPhiKhoa(int maDangKy,LocalDate han) throws SQLException {
        String select="""
            SELECT d.MaHocVien,d.MaCoSo,d.HocPhiSauGiam
            FROM dbo.DangKyKhoaHoc d WITH (UPDLOCK,HOLDLOCK)
            WHERE d.MaDangKy=? AND d.TrangThai<>'HUY'
            """;
        try(Connection c=ketNoi.mo()) { giaoDich(c,()->{
            int coSo=idCoSo(c,CoSo.TRUNG_TAM),hocVien;BigDecimal money;
            try(PreparedStatement s=c.prepareStatement(select)) {
                s.setInt(1,maDangKy);
                try(ResultSet r=s.executeQuery()){
                    if(!r.next()||r.getInt("MaCoSo")!=coSo)throw new SQLException("Đăng ký không thuộc Trung tâm hoặc đã hủy.");
                    hocVien=r.getInt("MaHocVien");money=so(r,"HocPhiSauGiam");
                }
            }
            if(money.signum()<=0)throw new SQLException("Đăng ký không có khoản học phí phải thu.");
            try(PreparedStatement check=c.prepareStatement("SELECT 1 FROM dbo.KhoanPhaiThu WHERE MaDangKy=?")){
                check.setInt(1,maDangKy);try(ResultSet r=check.executeQuery()){
                    if(r.next())throw new SQLException("Đăng ký này đã có khoản phải thu (không tạo trùng). ");
                }
            }
            try(PreparedStatement s=c.prepareStatement("""
                INSERT dbo.KhoanPhaiThu(MaHocVien,MaCoSo,LoaiKhoan,MaDangKy,SoTienGoc,HanThanhToan)
                VALUES(?,?,'HOC_PHI_KHOA',?,?,?)
                """)) {
                s.setInt(1,hocVien);s.setInt(2,coSo);s.setInt(3,maDangKy);
                s.setBigDecimal(4,money);thietLapHan(s,5,han);s.executeUpdate();
            }
        }); }
    }

    /** Phát sinh từ bảng CHỐT; chỉ tạo phần chưa có, các loại có mức tiền 0 được bỏ qua. */
    public int lapKhoanMamNon(int maBangTinh,boolean hocPhi,boolean tienAn,LocalDate han) throws SQLException {
        if(!hocPhi&&!tienAn)throw new IllegalArgumentException("Chọn ít nhất một loại khoản thu.");
        String sql="SELECT MaHocVien,MaCoSo,TongHocPhi,TongTienAn FROM dbo.BangTinhPhiMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaBangTinh=? AND TrangThai='CHOT'";
        int[] count={0};
        try(Connection c=ketNoi.mo()) { giaoDich(c,()->{
            int coSo=idMamNon(c),hocVien;BigDecimal hp,an;
            try(PreparedStatement s=c.prepareStatement(sql)) {
                s.setInt(1,maBangTinh);
                try(ResultSet r=s.executeQuery()){
                    if(!r.next()||r.getInt("MaCoSo")!=coSo)throw new SQLException("Bảng tính không thuộc Mầm non hoặc chưa chốt.");
                    hocVien=r.getInt("MaHocVien");hp=so(r,"TongHocPhi");an=so(r,"TongTienAn");
                }
            }
            if(hocPhi && hp.signum()>0 && !coKhoan(c,maBangTinh,"HOC_PHI_THANG")){
                themKhoanMamNon(c,maBangTinh,hocVien,coSo,"HOC_PHI_THANG",hp,han);count[0]++;
            }
            if(tienAn && an.signum()>0 && !coKhoan(c,maBangTinh,"TIEN_AN")){
                themKhoanMamNon(c,maBangTinh,hocVien,coSo,"TIEN_AN",an,han);count[0]++;
            }
            if(count[0]==0)throw new SQLException("Không có khoản mới: đã phát sinh trước đó hoặc số tiền bằng 0.");
        }); }
        return count[0];
    }
    private boolean coKhoan(Connection c,int bang,String loai) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM dbo.KhoanPhaiThu WHERE MaBangTinh=? AND LoaiKhoan=?")){
            s.setInt(1,bang);s.setString(2,loai);try(ResultSet r=s.executeQuery()){return r.next();}
        }
    }
    private void themKhoanMamNon(Connection c,int bang,int hocVien,int coSo,String loai,BigDecimal soTien,LocalDate han) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("""
            INSERT dbo.KhoanPhaiThu(MaHocVien,MaCoSo,LoaiKhoan,MaBangTinh,SoTienGoc,HanThanhToan)
            VALUES(?,?,?,?,?,?)
            """)){
            s.setInt(1,hocVien);s.setInt(2,coSo);s.setString(3,loai);s.setInt(4,bang);
            s.setBigDecimal(5,soTien);thietLapHan(s,6,han);s.executeUpdate();
        }
    }

    public void taoMucPhi(int maLop,boolean tienAn,LocalDate ngay,BigDecimal amount,int tk) throws SQLException {
        String table=tienAn?"dbo.DonGiaTienAn":"dbo.MucHocPhiLop";
        String money=tienAn?"DonGiaNgay":"HocPhiThang";
        String select="SELECT MaCoSo FROM dbo.LopMamNon WHERE MaLop=? AND TrangThai=1";
        try(Connection c=ketNoi.mo()) { giaoDich(c,()->{
            int coSo=idMamNon(c);
            try(PreparedStatement s=c.prepareStatement(select)) {
                s.setInt(1,maLop);try(ResultSet r=s.executeQuery()) {
                    if(!r.next()||r.getInt(1)!=coSo)throw new SQLException("Lớp không thuộc Mầm non hoặc đã ngừng.");
                }
            }
            kiemTraTaiKhoan(c,coSo,tk);
            try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM "+table+" WHERE MaLop=? AND ThangBatDau=?")){
                s.setInt(1,maLop);s.setDate(2,Date.valueOf(ngay));
                try(ResultSet r=s.executeQuery()){
                    if(r.next())throw new SQLException("Lớp đã có mức phí áp dụng từ tháng này; chọn kỳ áp dụng mới.");
                }
            }
            String sql="INSERT "+table+" (MaLop,ThangBatDau,"+money+",MaTaiKhoanThietLap) VALUES(?,?,?,?)";
            try(PreparedStatement s=c.prepareStatement(sql)) {
                s.setInt(1,maLop);s.setDate(2,Date.valueOf(ngay));s.setBigDecimal(3,amount);
                s.setInt(4,tk);s.executeUpdate();
            }
        }); }
    }

    private void kiemTraTaiKhoan(Connection c,int coSo,int tk) throws SQLException {
        String sql="""
            SELECT 1 FROM dbo.TaiKhoanCoSo cs JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=cs.MaTaiKhoan
            WHERE cs.MaCoSo=? AND cs.MaTaiKhoan=? AND cs.TrangThai=1 AND t.TrangThai=1
            """;
        try(PreparedStatement s=c.prepareStatement(sql)){
            s.setInt(1,coSo);s.setInt(2,tk);try(ResultSet r=s.executeQuery()){
                if(!r.next())throw new SQLException("Tài khoản không hoạt động tại cơ sở này.");
            }
        }
    }

    /** Thu một khoản trên một phiếu. Khóa khoản thu trong transaction để không vượt nợ khi thu đồng thời. */
    public void lapPhieu(CoSo coSo,int khoan,BigDecimal amount,String method,int taiKhoan,String note) throws SQLException {
        if(!List.of("TIEN_MAT","CHUYEN_KHOAN","KHAC").contains(method))
            throw new IllegalArgumentException("Hình thức thanh toán không hợp lệ.");
        if(note!=null&&note.length()>500)throw new IllegalArgumentException("Ghi chú quá 500 ký tự.");
        try(Connection c=ketNoi.mo()) { giaoDich(c,()->{
            int coSoId=idCoSo(c,coSo);kiemTraTaiKhoan(c,coSoId,taiKhoan);
            int hocVien;BigDecimal goc;
            try(PreparedStatement s=c.prepareStatement("""
                SELECT MaHocVien,MaCoSo,SoTienGoc FROM dbo.KhoanPhaiThu WITH (UPDLOCK,HOLDLOCK)
                WHERE MaKhoanThu=? AND TrangThai='HIEU_LUC'
                """)){
                s.setInt(1,khoan);try(ResultSet r=s.executeQuery()){
                    if(!r.next()||r.getInt("MaCoSo")!=coSoId)throw new SQLException("Khoản thu không thuộc cơ sở này hoặc đã hủy.");
                    hocVien=r.getInt("MaHocVien");goc=so(r,"SoTienGoc");
                }
            }
            BigDecimal adjustments;
            try(PreparedStatement s=c.prepareStatement("""
                SELECT COALESCE(SUM(SoTienDieuChinh),0) FROM dbo.DieuChinhKhoanThu
                WHERE MaKhoanThu=? AND TrangThai='XAC_NHAN'
                """)){
                s.setInt(1,khoan);try(ResultSet r=s.executeQuery()){r.next();adjustments=r.getBigDecimal(1);}
            }
            BigDecimal paid;
            try(PreparedStatement s=c.prepareStatement("""
                SELECT COALESCE(SUM(CASE WHEN ct.LoaiBienDong='PHAN_BO' THEN ct.SoTienPhanBo ELSE -ct.SoTienPhanBo END),0)
                FROM dbo.ChiTietPhieuThu ct JOIN dbo.PhieuThu p
                  ON p.MaPhieuThu=ct.MaPhieuThu AND p.MaCoSo=ct.MaCoSo
                WHERE ct.MaKhoanThu=? AND ct.MaCoSo=? AND p.TrangThai='XAC_NHAN'
                """)){
                s.setInt(1,khoan);s.setInt(2,coSoId);try(ResultSet r=s.executeQuery()){r.next();paid=r.getBigDecimal(1);}
            }
            BigDecimal remaining=goc.add(adjustments).subtract(paid);
            if(remaining.signum()<=0)throw new SQLException("Khoản này đã thanh toán đủ.");
            if(amount.signum()<=0 || amount.compareTo(remaining)>0)
                throw new SQLException("Số tiền thu phải > 0 và không vượt công nợ còn lại: "+remaining.toPlainString());
            String receipt="PT-"+UUID.randomUUID().toString().substring(0,24).toUpperCase();
            int receiptId;
            try(PreparedStatement s=c.prepareStatement("""
                INSERT dbo.PhieuThu(SoPhieuThu,MaHocVien,MaCoSo,TongTien,HinhThucThanhToan,MaTaiKhoan,TrangThai,MaGiaoDichNghiepVu,GhiChu)
                VALUES(?,?,?,?,?,?,'XAC_NHAN',?,?)
                """,Statement.RETURN_GENERATED_KEYS)){
                s.setString(1,receipt);s.setInt(2,hocVien);s.setInt(3,coSoId);
                s.setBigDecimal(4,amount);s.setString(5,method);s.setInt(6,taiKhoan);
                s.setString(7,maGiaoDich());s.setString(8,note);s.executeUpdate();
                try(ResultSet r=s.getGeneratedKeys()){
                    if(!r.next())throw new SQLException("Không lấy được mã phiếu thu mới.");receiptId=r.getInt(1);
                }
            }
            try(PreparedStatement s=c.prepareStatement("""
                INSERT dbo.ChiTietPhieuThu(MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo,SoTienPhanBo,LoaiBienDong,MaGiaoDichNghiepVu)
                VALUES(?,?,?,?,?,'PHAN_BO',?)
                """)){
                s.setInt(1,receiptId);s.setInt(2,khoan);s.setInt(3,hocVien);
                s.setInt(4,coSoId);s.setBigDecimal(5,amount);s.setString(6,maGiaoDich());s.executeUpdate();
            }
        }); }
    }

    /**
     * Ghi điều chỉnh có lịch sử, không sửa số gốc và không cho tổng phải thu
     * thấp hơn số đã thanh toán. Khoản và tài khoản phải cùng cơ sở.
     */
    public void dieuChinhKhoan(CoSo coSo,int maKhoan,BigDecimal thayDoi,
                               String lyDo,int taiKhoan) throws SQLException {
        if(thayDoi==null||thayDoi.signum()==0||thayDoi.precision()>18)
            throw new IllegalArgumentException("Số tiền điều chỉnh phải khác 0 và không quá lớn.");
        if(lyDo==null||lyDo.isBlank()||lyDo.length()>500)
            throw new IllegalArgumentException("Lý do điều chỉnh phải có từ 1 đến 500 ký tự.");
        try(Connection c=ketNoi.mo()) { giaoDich(c,()->{
            int coSoId=idCoSo(c,coSo);kiemTraTaiKhoan(c,coSoId,taiKhoan);
            BigDecimal goc;
            try(PreparedStatement s=c.prepareStatement("""
                SELECT MaCoSo,SoTienGoc FROM dbo.KhoanPhaiThu WITH (UPDLOCK,HOLDLOCK)
                WHERE MaKhoanThu=? AND TrangThai='HIEU_LUC'
                """)) {
                s.setInt(1,maKhoan);
                try(ResultSet r=s.executeQuery()) {
                    if(!r.next()||r.getInt("MaCoSo")!=coSoId)
                        throw new SQLException("Khoản thu không thuộc cơ sở này hoặc đã hủy.");
                    goc=so(r,"SoTienGoc");
                }
            }
            BigDecimal dieuChinh;
            try(PreparedStatement s=c.prepareStatement("""
                SELECT COALESCE(SUM(SoTienDieuChinh),0) FROM dbo.DieuChinhKhoanThu
                WHERE MaKhoanThu=? AND TrangThai='XAC_NHAN'
                """)) {
                s.setInt(1,maKhoan);
                try(ResultSet r=s.executeQuery()){r.next();dieuChinh=r.getBigDecimal(1);}
            }
            BigDecimal daThu;
            try(PreparedStatement s=c.prepareStatement("""
                SELECT COALESCE(SUM(CASE WHEN ct.LoaiBienDong='PHAN_BO'
                  THEN ct.SoTienPhanBo ELSE -ct.SoTienPhanBo END),0)
                FROM dbo.ChiTietPhieuThu ct
                JOIN dbo.PhieuThu p ON p.MaPhieuThu=ct.MaPhieuThu
                  AND p.MaCoSo=ct.MaCoSo AND p.TrangThai='XAC_NHAN'
                WHERE ct.MaKhoanThu=? AND ct.MaCoSo=?
                """)) {
                s.setInt(1,maKhoan);s.setInt(2,coSoId);
                try(ResultSet r=s.executeQuery()){r.next();daThu=r.getBigDecimal(1);}
            }
            BigDecimal moi=goc.add(dieuChinh).add(thayDoi);
            if(moi.signum()<0||moi.compareTo(daThu)<0||moi.precision()>18)
                throw new SQLException("Không thể điều chỉnh: phải thu mới thấp hơn tiền đã đóng ("
                    +daThu.toPlainString()+").");
            try(PreparedStatement s=c.prepareStatement("""
                INSERT dbo.DieuChinhKhoanThu
                  (MaKhoanThu,SoTienDieuChinh,LoaiDieuChinh,LyDo,MaTaiKhoan,MaGiaoDichNghiepVu)
                VALUES(?,?,'KHAC',?,?,?)
                """)) {
                s.setInt(1,maKhoan);s.setBigDecimal(2,thayDoi);
                s.setString(3,lyDo.trim());s.setInt(4,taiKhoan);
                s.setString(5,maGiaoDich());s.executeUpdate();
            }
        }); }
    }

    public ThongKe thongKe(CoSo coSo,YearMonth thang) throws SQLException {
        return thongKe(coSo,thang.atDay(1),thang.plusMonths(1).atDay(1));
    }
    public ThongKe thongKe(CoSo coSo,LocalDate tuNgay,LocalDate denTruoc) throws SQLException {
        if(tuNgay==null||denTruoc==null||!tuNgay.isBefore(denTruoc))
            throw new IllegalArgumentException("Khoảng báo cáo không hợp lệ.");
        try(Connection c=ketNoi.mo()) {
            int id=idCoSo(c,coSo);
            BigDecimal thuThang=BigDecimal.ZERO,thuAll=BigDecimal.ZERO,phaiThu=BigDecimal.ZERO,no=BigDecimal.ZERO;
            long noCount=0,paidCount=0,receiptCount=0;
            BigDecimal giamGia=BigDecimal.ZERO;
            Set<Integer> hocVienNo=new HashSet<>();
            try(PreparedStatement s=c.prepareStatement("""
                SELECT SUM(CASE WHEN NgayThu>=? AND NgayThu<? THEN TongTien ELSE 0 END) AS ThuThang,
                       SUM(TongTien) AS ThuAll,COUNT(*) AS SoPhieu FROM dbo.PhieuThu
                WHERE MaCoSo=? AND TrangThai='XAC_NHAN'
                """)){
                s.setTimestamp(1,dauNgayUtc(tuNgay));
                s.setTimestamp(2,dauNgayUtc(denTruoc));s.setInt(3,id);
                try(ResultSet r=s.executeQuery()) {if(r.next()){
                    thuThang=so(r,"ThuThang");thuAll=so(r,"ThuAll");
                    receiptCount=r.getLong("SoPhieu");
                }}
            }
            for(KhoanThu k:khoanThu(c,id)) {
                if(!"HIEU_LUC".equals(k.trangThai()))continue;
                phaiThu=phaiThu.add(k.phaiThu());no=no.add(k.conNo());
                if(k.conNo().signum()>0){noCount++;hocVienNo.add(k.maHocVien());}
                else paidCount++;
            }
            if(coSo==CoSo.TRUNG_TAM)try(PreparedStatement s=c.prepareStatement("""
                SELECT COALESCE(SUM(SoTienGiam),0) FROM dbo.DangKyKhoaHoc
                WHERE MaCoSo=? AND TrangThai<>'HUY'
                """)) {
                s.setInt(1,id);
                try(ResultSet r=s.executeQuery()){r.next();giamGia=r.getBigDecimal(1);}
            }
            return new ThongKe(thuThang,thuAll,phaiThu,no,noCount,paidCount,
                giamGia,hocVienNo.size(),receiptCount);
        }
    }
}
