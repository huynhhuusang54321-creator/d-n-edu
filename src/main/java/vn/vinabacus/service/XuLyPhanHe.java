package vn.vinabacus.service;

import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KetQuaGoiNam;
import vn.vinabacus.model.KetQuaPhieuThu;
import vn.vinabacus.model.MoTaManHinh.Trang;
import vn.vinabacus.repository.KhoDuLieuManHinh;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;

/** Nghiệp vụ của các màn hình mẫu. Chỉ làm việc với model/repository, không có Swing hay SQL. */
public final class XuLyPhanHe {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Set<String> TRANG_THAI_DIEM_DANH = Set.of(
            "Chưa điểm danh", "Có mặt", "Vắng có phép", "Vắng không phép");
    private final CoSo coSo;

    public XuLyPhanHe(CoSo coSo) { this.coSo = Objects.requireNonNull(coSo); }

    public List<Trang> layTrang(String menu) { return KhoDuLieuManHinh.layTrang(coSo, menu); }

    public vn.vinabacus.model.DuLieuMau.Bang layLichTuan() { return KhoDuLieuManHinh.layLichTuan(); }

    public Trang timTrang(String menu, String tab) {
        return layTrang(menu).stream().filter(t -> t.tab().equals(tab)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Chức năng chưa áp dụng tại cơ sở hiện tại: " + tab));
    }

    private void kiemTraTrang(Trang trang) {
        if (trang == null || trang.coSo() != coSo)
            throw new IllegalArgumentException("Màn hình không thuộc cơ sở đang làm việc.");
    }

    public List<LinkedHashMap<String,String>> lay(Trang trang) {
        kiemTraTrang(trang);
        return KhoDuLieuManHinh.lay(trang);
    }

    public void xoa(Trang trang, int viTri) {
        kiemTraTrang(trang);
        KhoDuLieuManHinh.xoa(trang, viTri);
    }

    public void luuBieuMau(Trang trang, int sua, Map<String,String> input) {
        kiemTraTrang(trang);
        Map<String,String> cleaned = new LinkedHashMap<>();
        for (String raw : trang.truong()) {
            String name = nhanThuc(raw), value = input.getOrDefault(name, "").trim();
            if (raw.contains("*") && value.isBlank())
                throw new IllegalArgumentException("Vui lòng nhập trường bắt buộc: " + name);
            String lower = name.toLowerCase(Locale.ROOT);
            if ((lower.contains("điện thoại") || lower.contains("sđt")) && !value.isBlank()
                    && !value.matches("0[0-9]{9,10}"))
                throw new IllegalArgumentException("Số điện thoại phải gồm 10–11 chữ số và bắt đầu bằng 0.");
            if (laSo(name) && !value.isBlank() && !value.replace(",", "").matches("-?\\d+"))
                throw new IllegalArgumentException("Trường " + name + " phải là số nguyên hợp lệ.");
            cleaned.put(name, value);
        }
        Map<String,String> normalized = sua >= 0 ? new LinkedHashMap<>(lay(trang).get(sua)) : new LinkedHashMap<>();
        normalized.putAll(cleaned);
        for (String column : trang.cot()) {
            if (!cleaned.containsKey(column)) {
                String value = tenDongNghia(column, cleaned);
                if (value != null) normalized.put(column, value);
            }
        }
        if (sua >= 0) KhoDuLieuManHinh.sua(trang, sua, normalized);
        else KhoDuLieuManHinh.them(trang, normalized);
    }

    private static String nhanThuc(String raw) { return raw.split("[#*]", 2)[0].trim(); }

    private static boolean laSo(String name) {
        String s = name.toLowerCase(Locale.ROOT);
        return s.contains("tiền") || s.contains("học phí") || s.contains("giảm giá") || s.contains("số buổi")
                || s.contains("số ngày") || s.contains("sĩ số") || s.contains("đơn giá") || s.contains("phải thu")
                || s.contains("đã thu") || s.contains("đã đóng") || s.contains("số lượng") || s.contains("tồn ")
                || s.contains("khấu trừ") || s.contains("số phát sinh") || s.contains("mức riêng");
    }

    private static String tenDongNghia(String column, Map<String,String> input) {
        for (String key : input.keySet())
            if (key.toLowerCase(Locale.ROOT).replace(" ", "").equals(column.toLowerCase(Locale.ROOT).replace(" ", "")))
                return input.get(key);
        return switch (column) {
            case "Họ và tên", "Họ tên", "Học viên" -> input.getOrDefault("Họ và tên", input.getOrDefault("Học viên", input.get("Họ tên")));
            case "Giờ học", "Khung giờ", "Lịch cố định" -> input.containsKey("Giờ bắt đầu")
                    ? input.get("Giờ bắt đầu") + "–" + input.getOrDefault("Giờ kết thúc", "") : null;
            case "Lớp đang học" -> input.getOrDefault("Lớp", input.get("Lớp đang học"));
            case "Mã SP" -> input.getOrDefault("Mã SP", input.get("Mã hàng hóa"));
            default -> null;
        };
    }

    public List<String> tuyChonTruong(String field) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (field.contains("#")) values.addAll(Arrays.asList(field.substring(field.indexOf('#') + 1).split(",")));
        String name = nhanThuc(field);
        String menu = null, tab = null, column = null;
        switch (name) {
            case "Môn học" -> { menu = "Quản lý khóa học"; tab = "Môn học"; column = "Tên môn học"; }
            case "Học viên" -> { menu = "Quản lý học viên"; tab = "Danh sách học viên"; column = "Họ và tên"; }
            case "Khóa học" -> { menu = "Quản lý khóa học"; tab = "Khóa học"; column = "Tên khóa"; }
            case "Lớp", "Lớp học" -> { menu = "Quản lý lớp học"; tab = "Danh sách lớp"; column = "Tên lớp"; }
            default -> { }
        }
        if (menu != null) {
            for (Trang t : layTrang(menu)) if (t.tab().equals(tab))
                for (Map<String,String> row : lay(t)) {
                    String value = row.get(column);
                    if (value != null && !value.isBlank()) values.add(value);
                }
        }
        if (values.isEmpty()) values.add("Chưa có dữ liệu để chọn");
        return new ArrayList<>(values);
    }

