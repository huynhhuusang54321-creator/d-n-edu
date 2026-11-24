package vn.vinabacus.service;

import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import vn.vinabacus.repository.LichHocLopRepository.BuoiTrongTuan;
import vn.vinabacus.repository.LichHocLopRepository.BuoiCoPhong;
import vn.vinabacus.repository.LichHocLopRepository.HocVienTheoLop;
import java.util.Locale;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.repository.KhoaHocRepository;
import vn.vinabacus.repository.LichHocLopRepository;
import vn.vinabacus.repository.LopTrungTamRepository;
import vn.vinabacus.repository.MonHocRepository;
import vn.vinabacus.repository.PhongHocRepository;

/** Nghiep vu lich hoc Trung tam, yeu cau migration PhongHoc da duoc ap dung. */
public final class LichHocLopService {
    private final CoSo coSo;
    private final MonHocRepository monRepo = new MonHocRepository();
    private final KhoaHocRepository khoaRepo = new KhoaHocRepository();
    private final LopTrungTamRepository lopRepo = new LopTrungTamRepository();
    private final LichHocLopRepository lichRepo = new LichHocLopRepository();
    private final PhongHocRepository phongRepo = new PhongHocRepository();

    public LichHocLopService(CoSo coSo) {
        if (coSo != CoSo.TRUNG_TAM)
            throw new IllegalArgumentException("Chỉ hỗ trợ lịch học Trung tâm");
        this.coSo = coSo;
    }

    private int maCoSo() throws SQLException { return monRepo.layMaCoSo(coSo); }

    public List<MonHoc> layMonHoc() throws SQLException { return monRepo.layDanhSach(maCoSo()); }
    public List<KhoaHoc> layKhoaHoc() throws SQLException { return khoaRepo.layDanhSach(maCoSo()); }
    public List<LopTrungTam> layLop() throws SQLException { return lopRepo.layDanhSach(maCoSo()); }
    public List<PhongHoc> layPhongHoc() throws SQLException { return phongRepo.layDanhSach(maCoSo()); }
    public List<HocVienTheoLop> layHocVienTheoLop() throws SQLException {
        return lichRepo.layHocVienTheoLop(maCoSo());
    }
    public List<DuKienKetThucLop.BuoiDieuChinh> layBuoiDieuChinh() throws SQLException {
        return lichRepo.layBuoiDieuChinh(maCoSo());
    }

    /** Gioi han lich tuan theo dung dot xep lop cua hoc vien. */
    public static LichHocLop gioiHanTheoXep(LichHocLop lich, HocVienTheoLop xep) {
        if (lich.maLop() != xep.maLop()) return null;
        LocalDate tu = lich.tuNgay().isAfter(xep.tuNgay()) ? lich.tuNgay() : xep.tuNgay();
        LocalDate den = lich.denNgay();
        if (xep.denNgay() != null && (den == null || xep.denNgay().isBefore(den)))
            den = xep.denNgay();
        if (den != null && den.isBefore(tu)) return null;
        return new LichHocLop(lich.maLichHoc(), lich.maLop(), lich.maKhoaHoc(),
                lich.maMonHoc(), lich.tenMonHoc(), lich.tenKhoaHoc(), lich.tenLop(),
                lich.thuTrongTuan(), lich.gioBatDau(), lich.gioKetThuc(),
                tu, den, lich.trangThai(), lich.maPhong(), lich.tenPhong());
    }

    public List<LichHocLop> timKiem(String tuKhoa, String trangThai) throws SQLException {
        String tim = boDau(tuKhoa == null ? "" : tuKhoa.trim());
        List<LichHocLop> ketQua = new ArrayList<>();
        for (LichHocLop lh : lichRepo.layDanhSach(maCoSo())) {
            if (trangThai != null && !trangThai.equals(lh.trangThai())) continue;
            if (!tim.isBlank() && !boDau(lh.tenMonHoc()).contains(tim)
                    && !boDau(lh.tenKhoaHoc()).contains(tim)
                    && !boDau(lh.tenLop()).contains(tim)
                    && !boDau(tenThu(lh.thuTrongTuan())).contains(tim)) continue;
            ketQua.add(lh);
        }
        return ketQua;
    }

