package vn.vinabacus.service;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.repository.MonHocRepository;

public final class MonHocService {
    private final CoSo coSo;
    private final MonHocRepository repo = new MonHocRepository();

    public MonHocService(CoSo coSo) {
        if (coSo != CoSo.TRUNG_TAM) {
            throw new IllegalArgumentException("Mon hoc TV3 chi danh cho Trung tam");
        }
        this.coSo = coSo;
    }

    public List<MonHoc> timKiem(String tuKhoa, Boolean trangThai) throws SQLException {
        int maCoSo = repo.layMaCoSo(coSo);
        String tim = boDau(tuKhoa == null ? "" : tuKhoa.trim());
        List<MonHoc> ketQua = new ArrayList<>();
        for (MonHoc m : repo.layDanhSach(maCoSo)) {
            if (trangThai != null && trangThai.booleanValue() != m.trangThai()) continue;
            if (!tim.isBlank() && !boDau(m.tenMonHoc()).contains(tim)
                    && !String.valueOf(m.maMonHoc()).contains(tim)) continue;
            ketQua.add(m);
        }
        return ketQua;
    }

    public int them(String ten, String ghiChu, boolean trangThai) throws SQLException {
        kiemTra(ten, ghiChu);
        return repo.them(repo.layMaCoSo(coSo), ten.trim(), rongThanhNull(ghiChu), trangThai);
    }

    public void sua(int maMonHoc, String ten, String ghiChu, boolean trangThai)
            throws SQLException {
        if (maMonHoc <= 0) throw new IllegalArgumentException("Ma mon hoc khong hop le");
        kiemTra(ten, ghiChu);
        repo.sua(repo.layMaCoSo(coSo), maMonHoc, ten.trim(), rongThanhNull(ghiChu), trangThai);
    }

    private static void kiemTra(String ten, String ghiChu) {
        if (ten == null || ten.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui long nhap ten mon hoc");
        }
        if (ten.trim().length() > 150) {
            throw new IllegalArgumentException("Ten mon hoc toi da 150 ky tu");
        }
        if (ghiChu != null && ghiChu.length() > 500) {
            throw new IllegalArgumentException("Ghi chu toi da 500 ky tu");
        }
    }

    private static String rongThanhNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String boDau(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }
}