    public String ketNoiHoSo(String name, Map<String,String> row) {
        String id = row.getOrDefault("Mã học viên", row.getOrDefault("Mã", ""));
        String student = row.getOrDefault("Họ và tên", row.getOrDefault("Họ tên", "Học viên"));
        StringBuilder text = new StringBuilder("HỌC VIÊN: " + student + "  •  " + id + "\n\n");
        switch (name) {
            case "Môn & Khóa" -> {
                if (coSo == CoSo.MAM_NON) text.append("Mầm non học theo lớp và năm học; không đăng ký khóa Trung tâm.\n");
                else for (Trang t : layTrang("Quản lý học viên")) if (t.tab().equals(name))
                    for (Map<String,String> r : lay(t)) if (student.equals(r.get("Học viên")))
                        text.append("• ").append(r.get("Môn học")).append(" / ").append(r.get("Khóa học"))
                            .append("\n   Lớp: ").append(r.get("Lớp đang học")).append(" · Đã học ")
                            .append(r.get("Đã học")).append("/").append(r.get("Số buổi chuẩn")).append(" buổi\n");
            }
            case "Lớp đang học" -> text.append("Lớp: ").append(row.getOrDefault("Lớp đang học", "Tra cứu từ bảng xếp lớp")).append("\n");
            case "Điểm danh" -> text.append("Điểm danh theo ").append(coSo == CoSo.MAM_NON ? "ngày" : "buổi")
                    .append(". Chọn menu Điểm danh để xem/lưu kết quả mẫu.\n");
            case "Học phí" -> text.append("Chọn menu Học phí để xem khoản phải thu và phiếu thu tương ứng.\n");
            case "Lịch sử" -> text.append("Lịch sử xếp lớp, chuyển lớp và thay đổi cần thiết được giữ riêng.\n");
            default -> { }
        }
        return text.toString();
    }

