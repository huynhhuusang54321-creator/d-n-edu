package vn.vinabacus.service;

import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.MoTaManHinh;
import vn.vinabacus.repository.KhoDuLieuManHinh;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Tra cứu quan hệ môn – khóa – lớp từ kho dữ liệu mẫu. */
public final class XuLySoDoMonKhoaLop {
    private List<Map<String, String>> layDong(String menu, String tab) {
        for (MoTaManHinh.Trang trang : KhoDuLieuManHinh.layTrang(CoSo.TRUNG_TAM, menu)) {
            if (trang.tab().equals(tab)) return new ArrayList<>(KhoDuLieuManHinh.lay(trang));
        }
        return List.of();
    }

    public List<String> layMonHoc() {
        LinkedHashSet<String> tenMon = new LinkedHashSet<>();
        for (Map<String, String> row : layDong("Quản lý khóa học", "Môn học")) {
            themTen(tenMon, row.get("Tên môn học"));
        }
        for (Map<String, String> row : layDong("Quản lý khóa học", "Khóa học")) {
            themTen(tenMon, row.get("Môn học"));
        }
        return List.copyOf(tenMon);
    }

    public List<String> layKhoaHoc(String monHoc) {
        LinkedHashSet<String> tenKhoa = new LinkedHashSet<>();
        for (Map<String, String> row : layDong("Quản lý khóa học", "Khóa học")) {
            if (Objects.equals(monHoc, row.get("Môn học"))) themTen(tenKhoa, row.get("Tên khóa"));
        }
        return List.copyOf(tenKhoa);
    }

    public Map<String, String> layThongTinKhoa(String monHoc, String khoaHoc) {
        for (Map<String, String> row : layDong("Quản lý khóa học", "Khóa học")) {
            if (Objects.equals(monHoc, row.get("Môn học")) && Objects.equals(khoaHoc, row.get("Tên khóa"))) {
                return Map.copyOf(row);
            }
        }
        return Map.of();
    }

    public List<Map<String, String>> layLopHoc(String monHoc, String khoaHoc) {
        if (layThongTinKhoa(monHoc, khoaHoc).isEmpty()) return List.of();
        List<Map<String, String>> ketQua = new ArrayList<>();
        for (Map<String, String> row : layDong("Quản lý lớp học", "Danh sách lớp")) {
            if (Objects.equals(khoaHoc, row.get("Khóa học/Năm học"))
                    && (!row.containsKey("Môn học") || Objects.equals(monHoc, row.get("Môn học")))) {
                ketQua.add(Map.copyOf(row));
            }
        }
        return List.copyOf(ketQua);
    }

    private static void themTen(LinkedHashSet<String> ten, String giaTri) {
        if (giaTri != null && !giaTri.isBlank()) ten.add(giaTri);
    }
}
