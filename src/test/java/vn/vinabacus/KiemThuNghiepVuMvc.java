package vn.vinabacus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.vinabacus.controller.DieuKhienTrangChu;
import vn.vinabacus.controller.DieuKhienPhanHe;
import vn.vinabacus.controller.DieuKhienSoDoMonKhoaLop;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KetQuaGoiNam;
import vn.vinabacus.model.KetQuaPhieuThu;
import vn.vinabacus.model.MoTaManHinh.Trang;

/** Kiểm thử luồng Controller -> Service -> Repository -> Data, không cần Swing/SQL. */
public final class KiemThuNghiepVuMvc {
    private KiemThuNghiepVuMvc() {}

    public static void main(String[] args) throws Exception {
        DieuKhienPhanHe mamNon = new DieuKhienPhanHe(CoSo.MAM_NON);
        DieuKhienPhanHe trungTam = new DieuKhienPhanHe(CoSo.TRUNG_TAM);
        kiemThuBieuMauVaCsv(mamNon, trungTam);
        kiemThuDiemDanh(mamNon, trungTam);
        kiemThuKho(mamNon, trungTam);
        kiemThuPhieuThu(mamNon, trungTam);
        kiemThuGoiNam(mamNon, trungTam);
        kiemThuSoDoVaTrangChu();
        kiemThuHocBuVaChotThang(mamNon, trungTam);
        System.out.println("PASS: MVC form/CRUD, snapshots, CSV, attendance, stock, receipts, annual packages, hierarchy and month lock.");
    }

    private static void kiemThuBieuMauVaCsv(DieuKhienPhanHe mn, DieuKhienPhanHe tt) throws Exception {
        Trang students = mn.timTrang("Quản lý học viên", "Danh sách học viên");
        int count = mn.lay(students).size();
        String originalName = mn.lay(students).get(0).get("Họ và tên");
        List<LinkedHashMap<String,String>> snapshot = mn.lay(students);
        snapshot.get(0).put("Họ và tên", "Không được sửa kho qua snapshot");
        snapshot.clear();
        check(mn.lay(students).size() == count, "Danh sách trả về phải tách khỏi kho");
        check(originalName.equals(mn.lay(students).get(0).get("Họ và tên")), "Map trả về phải tách khỏi kho");
        expectInvalid(() -> tt.lay(students));
        expectInvalid(() -> mn.luuBieuMau(students, -1, Map.of()));

        String specialName = "MVC, \"Đỗ An\"\nkiểm thử";
        Map<String,String> input = new LinkedHashMap<>();
        input.put("Họ và tên", specialName);
        input.put("Ngày sinh", "01/01/2020");
        input.put("Người liên hệ", "Người liên hệ kiểm thử");
        input.put("SĐT liên hệ", "123");
        input.put("Trạng thái", "Đang học");
        expectInvalid(() -> mn.luuBieuMau(students, -1, input));
        check(mn.lay(students).size() == count, "Lỗi validation không được tạo bản ghi");
        input.put("SĐT liên hệ", "0901234567");
        mn.luuBieuMau(students, -1, input);
        check(mn.lay(students).size() == count + 1, "Thêm học viên");
        Map<String,String> added = new LinkedHashMap<>(mn.lay(students).get(count));
        check(!added.get("Mã học viên").isBlank(), "Tự sinh mã khi để trống");
        check(specialName.equals(added.get("Họ và tên")), "Giữ nguyên ký tự Unicode và CSV đặc biệt");
        added.put("Ghi chú", "Thông tin ngoài các cột hiển thị");
        mn.luuBieuMau(students, count, added);
        check("Thông tin ngoài các cột hiển thị".equals(mn.lay(students).get(count).get("Ghi chú")), "Sửa phải lưu trường ngoài bảng");

        Path csv = Files.createTempFile("vinabacus-mvc-", ".csv");
        try {
            mn.xuatCSV(students, csv);
            String contents = Files.readString(csv, StandardCharsets.UTF_8);
            check(contents.charAt(0) == '\ufeff', "CSV UTF-8 có BOM");
            List<List<String>> parsed = parseCsv(contents.substring(1));
            check(parsed.size() == count + 2, "CSV phải có tiêu đề và đủ các dòng");
            check(parsed.get(0).equals(List.of(students.cot())), "Tiêu đề CSV giữ thứ tự cột");
            int nameIndex = List.of(students.cot()).indexOf("Họ và tên");
            check(specialName.equals(parsed.get(count + 1).get(nameIndex)), "CSV đọc lại được dấu phẩy, dấu nháy và xuống dòng");
        } finally {
            Files.deleteIfExists(csv);
            mn.xoa(students, count);
        }
        check(mn.lay(students).size() == count, "Xóa học viên");
    }