    public void xuatCSV(Trang trang, Path target) throws IOException {
        kiemTraTrang(trang);
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            List<String[]> rows = new ArrayList<>(); rows.add(trang.cot());
            for (Map<String,String> row : lay(trang)) {
                String[] cells = new String[trang.cot().length];
                for (int i = 0; i < cells.length; i++) cells[i] = row.getOrDefault(trang.cot()[i], "");
                rows.add(cells);
            }
            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) writer.write(',');
                    writer.write('"'); writer.write(row[i].replace("\"", "\"\"")); writer.write('"');
                }
                writer.write("\r\n");
            }
        }
    }

    public List<Map<String,String>> layHocVienDiemDanh() {
        List<Map<String,String>> rows = new ArrayList<>(lay(timTrang("Quản lý học viên", "Danh sách học viên")));
        List<Map<String,String>> fixtures = KhoDuLieuManHinh.hocVienDiemDanhBoSung();
        for (int i = rows.size(); i < fixtures.size(); i++) rows.add(fixtures.get(i));
        return rows;
    }

    private String khoaDiemDanh(String ngay, String lop, String buoi, String hocVien) {
        return coSo.name() + "|" + ngay + "|" + lop + "|" + (coSo == CoSo.MAM_NON ? "NGAY" : buoi) + "|" + hocVien;
    }

    public String trangThaiDiemDanh(String ngay, String lop, String buoi, String hocVien) {
        return KhoDuLieuManHinh.layDiemDanh(khoaDiemDanh(ngay, lop, buoi, hocVien));
    }

    public int luuDiemDanh(String ngay, String lop, String buoi, List<Map<String,String>> rows) {
        LocalDate date = ngayHopLe(ngay);
        if (lop == null || lop.isBlank() || "null".equals(lop)) throw new IllegalArgumentException("Vui lòng chọn lớp.");
        if (coSo == CoSo.TRUNG_TAM && (buoi == null || buoi.isBlank() || "null".equals(buoi)))
            throw new IllegalArgumentException("Vui lòng chọn buổi học.");
        if (coSo == CoSo.MAM_NON) {
            for (Map<String,String> month : lay(timTrang("Học phí & Tiền ăn", "Chốt tháng")))
                if ("Đã chốt".equals(month.get("Trạng thái")) &&
                        date.format(DateTimeFormatter.ofPattern("MM/uuuu")).equals(month.get("Tháng")))
                    throw new IllegalArgumentException("Tháng mẫu đã chốt. Chưa có xác thực Admin nên không cho sửa điểm danh sau chốt.");
        }
        for (Map<String,String> row : rows) {
            if (!TRANG_THAI_DIEM_DANH.contains(row.getOrDefault("Trạng thái", "")))
                throw new IllegalArgumentException("Trạng thái điểm danh không hợp lệ.");
            if (row.getOrDefault("Mã học viên", "").isBlank()) throw new IllegalArgumentException("Thiếu mã học viên.");
        }
        Trang trang = timTrang("Điểm danh", "Bảng điểm danh");
        String buoiText = coSo == CoSo.MAM_NON ? "Cả ngày" : buoi;
        int completed = 0;
        synchronized (KhoDuLieuManHinh.class) {
            for (Map<String,String> input : rows) {
                String student = input.get("Mã học viên"), status = input.get("Trạng thái");
                KhoDuLieuManHinh.diemDanh(khoaDiemDanh(ngay, lop, buoi, student), status);
                if (!"Chưa điểm danh".equals(status)) completed++;
                List<LinkedHashMap<String,String>> history = lay(trang);
                int found = -1;
                for (int i = 0; i < history.size(); i++) {
                    Map<String,String> r = history.get(i);
                    if (student.equals(r.get("Mã học viên")) && ngay.equals(r.get("Ngày"))
                            && lop.equals(r.get("Lớp")) && Objects.equals(buoiText, r.get("Buổi"))) { found = i; break; }
                }
                Map<String,String> row = new LinkedHashMap<>();
                row.put("Ngày", ngay); row.put("Mã học viên", student); row.put("Học viên", input.getOrDefault("Họ và tên", ""));
                row.put("Lớp", lop); row.put("Buổi", buoiText); row.put("Trạng thái", status); row.put("Ghi chú", input.getOrDefault("Ghi chú", ""));
                if (found < 0) KhoDuLieuManHinh.them(trang, row); else KhoDuLieuManHinh.sua(trang, found, row);
            }
        }
        return completed;
    }

    public KetQuaGoiNam tinhGoiNam(List<Integer> indices, int discount) {
        if (coSo != CoSo.TRUNG_TAM) throw new IllegalArgumentException("Gói học năm chỉ áp dụng tại Trung tâm.");
        List<LinkedHashMap<String,String>> courses = lay(timTrang("Quản lý khóa học", "Khóa học"));
        Set<Integer> selected = new LinkedHashSet<>(indices);
        long total = 0;
        for (int index : selected) total = Math.addExact(total, docSo(courses.get(index).get("Học phí mặc định")));
        long ten = Math.round(total * .1), fixed = Math.min(total, 500000);
        long off = switch (discount) { case 1 -> ten; case 2 -> fixed; case 3 -> 0; default -> Math.max(ten, fixed); };
        return new KetQuaGoiNam(selected.size(), total, off);
    }

    public KetQuaGoiNam dangKyGoiNam(String student, String start, String end, List<Integer> indices, int discount) {
        KetQuaGoiNam result = tinhGoiNam(indices, discount);
        if (result.soKhoa() < 2) throw new IllegalArgumentException("Hãy chọn ít nhất 2 khóa cho gói lộ trình.");
        if (!ngayHopLe(end).isAfter(ngayHopLe(start))) throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
        StringJoiner modules = new StringJoiner(", ");
        List<LinkedHashMap<String,String>> courses = lay(timTrang("Quản lý khóa học", "Khóa học"));
        for (int index : new LinkedHashSet<>(indices)) modules.add(courses.get(index).get("Tên khóa"));
        Map<String,String> row = new LinkedHashMap<>(); row.put("Học viên", student);
        row.put("Các khóa trong lộ trình", modules.toString()); row.put("Ngày bắt đầu", start); row.put("Ngày kết thúc", end);
        row.put("Học phí gốc", String.valueOf(result.tong())); row.put("Giảm giá", String.valueOf(result.giam()));
        row.put("Phải thu", String.valueOf(result.phaiThu())); row.put("Trạng thái", "Đăng ký");
        KhoDuLieuManHinh.them(timTrang("Quản lý khóa học", "Gói học 1 năm"), row);
        return result;
    }

    public KetQuaPhieuThu lapPhieuThu(String student, String amount, String method, List<Integer> selected) {
        long money = docSo(amount);
        if (money <= 0) throw new IllegalArgumentException("Số tiền thu phải lớn hơn 0.");
        boolean mn = coSo == CoSo.MAM_NON;
        String menu = mn ? "Học phí & Tiền ăn" : "Học phí";
        Trang debts = timTrang(menu, mn ? "Bảng tính phí tháng" : "Khoản phải thu");
        Trang receipts = timTrang(menu, "Phiếu thu");
        String paidKey = mn ? "Đã thu" : "Đã đóng";
        synchronized (KhoDuLieuManHinh.class) {
            List<LinkedHashMap<String,String>> rows = lay(debts);
            Map<Integer,Map<String,String>> changes = new LinkedHashMap<>();
            long remaining = money, allocated = 0; StringJoiner names = new StringJoiner(", ");
            for (int index : new LinkedHashSet<>(selected)) {
                Map<String,String> row = new LinkedHashMap<>(rows.get(index));
                if (!student.equals(row.get("Học viên"))) continue;
                long owed = Math.max(0, docSo(row.get("Phải thu")) - docSo(row.get(paidKey)));
                long use = Math.min(owed, remaining); if (use <= 0) continue;
                row.put(paidKey, String.valueOf(Math.addExact(docSo(row.get(paidKey)), use)));
                row.put("Còn nợ", String.valueOf(owed - use));
                if (!mn) row.put("Trạng thái", owed > use ? "Thiếu" : "Đủ");
                changes.put(index, row); allocated += use; remaining -= use;
                names.add(row.getOrDefault(mn ? "Tháng" : "Đăng ký khóa", "Khoản phí"));
            }
            if (allocated == 0) throw new IllegalArgumentException("Hãy chọn ít nhất một khoản còn nợ của học viên đang thu.");
            for (Map.Entry<Integer,Map<String,String>> entry : changes.entrySet()) KhoDuLieuManHinh.sua(debts, entry.getKey(), entry.getValue());
            Map<String,String> receipt = new LinkedHashMap<>(); receipt.put("Ngày thu", LocalDate.now().format(DATE));
            receipt.put("Học viên", student); receipt.put(mn ? "Thực thu" : "Số tiền thực thu", String.valueOf(money));
            receipt.put("Hình thức", method); receipt.put("Các khoản phân bổ", names.toString());
            receipt.put("Người thu", "Admin (demo)"); receipt.put("Trạng thái", "Đã thu");
            KhoDuLieuManHinh.them(receipts, receipt);
            if (remaining > 0 && !mn) {
                Map<String,String> credit = new LinkedHashMap<>(); credit.put("Học viên", student); credit.put("Nguồn tín dụng", "Thu dư phiếu mẫu");
                credit.put("Số phát sinh", String.valueOf(remaining)); credit.put("Đã sử dụng", "0"); credit.put("Còn lại", String.valueOf(remaining));
                credit.put("Ngày cập nhật", LocalDate.now().format(DATE)); KhoDuLieuManHinh.them(timTrang(menu, "Tín dụng học phí"), credit);
            }
            return new KetQuaPhieuThu(allocated, remaining);
        }
    }

    public long capNhatKho(String action, int index, String quantity, String note) {
        if (!Set.of("Nhập kho", "Xuất HS", "Xuất CS", "Kiểm kê").contains(action)) throw new IllegalArgumentException("Thao tác kho không hợp lệ.");
        if (quantity == null || !quantity.trim().matches("\\d+")) throw new IllegalArgumentException("Số lượng phải là số nguyên không âm.");
        long number;
        try { number = Long.parseLong(quantity.trim()); } catch (NumberFormatException ex) { throw new IllegalArgumentException("Số lượng quá lớn."); }
        if (!action.equals("Kiểm kê") && number == 0) throw new IllegalArgumentException("Số lượng cần lớn hơn 0.");
        Trang stock = timTrang("Kho – Dụng cụ", "Tồn kho theo tháng");
        synchronized (KhoDuLieuManHinh.class) {
            Map<String,String> row = new LinkedHashMap<>(lay(stock).get(index)); long current = docSo(row.get("Tồn cuối"));
            long after;
            try { after = switch (action) { case "Nhập kho" -> Math.addExact(current, number); case "Xuất HS", "Xuất CS" -> current - number; default -> number; }; }
            catch (ArithmeticException ex) { throw new IllegalArgumentException("Số lượng vượt giới hạn lưu trữ."); }
            if (after < 0) throw new IllegalArgumentException("Tồn kho không đủ. Tồn hiện tại: " + current);
            String column = action.equals("Nhập kho") ? "Nhập" : action;
            row.put(column, String.valueOf(action.equals("Kiểm kê") ? number - current : Math.addExact(docSo(row.get(column)), number)));
            row.put("Tồn cuối", String.valueOf(after)); KhoDuLieuManHinh.sua(stock, index, row);
            Map<String,String> movement = new LinkedHashMap<>(); movement.put("Ngày", LocalDate.now().format(DATE));
            movement.put("Mã SP", row.get("Mã SP")); movement.put("Tên hàng hóa", row.get("Tên hàng hóa"));
            movement.put("Loại", column); movement.put("Số lượng", String.valueOf(number)); movement.put("Cơ sở", coSo.layTen());
            movement.put("Tài khoản", "Admin (demo)"); movement.put("Ghi chú", note);
            KhoDuLieuManHinh.them(timTrang("Kho – Dụng cụ", "Lịch sử nhập xuất"), movement);
            return after;
        }
    }

    public void xetHocBu(String student, String course, String absence, String completed, String date, String reason) {
        if (coSo != CoSo.TRUNG_TAM) throw new IllegalArgumentException("Học bù theo khóa chỉ áp dụng cho Trung tâm.");
        if (!"Vắng có phép".equals(absence)) throw new IllegalArgumentException("Chỉ vắng CÓ PHÉP mới được học bù.");
        if (completed == null || !completed.matches("[0-2]"))
            throw new IllegalArgumentException("Tối đa 3 buổi học bù cho mỗi đăng ký khóa. Không còn lượt hợp lệ.");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Vui lòng nhập lý do.");
        ngayHopLe(date);
        Map<String,String> row = new LinkedHashMap<>(); row.put("Học viên", student); row.put("Đăng ký khóa", course);
        row.put("Buổi vắng", LocalDate.now().format(DATE)); row.put("Buổi bù", date); row.put("Đã bù/3", completed + "/3");
        row.put("Lý do", reason); row.put("Trạng thái", "Chờ xét");
        KhoDuLieuManHinh.them(timTrang("Học bù / Đổi buổi", "Xét học bù"), row);
    }

    public void chotThang() {
        if (coSo != CoSo.MAM_NON) throw new IllegalArgumentException("Chốt tháng mẫu áp dụng tại Mầm non.");
        Trang trang = timTrang("Học phí & Tiền ăn", "Chốt tháng");
        if (!lay(trang).isEmpty()) KhoDuLieuManHinh.sua(trang, 0, Map.of("Trạng thái", "Đã chốt"));
    }

    private static LocalDate ngayHopLe(String text) {
        try { return LocalDate.parse(text, DATE); }
        catch (DateTimeParseException | NullPointerException ex) { throw new IllegalArgumentException("Ngày phải hợp lệ, định dạng dd/MM/yyyy."); }
    }

    public static long docSo(String text) {
        if (text == null) return 0;
        try { return Long.parseLong(text.replaceAll("[^0-9-]", "")); }
        catch (NumberFormatException ex) { return 0; }
    }
}
