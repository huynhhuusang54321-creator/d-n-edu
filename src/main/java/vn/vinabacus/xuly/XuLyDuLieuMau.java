package vn.vinabacus.xuly;

import java.time.LocalDate;
import java.util.List;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DuLieuMau;

/**
 * Dữ liệu MẪU, không có JDBC hay bất cứ truy vấn CSDL nào.
 * Sẽ thay bằng Service + DAO thật sau khi hoàn thành các màn hình và đăng nhập.
 */
public final class XuLyDuLieuMau {
    private XuLyDuLieuMau() {}

    public static DuLieuMau.SoLieu laySoLieu(CoSo coSo) {
        return coSo == CoSo.MAM_NON
                ? new DuLieuMau.SoLieu(128, 6, 120, 5, 3)
                : new DuLieuMau.SoLieu(256, 18, 192, 9, 4);
    }

    public static List<DuLieuMau.Lich> layLich(CoSo coSo) {
        if (coSo == CoSo.MAM_NON) {
            return List.of(
                    new DuLieuMau.Lich("07:00 – 08:00", List.of("Đón học sinh")),
                    new DuLieuMau.Lich("08:00 – 08:30", List.of("Ăn sáng")),
                    new DuLieuMau.Lich("09:00 – 10:00", List.of("Hoạt động học")),
                    new DuLieuMau.Lich("11:00 – 13:30", List.of("Ăn trưa & Nghỉ trưa")),
                    new DuLieuMau.Lich("14:00 – 16:00", List.of("Hoạt động chiều")));
        }
        // Đúng yêu cầu nghiệp vụ: một khung giờ có thể có nhiều lớp.
        return List.of(
                new DuLieuMau.Lich("08:00 – 09:30", List.of("Toán tư duy – K1", "Tiếng Anh – K1")),
                new DuLieuMau.Lich("10:00 – 11:30", List.of("Tiếng Anh – K2", "Abacus – K1")),
                new DuLieuMau.Lich("14:00 – 15:30", List.of("Abacus – K1", "Tiếng Anh – K3")),
                new DuLieuMau.Lich("16:00 – 17:30", List.of("Toán tư duy – K2", "Tiếng Anh – K4", "Abacus – K2")),
                new DuLieuMau.Lich("18:00 – 19:30", List.of("Toán tư duy – K2", "Tiếng Anh – K5")));
    }

    /** Một ngày khác hiện tại chưa có dữ liệu mẫu, không bịa ra lịch sử điểm danh. */
    public static boolean coDuLieuMauTrongNgay(LocalDate ngay) {
        return LocalDate.now().equals(ngay);
    }