    private static void kiemThuDiemDanh(DieuKhienPhanHe mn, DieuKhienPhanHe tt) {
        String date = "17/02/2031", classroom = "Lớp MVC", session = "B_MVC_1", student = "HV_MVC_001";
        List<Map<String,String>> present = List.of(Map.of("Mã học viên", student, "Họ và tên", "Học viên MVC", "Trạng thái", "Có mặt"));
        Trang mnHistory = mn.timTrang("Điểm danh", "Bảng điểm danh");
        Trang ttHistory = tt.timTrang("Điểm danh", "Bảng điểm danh");
        int mnCount = mn.lay(mnHistory).size(), ttCount = tt.lay(ttHistory).size();
        expectInvalid(() -> mn.luuDiemDanh("31/02/2031", classroom, session, present));
        expectInvalid(() -> mn.luuDiemDanh(date, classroom, session, List.of(Map.of("Mã học viên", student, "Trạng thái", "Sai trạng thái"))));
        check(mn.lay(mnHistory).size() == mnCount, "Điểm danh lỗi không ghi lịch sử");
        check(mn.luuDiemDanh(date, classroom, session, present) == 1, "Đếm số học viên đã điểm danh");
        check(tt.trangThaiDiemDanh(date, classroom, session, student) == null, "Điểm danh phải tách theo cơ sở");
        List<Map<String,String>> absent = List.of(Map.of("Mã học viên", student, "Họ và tên", "Học viên MVC", "Trạng thái", "Vắng có phép", "Ghi chú", "Có đơn xin phép"));
        mn.luuDiemDanh(date, classroom, "B_MVC_2", absent);
        check(mn.lay(mnHistory).size() == mnCount + 1, "Mầm non cập nhật cùng ngày thay vì tạo trùng theo buổi");
        check("Vắng có phép".equals(mn.trangThaiDiemDanh(date, classroom, session, student)), "Mầm non dùng khóa ngày");
        check(tt.luuDiemDanh(date, classroom, session, present) == 1, "Lưu điểm danh Trung tâm");
        tt.luuDiemDanh(date, classroom, session, absent);
        check(tt.lay(ttHistory).size() == ttCount + 1, "Cùng buổi phải cập nhật lịch sử");
        tt.luuDiemDanh(date, classroom, "B_MVC_2", present);
        check(tt.lay(ttHistory).size() == ttCount + 2, "Trung tâm giữ hai buổi riêng");
        check("Vắng có phép".equals(tt.trangThaiDiemDanh(date, classroom, session, student)), "Giữ trạng thái buổi đầu");
        check("Có mặt".equals(tt.trangThaiDiemDanh(date, classroom, "B_MVC_2", student)), "Giữ trạng thái buổi sau");
    }

    private static void kiemThuKho(DieuKhienPhanHe mn, DieuKhienPhanHe tt) {
        Trang stock = tt.timTrang("Kho – Dụng cụ", "Tồn kho theo tháng");
        Trang history = tt.timTrang("Kho – Dụng cụ", "Lịch sử nhập xuất");
        Trang otherStock = mn.timTrang("Kho – Dụng cụ", "Tồn kho theo tháng");
        long before = tt.docSo(tt.lay(stock).get(0).get("Tồn cuối"));
        long imported = tt.docSo(tt.lay(stock).get(0).get("Nhập"));
        String otherBefore = mn.lay(otherStock).get(0).get("Tồn cuối");
        int historyCount = tt.lay(history).size();
        check(tt.capNhatKho("Nhập kho", 0, "7", "Nhập kiểm thử MVC") == before + 7, "Tính tồn sau nhập");
        check(tt.docSo(tt.lay(stock).get(0).get("Tồn cuối")) == before + 7, "Lưu tồn sau nhập");
        check(tt.docSo(tt.lay(stock).get(0).get("Nhập")) == imported + 7, "Cộng cột nhập");
        check(tt.lay(history).size() == historyCount + 1, "Tạo lịch sử nhập kho");
        Map<String,String> movement = tt.lay(history).get(historyCount);
        check("Nhập".equals(movement.get("Loại")) && "7".equals(movement.get("Số lượng")), "Lịch sử đúng loại và số lượng");
        expectInvalid(() -> tt.capNhatKho("Xuất HS", 0, String.valueOf(before + 8), "Không đủ tồn"));
        expectInvalid(() -> tt.capNhatKho("Nhập kho", 0, "-1", "Số lượng âm"));
        check(tt.docSo(tt.lay(stock).get(0).get("Tồn cuối")) == before + 7, "Xuất lỗi không đổi tồn");
        check(tt.lay(history).size() == historyCount + 1, "Xuất lỗi không tạo lịch sử");
        check(tt.capNhatKho("Xuất HS", 0, "3", "Xuất kiểm thử MVC") == before + 4, "Xuất hợp lệ trừ tồn");
        check(tt.docSo(tt.lay(stock).get(0).get("Tồn cuối")) == before + 4, "Lưu tồn sau xuất");
        check(tt.lay(history).size() == historyCount + 2, "Lưu lịch sử xuất");
        check(otherBefore.equals(mn.lay(otherStock).get(0).get("Tồn cuối")), "Tồn kho tách theo cơ sở");
    }