    /** Thêm một hoặc nhiều thứ học cho cùng lớp, ngày hiệu lực, trạng thái. */
    public List<Integer> themNhieu(int maLop, Integer maPhong, List<BuoiTrongTuan> cacBuoi,
                                  LocalDate tuNgay, LocalDate denNgay, String trangThai)
            throws SQLException {
        if (maLop <= 0) throw new IllegalArgumentException("Hãy chọn một lớp học");
        if (cacBuoi == null || cacBuoi.isEmpty())
            throw new IllegalArgumentException("Hãy đánh dấu ít nhất một thứ học trong tuần");
        Set<String> daChon = new HashSet<>();
        for (BuoiTrongTuan b : cacBuoi) {
            if (b == null) throw new IllegalArgumentException("Buổi học không hợp lệ");
            if (!daChon.add(b.thu() + ":" + b.batDau() + ":" + b.ketThuc()))
                throw new IllegalArgumentException("Không được chọn trùng một ô thứ/ca hai lần");
            kiemTra(b.thu(), b.batDau(), b.ketThuc(), tuNgay, denNgay, trangThai);
        }
        return lichRepo.themNhieu(maCoSo(), maLop, maPhong, cacBuoi, tuNgay, denNgay, trangThai);
    }

    /** Gan phong rieng cho tung o; repository bao dam giao dich atomic. */
    public List<Integer> themNhieuTheoPhong(int maLop, List<BuoiCoPhong> cacBuoi,
                                           LocalDate tuNgay, LocalDate denNgay, String trangThai)
            throws SQLException {
        if (maLop <= 0 || cacBuoi == null || cacBuoi.isEmpty())
            throw new IllegalArgumentException("Hãy chọn lớp và ít nhất một buổi học");
        Set<String> khongTrung = new HashSet<>();
        for (BuoiCoPhong item : cacBuoi) {
            if (item == null || item.buoi() == null)
                throw new IllegalArgumentException("Buổi học không hợp lệ");
            BuoiTrongTuan b = item.buoi();
            if (!khongTrung.add(b.thu() + ":" + b.batDau()))
                throw new IllegalArgumentException("Đã chọn trùng một ô ca học");
            kiemTra(b.thu(), b.batDau(), b.ketThuc(), tuNgay, denNgay, trangThai);
            if ("HIEU_LUC".equals(trangThai) && item.maPhong() == null)
                throw new IllegalArgumentException("Mỗi buổi hiệu lực phải được gán phòng");
        }
        return lichRepo.themNhieuTheoPhong(maCoSo(), maLop, cacBuoi, tuNgay, denNgay, trangThai);
    }

    public int them(int maLop, Integer maPhong, int thu, LocalTime batDau, LocalTime ketThuc,
                    LocalDate tuNgay, LocalDate denNgay, String trangThai) throws SQLException {
        if (maLop <= 0) throw new IllegalArgumentException("Hãy chọn lớp học");
        kiemTra(thu, batDau, ketThuc, tuNgay, denNgay, trangThai);
        return lichRepo.them(maCoSo(), maLop, maPhong, thu, batDau, ketThuc,
                tuNgay, denNgay, trangThai);
    }

    public void sua(int maLichHoc, Integer maPhong, int thu, LocalTime batDau, LocalTime ketThuc,
                    LocalDate tuNgay, LocalDate denNgay, String trangThai) throws SQLException {
        if (maLichHoc <= 0) throw new IllegalArgumentException("Hãy chọn lịch cần sửa");
        kiemTra(thu, batDau, ketThuc, tuNgay, denNgay, trangThai);
        lichRepo.sua(maCoSo(), maLichHoc, maPhong, thu, batDau, ketThuc, tuNgay, denNgay, trangThai);
    }

