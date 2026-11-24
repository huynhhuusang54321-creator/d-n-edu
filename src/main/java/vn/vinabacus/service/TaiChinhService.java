package vn.vinabacus.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import vn.vinabacus.model.TaiChinhDTO;
import vn.vinabacus.repository.TaiChinhRepository;
import vn.vinabacus.repository.TaiChinhDangKyRepository;
import vn.vinabacus.model.CoSo;

/** Tính toán, kiểm tra dữ liệu nghiệp vụ độc lập Swing. */
public final class TaiChinhService {
    private final TaiChinhRepository repo;
    public TaiChinhService() { this(new TaiChinhRepository()); }
    public TaiChinhService(TaiChinhRepository repo) { this.repo = repo; }
    public static BigDecimal tien(String input) {
        if (input == null || !input.trim().matches("[0-9]+(\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException("Số tiền chỉ gồm chữ số, có tối đa 2 chữ số thập phân; không nhập dấu phân cách hàng nghìn.");
        }
        BigDecimal n = new BigDecimal(input.trim()).setScale(2, RoundingMode.UNNECESSARY);
        if (n.precision() > 18) throw new IllegalArgumentException("Số tiền quá lớn.");
        return n;
    }
    public static String hienTien(BigDecimal n) { return n.stripTrailingZeros().toPlainString(); }
    /** Điều chỉnh cho phép dấu + / -, nhưng không chấp nhận giá trị 0. */
    public static BigDecimal tienDieuChinh(String input) {
        if(input==null||!input.trim().matches("[+-]?[0-9]+(\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("Nhập số tiền điều chỉnh có dấu +/- và tối đa 2 chữ số thập phân.");
        BigDecimal value=new BigDecimal(input.trim()).setScale(2,RoundingMode.UNNECESSARY);
        if(value.signum()==0||value.precision()>18)
            throw new IllegalArgumentException("Số tiền điều chỉnh phải khác 0 và không quá lớn.");
        return value;
    }
    public static BigDecimal conNo(BigDecimal phaiThu, BigDecimal daThanhToan) {
        if (phaiThu.signum() < 0 || daThanhToan.signum() < 0)
            throw new IllegalArgumentException("Số tiền không hợp lệ.");
        return phaiThu.subtract(daThanhToan);
    }
    public static BigDecimal sauGiam(BigDecimal goc, BigDecimal giam) {
        if (goc.signum() < 0 || giam.signum() < 0 || giam.compareTo(goc) > 0)
            throw new IllegalArgumentException("Mức giảm không hợp lệ.");
        return goc.subtract(giam);
    }
    public static LocalDate thangBatDau(String input) {
        try { return YearMonth.parse(input.trim()).atDay(1); }
        catch (Exception ex) { throw new IllegalArgumentException("Nhập kỳ theo định dạng yyyy-MM, ví dụ 2026-11."); }
    }
    public List<TaiChinhDTO.KhoanThu> khoanThu(CoSo coSo) throws Exception { return repo.khoanThu(coSo); }
    public List<TaiChinhDTO.PhieuThu> phieuThu(CoSo coSo,YearMonth thang) throws Exception { return repo.phieuThu(coSo,thang); }
    public List<TaiChinhDTO.PhieuThu> phieuThu(CoSo coSo,LocalDate tuNgay,LocalDate denTruoc)throws Exception {
        return repo.phieuThu(coSo,tuNgay,denTruoc);
    }
    public List<TaiChinhDTO.PhanBoPhieu> phanBoPhieu(CoSo coSo,int maPhieu) throws Exception {
        return repo.phanBoPhieu(coSo,maPhieu);
    }
    public List<TaiChinhDTO.LichSuThu> lichSuThuHocVien(CoSo coSo,int maHocVien)throws Exception {
        return repo.lichSuThuHocVien(coSo,maHocVien);
    }
    public List<TaiChinhDTO.UuDaiDangKy> uuDaiHocVien(int maHocVien)throws Exception {
        return repo.uuDaiHocVien(maHocVien);
    }
    public int lapPhieuNhieuKhoan(CoSo coSo,int maHocVien,BigDecimal soTien,
            List<TaiChinhDangKyRepository.PhanBo> phanBo,String hinhThuc,
            int taiKhoan,String ghiChu,UUID token) throws Exception {
        return repo.lapPhieuNhieuKhoan(coSo,maHocVien,soTien,phanBo,hinhThuc,taiKhoan,ghiChu,token);
    }
    public void huyPhieu(CoSo coSo,int maPhieu,int taiKhoan,String lyDo) throws Exception {
        repo.huyPhieu(coSo,maPhieu,taiKhoan,lyDo);
    }
    public List<TaiChinhDTO.LuaChon> taiKhoan(CoSo coSo) throws Exception { return repo.taiKhoan(coSo); }
    public List<TaiChinhDTO.LuaChon> lopMamNon() throws Exception { return repo.lopMamNon(); }
    public List<TaiChinhDTO.DangKyChuaThu> dangKyChuaThu() throws Exception { return repo.dangKyChuaThu(); }
    public List<TaiChinhDTO.BangTinhChot> bangTinhChot() throws Exception { return repo.bangTinhChot(); }
    public List<TaiChinhDTO.HocPhiTrungTam> hocPhiTrungTam() throws Exception { return repo.hocPhiTrungTam(); }
    public void dieuChinhKhoan(CoSo coSo,int maKhoan,BigDecimal thayDoi,String lyDo,int taiKhoan) throws Exception {
        if(thayDoi==null||thayDoi.signum()==0)throw new IllegalArgumentException("Số tiền điều chỉnh phải khác 0.");
        repo.dieuChinhKhoan(coSo,maKhoan,thayDoi,lyDo,taiKhoan);
    }
    public List<TaiChinhDTO.MucPhi> mucPhiMamNon() throws Exception { return repo.mucPhiMamNon(); }
    public TaiChinhDTO.ThongKe thongKe(CoSo coSo, YearMonth thang) throws Exception { return repo.thongKe(coSo,thang); }
    public TaiChinhDTO.ThongKe thongKe(CoSo coSo,LocalDate tu,LocalDate den)throws Exception {
        return repo.thongKe(coSo,tu,den);
    }
    public List<TaiChinhDTO.DongThuBaoCao> baoCaoThu(CoSo coSo,LocalDate tu,LocalDate den)throws Exception {
        return repo.baoCaoThu(coSo,tu,den);
    }
    public void lapKhoanHocPhiKhoa(int dangKy, LocalDate han) throws Exception { repo.lapKhoanHocPhiKhoa(dangKy,han); }
    public int lapKhoanMamNon(int bangTinh, boolean hocPhi,boolean tienAn,LocalDate han) throws Exception { return repo.lapKhoanMamNon(bangTinh,hocPhi,tienAn,han); }
    public void taoMucPhi(int maLop, boolean tienAn, LocalDate apDung, BigDecimal tien, int taiKhoan) throws Exception {
        if (tien.signum()<0) throw new IllegalArgumentException("Đơn giá phải >= 0.");
        if (apDung.getDayOfMonth()!=1) throw new IllegalArgumentException("Kỳ áp dụng phải là đầu tháng.");
        repo.taoMucPhi(maLop,tienAn,apDung,tien,taiKhoan);
    }
    public void lapPhieu(CoSo coSo,int maKhoan,BigDecimal soTien,String hinhThuc,int taiKhoan,String ghiChu) throws Exception {
        if (soTien.signum()<=0) throw new IllegalArgumentException("Số tiền thu phải > 0.");
        repo.lapPhieu(coSo,maKhoan,soTien,hinhThuc,taiKhoan,ghiChu);
    }
}
