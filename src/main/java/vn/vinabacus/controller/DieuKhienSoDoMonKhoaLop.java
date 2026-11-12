package vn.vinabacus.controller;

import vn.vinabacus.service.XuLySoDoMonKhoaLop;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Cung cấp dữ liệu sơ đồ cho View; nghiệp vụ tra cứu nằm trong Service. */
public final class DieuKhienSoDoMonKhoaLop {
    private final XuLySoDoMonKhoaLop xuLy;

    public DieuKhienSoDoMonKhoaLop() { this(new XuLySoDoMonKhoaLop()); }
    public DieuKhienSoDoMonKhoaLop(XuLySoDoMonKhoaLop xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }
    public List<String> layMonHoc() { return xuLy.layMonHoc(); }
    public List<String> layKhoaHoc(String monHoc) { return xuLy.layKhoaHoc(monHoc); }
    public Map<String, String> layThongTinKhoa(String monHoc, String khoaHoc) {
        return xuLy.layThongTinKhoa(monHoc, khoaHoc);
    }
    public List<Map<String, String>> layLopHoc(String monHoc, String khoaHoc) {
        return xuLy.layLopHoc(monHoc, khoaHoc);
    }
}
