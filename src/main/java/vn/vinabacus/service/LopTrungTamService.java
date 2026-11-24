package vn.vinabacus.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.repository.KhoaHocRepository;
import vn.vinabacus.repository.LichHocLopRepository;
import vn.vinabacus.repository.LopTrungTamRepository;
import vn.vinabacus.repository.MonHocRepository;

public final class LopTrungTamService {
    private final MonHocRepository monRepo = new MonHocRepository();
    private final KhoaHocRepository khoaRepo = new KhoaHocRepository();
    private final LopTrungTamRepository lopRepo = new LopTrungTamRepository();
    private final LichHocLopRepository lichRepo = new LichHocLopRepository();
    private final CoSo coSo;

    public LopTrungTamService(CoSo coSo) {
        if (coSo != CoSo.TRUNG_TAM)
            throw new IllegalArgumentException("Lớp Trung tâm chỉ sử dụng ở cơ sở Trung tâm");
        this.coSo = coSo;
    }

    private int maCoSo() throws SQLException { return monRepo.layMaCoSo(coSo); }

    public List<MonHoc> layMonHoc() throws SQLException {
        return monRepo.layDanhSach(maCoSo());
    }

    public List<KhoaHoc> layKhoaHoc() throws SQLException {
        return khoaRepo.layDanhSach(maCoSo());
    }

    public List<LopTrungTamRepository.GiaoVien> layGiaoVien() throws SQLException {
        return lopRepo.layGiaoVien(maCoSo());
    }

    public List<LopTrungTamRepository.GiaoVien> layGiaoVienChuyenMon(
            int maMonHoc, LocalDate ngay) throws SQLException {
        if (maMonHoc <= 0 || ngay == null)
            throw new IllegalArgumentException("Môn học hoặc ngày chuyển không hợp lệ");
        return lopRepo.layGiaoVienChuyenMon(maCoSo(), maMonHoc, ngay);
    }

    public void themChuyenMonGiaoVien(int maNhanVien, int maMonHoc,
                                     LocalDate ngay, String lyDo) throws SQLException {
        if (maNhanVien <= 0 || maMonHoc <= 0
                || ngay == null || !ngay.equals(LocalDate.now()))
            throw new IllegalArgumentException("Chỉ khai báo chuyên môn hợp lệ từ hôm nay");
        kiemTraLyDo(lyDo);
        lopRepo.themChuyenMonGiaoVien(maCoSo(), maNhanVien, maMonHoc,
                ngay, lyDo.trim());
    }

    public void ketThucChuyenMonGiaoVien(int maNhanVien, int maMonHoc,
                                         LocalDate ngay, String lyDo) throws SQLException {
        if (maNhanVien <= 0 || maMonHoc <= 0
                || ngay == null || !ngay.equals(LocalDate.now()))
            throw new IllegalArgumentException("Chỉ kết thúc chuyên môn hợp lệ từ hôm nay");
        kiemTraLyDo(lyDo);
        lopRepo.ketThucChuyenMonGiaoVien(maCoSo(), maNhanVien, maMonHoc,
                ngay, lyDo.trim());
    }

    public List<LopTrungTamRepository.HocVienTrongLop> hocVienLop(int maLop)
            throws SQLException {
        return lopRepo.layHocVien(maCoSo(), maLop);
    }
    public List<LopTrungTamRepository.LichSuPhanCong> lichSuPhanCong(int maLop)
            throws SQLException {
        if (maLop <= 0) throw new IllegalArgumentException("Lớp không hợp lệ");
        return lopRepo.layLichSuPhanCong(maCoSo(), maLop);
    }

    public List<LichHocLop> lichLop(int maLop) throws SQLException {
        List<LichHocLop> ds = new ArrayList<>();
        for (LichHocLop lich : lichRepo.layDanhSach(maCoSo())) {
            if (lich.maLop() == maLop) ds.add(lich);
        }
        return ds;
    }

