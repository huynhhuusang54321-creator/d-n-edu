package vn.vinabacus.model;

import java.util.List;

/** Cơ sở đang xem; chỉ dùng trong bản demo UI, chưa phải phân quyền đăng nhập. */
public enum CoSo {
    MAM_NON("Mầm non"),
    TRUNG_TAM("Trung tâm");

    private final String ten;
    CoSo(String ten) { this.ten = ten; }
    public String layTen() { return ten; }

    public List<String> layMenu() {
        if (this == MAM_NON) {
            return List.of("Trang chủ", "Quản lý học viên", "Quản lý lớp học", "Sắp lịch học",
                    "Điểm danh", "Học phí & Tiền ăn", "Tuyển sinh", "Kho – Dụng cụ",
                    "Báo cáo – Thống kê", "Hệ thống");
        }
        return List.of("Trang chủ", "Quản lý học viên", "Quản lý khóa học", "Quản lý lớp học",
                "Sắp lịch học", "Điểm danh", "Học phí", "Học bù / Đổi buổi",
                "Tuyển sinh", "Kho – Dụng cụ", "Báo cáo – Thống kê", "Hệ thống");
    }
}
