package vn.vinabacus.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.repository.KhoaHocRepository;
import vn.vinabacus.repository.MonHocRepository;

public final class KhoaHocService {
    private final CoSo coSo;
    private final KhoaHocRepository repo = new KhoaHocRepository();
    private final MonHocRepository monRepo = new MonHocRepository();

    public KhoaHocService(CoSo coSo) {
        if (coSo != CoSo.TRUNG_TAM) {
            throw new IllegalArgumentException("Quan ly khoa hoc chi danh cho Trung tam");
        }
        this.coSo = coSo;
    }

    private int maCoSo() throws SQLException { return monRepo.layMaCoSo(coSo); }

    public List<KhoaHoc> timKiem(String tuKhoa, Boolean trangThai) throws SQLException {
        String tim = boDau(tuKhoa == null ? "" : tuKhoa.trim());
        List<KhoaHoc> ketQua = new ArrayList<>();
        for (KhoaHoc k : repo.layDanhSach(maCoSo())) {
            if (trangThai != null && trangThai != k.trangThai()) continue;
            if (!tim.isEmpty() && !boDau(k.tenKhoaHoc()).contains(tim)
                    && !boDau(k.tenMonHoc()).contains(tim)
                    && !String.valueOf(k.maKhoaHoc()).contains(tim)) continue;
            ketQua.add(k);
        }
        return ketQua;
    }

    public List<MonHoc> layMonHoc() throws SQLException {
        return monRepo.layDanhSach(maCoSo());
    }

    public int them(int maMonHoc, String ten, int soBuoi, BigDecimal hocPhi,
                   boolean trangThai) throws SQLException {
        kiemTra(maMonHoc, ten, soBuoi, hocPhi);
        return repo.them(maCoSo(), maMonHoc, ten.trim(), soBuoi, hocPhi, trangThai);
    }

    public void sua(int maKhoaHoc, int maMonHoc, String ten, int soBuoi,
                   BigDecimal hocPhi, boolean trangThai) throws SQLException {
        if (maKhoaHoc <= 0) throw new IllegalArgumentException("Ma khoa hoc khong hop le");
        kiemTra(maMonHoc, ten, soBuoi, hocPhi);
        repo.sua(maCoSo(), maKhoaHoc, maMonHoc, ten.trim(), soBuoi, hocPhi, trangThai);
    }

    private static void kiemTra(int maMonHoc, String ten, int soBuoi, BigDecimal hocPhi) {
        if (maMonHoc <= 0) throw new IllegalArgumentException("Hay chon mon hoc");
        if (ten == null || ten.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui long nhap ten khoa hoc");
        }
        if (ten.trim().length() > 200) {
            throw new IllegalArgumentException("Ten khoa hoc toi da 200 ky tu");
        }
        if (soBuoi <= 0) throw new IllegalArgumentException("So buoi phai lon hon 0");
        if (hocPhi == null || hocPhi.signum() < 0 || hocPhi.scale() > 2
                || hocPhi.precision() - hocPhi.scale() > 16) {
            throw new IllegalArgumentException("Hoc phi phai >= 0 va dung DECIMAL(18,2)");
        }
    }

    private static String boDau(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}