    public List<LopTrungTam> timKiem(String tuKhoa, String trangThai) throws SQLException {
        String tim = boDau(tuKhoa == null ? "" : tuKhoa.trim());
        List<LopTrungTam> ketQua = new ArrayList<>();
        for (LopTrungTam l : lopRepo.layDanhSach(maCoSo())) {
            if (trangThai != null && !trangThai.equals(l.trangThai())) continue;
            if (!tim.isEmpty() && !boDau(l.tenLop()).contains(tim)
                    && !boDau(l.tenKhoaHoc()).contains(tim)
                    && !boDau(l.tenMonHoc()).contains(tim)
                    && !boDau(l.tenGiaoVien()).contains(tim)
                    && !boDau(l.tenPhong()).contains(tim)
                    && !String.valueOf(l.maLop()).contains(tim)) continue;
            ketQua.add(l);
        }
        return ketQua;
    }

    public int them(int maKhoaHoc, String tenLop, Integer maGiaoVien,
                    LocalDate ngayKhaiGiang, String trangThai, int siSoToiDa) throws SQLException {
        kiemTra(maKhoaHoc, tenLop, maGiaoVien, trangThai, siSoToiDa);
        return lopRepo.them(maCoSo(), maKhoaHoc, tenLop.trim(), maGiaoVien,
                ngayKhaiGiang, trangThai, siSoToiDa);
    }

