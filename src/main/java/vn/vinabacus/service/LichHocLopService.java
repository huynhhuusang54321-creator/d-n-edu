package vn.vinabacus.service;

import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import vn.vinabacus.repository.LichHocLopRepository.BuoiTrongTuan;
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

/** Nghiệp vụ lịch học Trung tâm, bao gồm kiểm tra phòng và khung giờ. */
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
    public List<PhongHoc> layPhongHoc() throws SQLException {
        return phongRepo.layDanhSach(maCoSo());
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
    public List<Integer> themNhieu(int maLop, List<BuoiTrongTuan> cacBuoi,
                                  LocalDate tuNgay, LocalDate denNgay, String trangThai)
            throws SQLException {
        if (maLop <= 0) throw new IllegalArgumentException("Hãy chọn một lớp học");
        if (cacBuoi == null || cacBuoi.isEmpty())
            throw new IllegalArgumentException("Hãy đánh dấu ít nhất một thứ học trong tuần");
        for (BuoiTrongTuan b : cacBuoi) {
            if (b == null) throw new IllegalArgumentException("Buổi học không hợp lệ");
            if (b.maPhong() != null && b.maPhong() <= 0)
                throw new IllegalArgumentException("Phòng học không hợp lệ");
            kiemTra(b.thu(), b.batDau(), b.ketThuc(), tuNgay, denNgay, trangThai);
        }
        return lichRepo.themNhieu(maCoSo(), maLop, cacBuoi, tuNgay, denNgay, trangThai);
    }

    public int them(int maLop, int thu, LocalTime batDau, LocalTime ketThuc,
                    LocalDate tuNgay, LocalDate denNgay, String trangThai) throws SQLException {
        if (maLop <= 0) throw new IllegalArgumentException("Hãy chọn lớp học");
        kiemTra(thu, batDau, ketThuc, tuNgay, denNgay, trangThai);
        return lichRepo.them(maCoSo(), maLop, thu, batDau, ketThuc,
                tuNgay, denNgay, trangThai);
    }

    public void sua(int maLichHoc, int thu, LocalTime batDau, LocalTime ketThuc,
                    LocalDate tuNgay, LocalDate denNgay, String trangThai) throws SQLException {
        if (maLichHoc <= 0) throw new IllegalArgumentException("Hãy chọn lịch cần sửa");
        kiemTra(thu, batDau, ketThuc, tuNgay, denNgay, trangThai);
        lichRepo.sua(maCoSo(), maLichHoc, thu, batDau, ketThuc,
                tuNgay, denNgay, trangThai);
    }

    /** Exposed for tests without a live SQL Server. */
    public static void kiemTra(int thu, LocalTime batDau, LocalTime ketThuc,
                               LocalDate tuNgay, LocalDate denNgay, String trangThai) {
        if (thu < 2 || thu > 8) throw new IllegalArgumentException("Thứ học phải từ Thứ 2 đến Chủ nhật");
        if (tuNgay == null) throw new IllegalArgumentException("Hãy chọn Từ ngày");
        if (denNgay != null && denNgay.isBefore(tuNgay))
            throw new IllegalArgumentException("Đến ngày không được trước Từ ngày");
        if ((batDau == null) != (ketThuc == null))
            throw new IllegalArgumentException("Hãy nhập đủ cả giờ bắt đầu và giờ kết thúc, hoặc để trống cả hai");
        if (batDau != null && !ketThuc.isAfter(batDau))
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