    private static void kiemThuPhieuThu(DieuKhienPhanHe mn, DieuKhienPhanHe tt) {
        Trang debts = tt.timTrang("Học phí", "Khoản phải thu");
        Trang receipts = tt.timTrang("Học phí", "Phiếu thu");
        Trang credits = tt.timTrang("Học phí", "Tín dụng học phí");
        Trang mnDebts = mn.timTrang("Học phí & Tiền ăn", "Bảng tính phí tháng");
        List<LinkedHashMap<String,String>> mnBefore = mn.lay(mnDebts);
        Map<String,String> original = tt.lay(debts).get(0);
        String student = original.get("Học viên");
        long owed = tt.docSo(original.get("Phải thu")) - tt.docSo(original.get("Đã đóng"));
        int receiptCount = tt.lay(receipts).size(), creditCount = tt.lay(credits).size();
        expectInvalid(() -> tt.lapPhieuThu(student, "0", "Tiền mặt", List.of(0)));
        expectInvalid(() -> tt.lapPhieuThu("Học viên khác", "100000", "Tiền mặt", List.of(0)));
        check(tt.lay(debts).get(0).equals(original) && tt.lay(receipts).size() == receiptCount, "Phiếu không hợp lệ không thay công nợ");
        long extra = 123456, money = owed + extra;
        KetQuaPhieuThu result = tt.lapPhieuThu(student, String.valueOf(money), "Chuyển khoản", List.of(0, 0));
        check(result.phanBo() == owed && result.thuDu() == extra, "Phân bổ đúng khoản còn nợ và tiền thu dư");
        Map<String,String> after = tt.lay(debts).get(0);
        check(tt.docSo(after.get("Đã đóng")) == tt.docSo(original.get("Phải thu")), "Khoản phải thu được ghi nhận đã đóng");
        check("0".equals(after.get("Còn nợ")) && "Đủ".equals(after.get("Trạng thái")), "Tất toán công nợ");
        check(tt.lay(receipts).size() == receiptCount + 1, "Tạo phiếu thu");
        check(String.valueOf(money).equals(tt.lay(receipts).get(receiptCount).get("Số tiền thực thu")), "Phiếu lưu đúng thực thu");
        check(tt.lay(credits).size() == creditCount + 1 && String.valueOf(extra).equals(tt.lay(credits).get(creditCount).get("Còn lại")), "Chỉ số dư thực thu sinh tín dụng");
        check(mn.lay(mnDebts).equals(mnBefore), "Phiếu Trung tâm không sửa công nợ Mầm non");
    }

    private static void kiemThuGoiNam(DieuKhienPhanHe mn, DieuKhienPhanHe tt) {
        Trang packages = tt.timTrang("Quản lý khóa học", "Gói học 1 năm");
        int count = tt.lay(packages).size();
        KetQuaGoiNam result = tt.tinhGoiNam(List.of(0, 1, 0), 0);
        check(result.soKhoa() == 2 && result.tong() == 6480000, "Gói năm chỉ tính mỗi khóa một lần");
        check(result.giam() == 648000 && result.phaiThu() == 5832000, "Chọn ưu đãi cao nhất và tính phải thu");
        check(tt.tinhGoiNam(List.of(0, 1), 2).giam() == 500000, "Ưu đãi tiền cố định");
        check(tt.tinhGoiNam(List.of(0, 1), 3).giam() == 0, "Cho phép không áp dụng ưu đãi");
        expectInvalid(() -> mn.tinhGoiNam(List.of(0, 1), 0));
        expectInvalid(() -> tt.dangKyGoiNam("Học viên MVC", "01/12/2026", "01/12/2027", List.of(0), 0));
        expectInvalid(() -> tt.dangKyGoiNam("Học viên MVC", "01/12/2026", "01/12/2026", List.of(0, 1), 0));
        check(tt.lay(packages).size() == count, "Đăng ký gói lỗi không được lưu");
        KetQuaGoiNam saved = tt.dangKyGoiNam("Học viên MVC", "01/12/2026", "01/12/2027", List.of(0, 1), 0);
        check(saved.equals(result) && tt.lay(packages).size() == count + 1, "Lưu đăng ký gói năm");
        Map<String,String> row = tt.lay(packages).get(count);
        check("5832000".equals(row.get("Phải thu")) && "Học viên MVC".equals(row.get("Học viên")), "Giữ đúng người học và số tiền gói");
    }