    public int themVoiLich(int maKhoaHoc,String tenLop,Integer maGiaoVien,
            LocalDate ngayKhaiGiang,String trangThai,int siSoToiDa,
            List<vn.vinabacus.repository.LichHocLopRepository.BuoiCoPhong> cacBuoi,
            LocalDate tuNgay) throws SQLException {
        kiemTra(maKhoaHoc,tenLop,maGiaoVien,trangThai,siSoToiDa);
        if (cacBuoi == null || cacBuoi.isEmpty())
            throw new IllegalArgumentException("Hãy chọn ít nhất một ca học");
        java.util.Set<String> daChon = new java.util.HashSet<>();
        for (var item:cacBuoi) {
            if (item == null || item.buoi() == null || item.maPhong() == null)
                throw new IllegalArgumentException("Mỗi ca cần một phòng hợp lệ");
            var b=item.buoi();
            vn.vinabacus.service.CaHocTrungTam.batBuocCaCoDinh(b.batDau(),b.ketThuc());
            if (b.thu()<2 || b.thu()>8 || !daChon.add(b.thu()+":"+b.batDau()))
                throw new IllegalArgumentException("Thứ hoặc ca học bị trùng/không hợp lệ");
        }
        if (tuNgay == null || tuNgay.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Ngày lịch hiệu lực không được trong quá khứ");
        return lopRepo.themVoiLich(maCoSo(),maKhoaHoc,tenLop.trim(),maGiaoVien,
                ngayKhaiGiang,trangThai,siSoToiDa,cacBuoi,tuNgay);
    }

    public void sua(int maLop, int maKhoaHoc, String tenLop, Integer maGiaoVien,
                    LocalDate ngayKhaiGiang, String trangThai, Integer siSoToiDa) throws SQLException {
        if (maLop <= 0) throw new IllegalArgumentException("Không tìm thấy mã lớp");
        kiemTraThongTin(maKhoaHoc, tenLop, maGiaoVien, trangThai);
        if (siSoToiDa != null && siSoToiDa <= 0)
            throw new IllegalArgumentException("Sĩ số tối đa phải lớn hơn 0 hoặc để trống khi chưa xác định");
        lopRepo.sua(maCoSo(), maLop, maKhoaHoc, tenLop.trim(), maGiaoVien,
                ngayKhaiGiang, trangThai, siSoToiDa);
    }
    public void suaVoiLich(int maLop,int maKhoaHoc,String tenLop,Integer maGiaoVien,
            LocalDate ngayKhaiGiang,String trangThai,Integer siSoToiDa,
            List<vn.vinabacus.repository.LichHocLopRepository.BuoiCoPhong> cacBuoi,
            LocalDate tuNgay) throws SQLException {
        if (maLop<=0) throw new IllegalArgumentException("Hãy chọn lớp học");
        kiemTraThongTin(maKhoaHoc,tenLop,maGiaoVien,trangThai);
        if (siSoToiDa!=null && siSoToiDa<=0)
            throw new IllegalArgumentException("Sĩ số tối đa phải lớn hơn 0");
        if (cacBuoi==null || cacBuoi.isEmpty())
            throw new IllegalArgumentException("Hãy chọn lịch cần bổ sung");
        java.util.Set<String> daChon=new java.util.HashSet<>();
        for(var item:cacBuoi) {
            if(item==null || item.buoi()==null || item.maPhong()==null)
                throw new IllegalArgumentException("Mỗi ca cần phòng hợp lệ");
            var b=item.buoi();
            CaHocTrungTam.batBuocCaCoDinh(b.batDau(),b.ketThuc());
            if(b.thu()<2 || b.thu()>8 || !daChon.add(b.thu()+":"+b.batDau()))
                throw new IllegalArgumentException("Thứ/ca trùng hoặc không hợp lệ");
        }
        if(tuNgay==null || tuNgay.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Ngày hiệu lực lịch không được ở quá khứ");
        lopRepo.suaVoiLich(maCoSo(),maLop,maKhoaHoc,tenLop.trim(),maGiaoVien,
                ngayKhaiGiang,trangThai,siSoToiDa,cacBuoi,tuNgay);
    }

    /** Chỉ chuyển trong ngày; không tự đặt lại ngày cho lịch giảng dạy cũ. */
    public void chuyenGiaoVien(int maLop, int maGiaoVienMoi,
                              LocalDate ngayChuyen, String lyDo) throws SQLException {
        if (maLop <= 0 || maGiaoVienMoi <= 0)
            throw new IllegalArgumentException("Hãy chọn lớp và giáo viên mới hợp lệ");
        if (ngayChuyen == null || !ngayChuyen.equals(LocalDate.now()))
            throw new IllegalArgumentException("Chuyển phân công chỉ có hiệu lực trong ngày hiện tại");
        kiemTraLyDo(lyDo);
        lopRepo.chuyenGiaoVien(maCoSo(), maLop, maGiaoVienMoi,
                ngayChuyen, lyDo.trim());
    }

    private static void kiemTraLyDo(String lyDo) {
        if (lyDo == null || lyDo.isBlank() || lyDo.trim().length() > 500)
            throw new IllegalArgumentException("Lý do phải có từ 1 đến 500 ký tự");
    }

    private static void kiemTra(int maKhoaHoc, String tenLop, Integer maGiaoVien,
                                String trangThai, int siSoToiDa) {
        kiemTraThongTin(maKhoaHoc, tenLop, maGiaoVien, trangThai);
        if (siSoToiDa <= 0)
            throw new IllegalArgumentException("Sĩ số tối đa phải lớn hơn 0");
    }

    private static void kiemTraThongTin(int maKhoaHoc, String tenLop,
                                        Integer maGiaoVien, String trangThai) {
        if (maKhoaHoc <= 0) throw new IllegalArgumentException("Hãy chọn khóa học");
        if (tenLop == null || tenLop.isBlank())
            throw new IllegalArgumentException("Hãy nhập tên lớp");
        if (tenLop.trim().length() > 180)
            throw new IllegalArgumentException("Tên lớp không quá 180 ký tự");
        if (maGiaoVien != null && maGiaoVien <= 0)
            throw new IllegalArgumentException("Giáo viên không hợp lệ");
        if (!List.of("DANG_MO", "TAM_DUNG", "DA_DONG").contains(trangThai))
            throw new IllegalArgumentException("Trạng thái lớp không hợp lệ");
    }

    private static String boDau(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}