    public static DuLieuMau.Bang layBang(CoSo coSo, String muc) {
        return switch (muc) {
            case "Quản lý học viên" -> new DuLieuMau.Bang(
                    new String[]{"Mã HV", "Họ và tên", "Ngày sinh", "Môn / Lớp", "Trạng thái"},
                    new Object[][]{{"HV001", "Nguyễn Minh Anh", "12/03/2018", coSo == CoSo.MAM_NON ? "Lớp Lá A" : "Tiếng Anh – K1", "Đang học"},
                        {"HV002", "Trần Gia Bảo", "09/11/2017", coSo == CoSo.MAM_NON ? "Lớp Chồi A" : "Toán tư duy – K2", "Đang học"},
                        {"HV003", "Lê Khánh Linh", "25/07/2019", coSo == CoSo.MAM_NON ? "Lớp Mầm A" : "Abacus – K1", "Đang học"}},
                    "Hồ sơ học viên, danh sách môn – khóa – lớp và lịch sử học tập.");
            case "Quản lý khóa học" -> new DuLieuMau.Bang(
                    new String[]{"Mã khóa", "Môn học", "Tên khóa", "Số buổi", "Trạng thái"},
                    new Object[][]{{"KH001", "Tiếng Anh", "Happy Kids Starter", 32, "Đang mở"},
                        {"KH002", "Toán tư duy", "Kid 1", 32, "Đang mở"},
                        {"KH003", "Tiếng Anh", "Cambridge Starters", 16, "Đang mở"}},
                    "Quản lý môn học, khóa học và đăng ký gói một năm có giảm giá.");
            case "Quản lý lớp học" -> new DuLieuMau.Bang(
                    new String[]{"Mã lớp", "Tên lớp", "Giáo viên", "Học viên", "Trạng thái"},
                    coSo == CoSo.MAM_NON
                      ? new Object[][]{{"MN01", "Mầm A", "Nguyễn Thị Hoa", 21, "Đang học"},
                         {"MN02", "Chồi A", "Trần Thị Mai", 22, "Đang học"},
                         {"MN03", "Lá A", "Lê Thị Hương", 20, "Đang học"}}
                      : new Object[][]{{"TT01", "Toán tư duy – K1", "Nguyễn Thị Hoa", 18, "Đang học"},
                         {"TT02", "Tiếng Anh – K1", "Trần Thị Mai", 16, "Đang học"},
                         {"TT03", "Abacus – K1", "Lê Thị Hương", 15, "Đang học"}},
                    "Xếp lớp, phân công giáo viên và xem học viên trong từng lớp.");
            case "Sắp lịch học" -> new DuLieuMau.Bang(
                    new String[]{"Thứ", "Khung giờ", "Lớp / Hoạt động", "Giáo viên", "Trạng thái"},
                    coSo == CoSo.MAM_NON
                      ? new Object[][]{{"Thứ Hai", "07:00 – 08:00", "Đón học sinh", "Giáo viên trực", "Đã xếp"},
                        {"Thứ Hai", "09:00 – 10:00", "Hoạt động học", "Giáo viên lớp", "Đã xếp"}}
                      : new Object[][]{{"Thứ Hai", "08:00 – 09:30", "Toán tư duy – K1", "Nguyễn Thị Hoa", "Đã xếp"},
                        {"Thứ Hai", "08:00 – 09:30", "Tiếng Anh – K1", "Trần Thị Mai", "Đã xếp"},
                        {"Thứ Hai", "10:00 – 11:30", "Abacus – K1", "Lê Thị Hương", "Đã xếp"}},
                    "Các lớp có thể học cùng khung giờ; việc kiểm tra trùng giáo viên sẽ triển khai ở Service.");
            case "Điểm danh" -> new DuLieuMau.Bang(
                    new String[]{"Mã HV", "Họ và tên", "Lớp", "Ngày / Buổi", "Trạng thái"},
                    new Object[][]{{"HV001", "Nguyễn Minh Anh", coSo == CoSo.MAM_NON ? "Lá A" : "Tiếng Anh – K1", "Hôm nay", "Có mặt"},
                        {"HV002", "Trần Gia Bảo", coSo == CoSo.MAM_NON ? "Chồi A" : "Toán tư duy – K2", "Hôm nay", "Vắng có phép"},
                        {"HV003", "Lê Khánh Linh", coSo == CoSo.MAM_NON ? "Mầm A" : "Abacus – K1", "Hôm nay", "Có mặt"}},
                    coSo == CoSo.MAM_NON ? "Mầm non điểm danh theo ngày, không theo dõi ăn/ngủ riêng." :
                        "Trung tâm điểm danh theo từng buổi; mỗi đăng ký khóa có tối đa 3 buổi học bù có phép.");
            case "Học phí", "Học phí & Tiền ăn" -> new DuLieuMau.Bang(
                    new String[]{"Mã HV", "Họ và tên", "Khoản thu", "Phải thu", "Tình trạng"},
                    coSo == CoSo.MAM_NON
                      ? new Object[][]{{"HV001", "Nguyễn Minh Anh", "Học phí tháng", "2.000.000", "Đã đóng"},
                        {"HV002", "Trần Gia Bảo", "Tiền ăn tháng", "850.000", "Còn thiếu"}}
                      : new Object[][]{{"HV001", "Nguyễn Minh Anh", "Khóa Tiếng Anh", "3.190.000", "Đã đóng"},
                        {"HV002", "Trần Gia Bảo", "Gói học năm", "9.000.000", "Đang thanh toán"}},
                    "Phiếu thu, phân bổ tiền và công nợ được triển khai sau khi kết nối SQL Server.");
            case "Học bù / Đổi buổi" -> new DuLieuMau.Bang(
                    new String[]{"Mã HV", "Họ và tên", "Lớp", "Loại", "Trạng thái"},
                    new Object[][]{{"HV001", "Nguyễn Minh Anh", "Tiếng Anh – K1", "Đổi buổi", "Đang xét"},
                        {"HV002", "Trần Gia Bảo", "Toán tư duy – K2", "Học bù", "Đã xếp"}},
                    "Đổi buổi trong tuần khác với học bù; học bù tối đa 3 buổi mỗi đăng ký khóa.");
            case "Tuyển sinh" -> new DuLieuMau.Bang(
                    new String[]{"Mã", "Người liên hệ", "Số điện thoại", "Giai đoạn", "Ghi chú"},
                    new Object[][]{{"TS001", "Nguyễn Văn B", "09xx xxx xxx", "Tiếp nhận", "Quan tâm tiếng Anh"},
                        {"TS002", "Trần Thị C", "09xx xxx xxx", "Học thử", "Chờ phản hồi"}},
                    "Quy trình: Tiếp nhận → Học thử → Chờ xác nhận → Xếp lớp → Nhập học chính thức.");
            case "Kho – Dụng cụ" -> new DuLieuMau.Bang(
                    new String[]{"Mã SP", "Tên hàng hóa", "ĐVT", "Tồn đầu", "Tồn cuối"},
                    new Object[][]{{"SP001", "Bút chì", "Cây", 100, 85}, {"SP002", "Vở bài tập", "Quyển", 50, 42},
                        {"SP003", "Huy hiệu", "Cái", 60, 54}},
                    "Theo dõi Nhập, Xuất HS, Xuất CS, Kiểm kê và Xuất Excel từng cơ sở.");
            case "Báo cáo – Thống kê" -> new DuLieuMau.Bang(
                    new String[]{"Tên báo cáo", "Phạm vi", "Kỳ", "Trạng thái"},
                    new Object[][]{{"Tình hình học viên", coSo.layTen(), "Tháng hiện tại", "Có thể xem"},
                        {"Điểm danh", coSo.layTen(), "Tháng hiện tại", "Có thể xem"},
                        {"Học phí", coSo.layTen(), "Tháng hiện tại", "Có thể xem"}},
                    "Các báo cáo sẽ liên kết dữ liệu và xuất Excel khi hoàn thiện nghiệp vụ.");
            default -> new DuLieuMau.Bang(
                    new String[]{"Thiết lập", "Giá trị", "Quyền"},
                    new Object[][]{{"Cơ sở làm việc", coSo.layTen(), "Theo tài khoản"},
                        {"Tên ứng dụng", "VINABACUS", "Admin"},
                        {"Bảng màu", "Xanh – Vàng – Trắng", "Dùng chung"}},
                    "Cấu hình tài khoản, phân quyền và nhận diện ứng dụng sẽ hoàn thiện sau.");
        };
    }
}
