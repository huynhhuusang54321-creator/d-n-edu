package vn.vinabacus.config;

import java.util.List;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.MucMenu;

/** Danh muc giao dien: moi co so chi hien thi menu phu hop. Khong truy van SQL. */
public final class MenuConfig {
    private MenuConfig() {}
    private static MucMenu m(String id, String ten, String nhom, String icon) {
        return new MucMenu(id, ten, nhom, icon);
    }

    // Trung tam Toan tu duy - Ngoai ngu: hoc vien, dao tao, tai chinh, van hanh.
    private static final List<MucMenu> TRUNG_TAM = List.of(
            m("tt-trang-chu", "Trang chủ", "CHUNG", "home"),
            m("tt-hoc-vien", "Học viên", "HỌC VIÊN", "people"),
            m("tt-tuyen-sinh", "Tuyển sinh", "HỌC VIÊN", "people"),
            m("tt-dang-ky", "Đăng ký khóa học", "HỌC VIÊN", "check"),
            m("tt-mon-hoc", "Môn học", "ĐÀO TẠO", "book"),
            m("tt-khoa-hoc", "Khóa học", "ĐÀO TẠO", "layers"),
            m("tt-lop-hoc", "Lớp học", "ĐÀO TẠO", "people"),
            m("tt-lich-hoc", "Lịch học", "ĐÀO TẠO", "calendar"),
            m("tt-diem-danh", "Điểm danh", "ĐÀO TẠO", "check"),
            m("tt-hoc-phi", "Học phí", "TÀI CHÍNH", "money"),
            m("tt-phieu-thu", "Phiếu thu", "TÀI CHÍNH", "money"),
            m("tt-nhan-su", "Nhân sự / Giáo viên", "VẬN HÀNH", "people"),
            m("tt-kho", "Kho - Dụng cụ", "VẬN HÀNH", "warehouse"),
            m("tt-bao-cao", "Báo cáo", "VẬN HÀNH", "chart"),
            m("tt-he-thong", "Hệ thống", "VẬN HÀNH", "settings")
    );

    // Mam non: nam hoc, lop mam non, xep lop, diem danh va tien an.
    private static final List<MucMenu> MAM_NON = List.of(
            m("mn-trang-chu", "Trang chủ", "CHUNG", "home"),
            m("mn-hoc-sinh", "Học sinh", "HỌC SINH", "people"),
            m("mn-tuyen-sinh", "Tuyển sinh", "HỌC SINH", "people"),
            m("mn-nam-hoc", "Năm học", "LỚP & CHĂM SÓC", "calendar"),
            m("mn-lop-hoc", "Lớp Mầm non", "LỚP & CHĂM SÓC", "people"),
            m("mn-xep-lop", "Xếp lớp", "LỚP & CHĂM SÓC", "layers"),
            m("mn-diem-danh", "Điểm danh", "LỚP & CHĂM SÓC", "check"),
            m("mn-hoc-phi", "Học phí & Tiền ăn", "TÀI CHÍNH", "money"),
            m("mn-phieu-thu", "Phiếu thu", "TÀI CHÍNH", "money"),
            m("mn-nhan-su", "Nhân sự / Giáo viên", "VẬN HÀNH", "people"),
            m("mn-kho", "Kho - Dụng cụ", "VẬN HÀNH", "warehouse"),
            m("mn-bao-cao", "Báo cáo", "VẬN HÀNH", "chart"),
            m("mn-he-thong", "Hệ thống", "VẬN HÀNH", "settings")
    );

    public static List<MucMenu> choCoSo(CoSo coSo) {
        if (coSo == null) throw new IllegalArgumentException("Thiếu cơ sở");
        return coSo == CoSo.TRUNG_TAM ? TRUNG_TAM : MAM_NON;
    }
}