    /** Tach lich lặp tu ngay hieu luc; ban ghi cu va buoi cu duoc giu. */
    public int suaTuNgay(int maLichHoc, Integer maPhong, int thu,
                         LocalTime batDau, LocalTime ketThuc,
                         LocalDate ngayHieuLuc, LocalDate denNgay, String trangThai)
            throws SQLException {
        if (maLichHoc <= 0) throw new IllegalArgumentException("Hãy chọn lịch cần điều chỉnh");
        kiemTra(thu, batDau, ketThuc, ngayHieuLuc, denNgay, trangThai);
        if (ngayHieuLuc.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Ngày hiệu lực không được ở trong quá khứ");
        return lichRepo.suaTuNgay(maCoSo(), maLichHoc, maPhong, thu,
                batDau, ketThuc, ngayHieuLuc, denNgay, trangThai);
    }

    /** Ngày gợi ý khi mở form sửa; ngày bắt đầu của lịch cũ vẫn giữ nguyên trong CSDL. */
    public static LocalDate ngaySuaMacDinh(LocalDate tuNgayCu, LocalDate homNay) {
        if (tuNgayCu == null || homNay == null)
            throw new IllegalArgumentException("Thiếu ngày bắt đầu lịch hoặc ngày hiện tại");
        return tuNgayCu.isBefore(homNay) ? homNay : tuNgayCu;
    }

    /** Chặn ngay trong form để người dùng thấy rõ ngày hiệu lực phải chọn. */
    public static void kiemTraNgaySua(LocalDate tuNgayCu, LocalDate denNgayCu,
                                     LocalDate ngayHieuLuc, LocalDate homNay) {
        if (tuNgayCu == null || homNay == null)
            throw new IllegalArgumentException("Thiếu thời gian áp dụng của lịch cũ");
        if (denNgayCu != null && denNgayCu.isBefore(homNay))
            throw new IllegalArgumentException("Lịch cũ đã kết thúc ngày " + denNgayCu
                    + "; không thể sửa hồi tố. Hãy tạo lịch mới.");
        if (ngayHieuLuc == null || ngayHieuLuc.isBefore(homNay))
            throw new IllegalArgumentException("Lịch cũ bắt đầu ngày " + tuNgayCu
                    + ". Từ ngày thay đổi phải là " + homNay
                    + " hoặc ngày tương lai để giữ lịch sử.");
        if (ngayHieuLuc.isAfter(tuNgayCu) && denNgayCu != null
                && ngayHieuLuc.isAfter(denNgayCu))
            throw new IllegalArgumentException("Ngày hiệu lực " + ngayHieuLuc
                    + " vượt ngày kết thúc lịch cũ " + denNgayCu
                    + "; hãy tạo lịch mới hoặc chọn ngày trong thời hạn cũ.");
    }

    public int huyBuoi(int maLichHoc, LocalDate ngayHoc, String lyDo)
            throws SQLException {
        if (maLichHoc <= 0) throw new IllegalArgumentException("Hay chon lich can huy buoi");
        if (lyDo == null || lyDo.isBlank())
            throw new IllegalArgumentException("Hay ghi ly do huy buoi");
        return lichRepo.huyBuoi(maCoSo(), maLichHoc, ngayHoc, lyDo.trim());
    }

    public int themBuoiBu(int maLop, Integer maPhong, LocalDate ngayHoc,
                          int soCa, String ghiChu) throws SQLException {
        if (maLop <= 0) throw new IllegalArgumentException("Hay chon lop hoc");
        if (maPhong == null || maPhong <= 0)
            throw new IllegalArgumentException("Hay chon phong cho buoi bu");
        CaHocTrungTam.layTheoSoCa(soCa);
        return lichRepo.themBuoiBu(maCoSo(), maLop, maPhong, ngayHoc, soCa, ghiChu);
    }

    /** Exposed for tests without a live SQL Server. */
    public static void kiemTra(int thu, LocalTime batDau, LocalTime ketThuc,
                               LocalDate tuNgay, LocalDate denNgay, String trangThai) {
        if (thu < 2 || thu > 8) throw new IllegalArgumentException("Thứ học phải từ Thứ 2 đến Chủ nhật");
        if (tuNgay == null) throw new IllegalArgumentException("Hãy chọn Từ ngày");
        if (denNgay != null && denNgay.isBefore(tuNgay))
            throw new IllegalArgumentException("Đến ngày không được trước Từ ngày");
        if ((batDau == null) != (ketThuc == null))
            throw new IllegalArgumentException("Giờ bắt đầu và kết thúc phải đi cùng nhau");
        // Lich da huy duoc giu gio cu de huy an toan ca cac lich legacy ngoai 8 ca.
        if ("HIEU_LUC".equals(trangThai)) CaHocTrungTam.batBuocCaCoDinh(batDau, ketThuc);
        else if (batDau != null && !ketThuc.isAfter(batDau))
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        if (!List.of("HIEU_LUC", "HUY").contains(trangThai))
            throw new IllegalArgumentException("Trạng thái lịch học không hợp lệ");
    }

    public static String tenThu(int thu) {
        return thu == 8 ? "Chủ nhật" : "Thứ " + thu;
    }

    private static String boDau(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}