    private static void kiemThuSoDoVaTrangChu() {
        DieuKhienSoDoMonKhoaLop diagram = new DieuKhienSoDoMonKhoaLop();
        check(diagram.layMonHoc().containsAll(List.of("Tiếng Anh", "Toán tư duy")), "Sơ đồ có các môn mẫu");
        check(diagram.layKhoaHoc("Tiếng Anh").contains("Tiếng Anh A1"), "Lọc khóa thuộc môn");
        check(!diagram.layKhoaHoc("Tiếng Anh").contains("Kid 1"), "Loại khóa của môn khác");
        check(!diagram.layLopHoc("Tiếng Anh", "Tiếng Anh A1").isEmpty(), "Tra cứu các lớp của khóa");
        for (Map<String,String> row : diagram.layLopHoc("Tiếng Anh", "Tiếng Anh A1"))
            check("Tiếng Anh A1".equals(row.get("Khóa học/Năm học")), "Lớp thuộc đúng khóa");
        check(diagram.layThongTinKhoa("Toán tư duy", "Tiếng Anh A1").isEmpty(), "Không tra khóa nhầm môn");
        check(diagram.layLopHoc("Toán tư duy", "Tiếng Anh A1").isEmpty(), "Không hiển thị lớp cho cặp môn/khóa sai");
        DieuKhienTrangChu home = new DieuKhienTrangChu(CoSo.MAM_NON);
        check(home.laySoLieu().hocVien() == 128 && home.layLich().size() == 5, "Dashboard đi qua controller");
    }

    private static void kiemThuHocBuVaChotThang(DieuKhienPhanHe mn, DieuKhienPhanHe tt) {
        Trang makeup = tt.timTrang("Học bù / Đổi buổi", "Xét học bù");
        int count = tt.lay(makeup).size();
        expectInvalid(() -> tt.xetHocBu("Học viên MVC", "Tiếng Anh A1", "Vắng không phép", "0", "20/12/2026", "Xin học bù"));
        expectInvalid(() -> tt.xetHocBu("Học viên MVC", "Tiếng Anh A1", "Vắng có phép", "3", "20/12/2026", "Xin học bù"));
        check(tt.lay(makeup).size() == count, "Không lưu yêu cầu học bù không hợp lệ");
        tt.xetHocBu("Học viên MVC", "Tiếng Anh A1", "Vắng có phép", "2", "20/12/2026", "Có đơn xin phép");
        check(tt.lay(makeup).size() == count + 1, "Lưu yêu cầu học bù hợp lệ");
        Trang month = mn.timTrang("Học phí & Tiền ăn", "Chốt tháng");
        Map<String,String> before = new LinkedHashMap<>(mn.lay(month).get(0));
        try {
            mn.chotThang();
            check("Đã chốt".equals(mn.lay(month).get(0).get("Trạng thái")), "Lưu trạng thái chốt tháng");
            String date = "01/" + before.get("Tháng");
            expectInvalid(() -> mn.luuDiemDanh(date, "Lá 1", "Cả ngày", List.of(Map.of("Mã học viên", "HV_MVC_LOCK", "Trạng thái", "Có mặt"))));
            expectInvalid(tt::chotThang);
        } finally {
            mn.luuBieuMau(month, 0, before);
        }
    }

    private static void expectInvalid(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Phải từ chối yêu cầu không hợp lệ");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Bộ đọc nhỏ để kiểm tra roundtrip CSV, gồm ô có dấu nháy và xuống dòng. */
    private static List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { row.add(cell.toString()); cell.setLength(0); }
            else if (c == '\r' && !quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                row.add(cell.toString()); rows.add(row); row = new ArrayList<>(); cell.setLength(0); i++;
            } else cell.append(c);
        }
        check(!quoted && row.isEmpty() && cell.length() == 0, "CSV phải kết thúc đủ dòng và dấu nháy");
        return rows;
    }
}
