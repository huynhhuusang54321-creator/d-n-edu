package vn.vinabacus.test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.repository.ChuongTrinhGiamGiaRepository;
import vn.vinabacus.repository.DangKyKhoaHocRepository;
import vn.vinabacus.repository.KhoaHocRepository;
import vn.vinabacus.repository.LichHocLopRepository;
import vn.vinabacus.repository.LopTrungTamRepository;
import vn.vinabacus.repository.MonHocRepository;
import vn.vinabacus.repository.NhanVienRepository;
import vn.vinabacus.repository.PhongHocRepository;

/** Kiểm tra đọc SQL thật; tuyệt đối không gọi API ghi dữ liệu. */
public final class KiemThuGiaiDoan4ReadOnly {
    private KiemThuGiaiDoan4ReadOnly() { }

    public static void main(String[] args) throws Exception {
        MonHocRepository monRepo = new MonHocRepository();
        int maCoSo = monRepo.layMaCoSo(CoSo.TRUNG_TAM);
        List<MonHoc> mon = monRepo.layDanhSach(maCoSo);
        List<KhoaHoc> khoa = new KhoaHocRepository().layDanhSach(maCoSo);
        LopTrungTamRepository lopRepo = new LopTrungTamRepository();
        List<LopTrungTam> lop = lopRepo.layDanhSach(maCoSo);
        Set<Integer> maMon = mon.stream().map(MonHoc::maMonHoc).collect(Collectors.toSet());
        Set<Integer> maKhoa = khoa.stream().map(KhoaHoc::maKhoaHoc).collect(Collectors.toSet());
        for (KhoaHoc k : khoa) {
            if (!maMon.contains(k.maMonHoc()))
                throw new AssertionError("Khóa không thuộc môn của Trung tâm: " + k.maKhoaHoc());
        }
        for (LopTrungTam l : lop) {
            if (!maKhoa.contains(l.maKhoaHoc()))
                throw new AssertionError("Lớp không thuộc khóa của Trung tâm: " + l.maLop());
            lopRepo.layHocVien(maCoSo, l.maLop());
        }
        var lopChon = new DangKyKhoaHocRepository().thongTinLop(maCoSo);
        for (var lc : lopChon) {
            if ("Đang nhận".equals(lc.trangThaiNhan())
                    && (lc.siSoToiDaLop() == null || lc.siSoToiDaLop() <= 0
                        || lc.sucChuaPhong() == null || lc.sucChuaPhong() <= 0
                        || lc.siSoToiDaLop() > lc.sucChuaPhong()
                        || lc.gioiHanTiepNhan() != lc.siSoToiDaLop()
                        || lc.soDaGiuCho() >= lc.gioiHanTiepNhan())) {
                throw new AssertionError("Lớp nhận học viên khi thiếu giới hạn/phòng: "
                        + lc.maLop());
            }
        }
        int soLopChon = lopChon.size();
        PhongHocRepository phongRepo = new PhongHocRepository();
        int soPhong = phongRepo.layDanhSach(maCoSo).size();
        int soLopDungPhong = phongRepo.layLopSuDung(maCoSo).size();
        int soLich = new LichHocLopRepository().layDanhSach(maCoSo).size();
        NhanVienRepository nhanVienRepo = new NhanVienRepository();
        var nhanVien = nhanVienRepo.timTheoCoSo(maCoSo);
        for (var nv : nhanVien) {
            nhanVienRepo.chiTietPhanCongTrungTam(maCoSo, nv.getMaNhanVien());
        }
        int soGiamGia = new ChuongTrinhGiamGiaRepository().layDanhSach(maCoSo).size();
        System.out.printf("READ_ONLY_OK coSo=%d mon=%d khoa=%d lop=%d phong=%d "
                        + "lopDungPhong=%d lich=%d giaoVienNhanSu=%d "
                        + "lopDangKy=%d giamGia=%d%n",
                maCoSo, mon.size(), khoa.size(), lop.size(), soPhong,
                soLopDungPhong, soLich, nhanVien.size(), soLopChon, soGiamGia);
    }
}
