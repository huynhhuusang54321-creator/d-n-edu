package vn.vinabacus.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.repository.KhoaHocRepository;
import vn.vinabacus.repository.LopTrungTamRepository;
import vn.vinabacus.repository.MonHocRepository;

public final class LopTrungTamService {
    private final MonHocRepository monRepo = new MonHocRepository();
    private final KhoaHocRepository khoaRepo = new KhoaHocRepository();
    private final LopTrungTamRepository lopRepo = new LopTrungTamRepository();
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

    public List<LopTrungTam> timKiem(String tuKhoa, String trangThai) throws SQLException {
        String tim = boDau(tuKhoa == null ? "" : tuKhoa.trim());
        List<LopTrungTam> ketQua = new ArrayList<>();
        for (LopTrungTam l : lopRepo.layDanhSach(maCoSo())) {
            if (trangThai != null && !trangThai.equals(l.trangThai())) continue;
            if (!tim.isEmpty() && !boDau(l.tenLop()).contains(tim)
                    && !boDau(l.tenKhoaHoc()).contains(tim)
                    && !boDau(l.tenMonHoc()).contains(tim)
                    && !boDau(l.tenGiaoVien()).contains(tim)) continue;
            ketQua.add(l);
        }
        return ketQua;
    }

    public int them(int maKhoaHoc, String tenLop, Integer maGiaoVien,
                    LocalDate ngayKhaiGiang, String trangThai) throws SQLException {
        kiemTra(maKhoaHoc, tenLop, maGiaoVien, trangThai);
        return lopRepo.them(maCoSo(), maKhoaHoc, tenLop.trim(), maGiaoVien,
                ngayKhaiGiang, trangThai);
    }

    public void sua(int maLop, int maKhoaHoc, String tenLop, Integer maGiaoVien,
                    LocalDate ngayKhaiGiang, String trangThai) throws SQLException {
        if (maLop <= 0) throw new IllegalArgumentException("Không tìm thấy mã lớp");
        kiemTra(maKhoaHoc, tenLop, maGiaoVien, trangThai);
        lopRepo.sua(maCoSo(), maLop, maKhoaHoc, tenLop.trim(), maGiaoVien,
                ngayKhaiGiang, trangThai);
    }

    private static void kiemTra(int maKhoaHoc, String tenLop, Integer maGiaoVien,
                                String trangThai) {
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
