package vn.vinabacus.doituong.phanhe;

import vn.vinabacus.model.CoSo;
import java.util.*;

/** Đặc tả từng tab theo 49 ảnh UI; dữ liệu mẫu không đại diện dữ liệu SQL thật. */
public final class MoTaManHinh {
    private MoTaManHinh() {}
    public record Trang(CoSo coSo, String menu, String tab, String moTa, String[] cot, String[] truong, String[] thaoTac, String[][] mau) {
        public String khoa() {return coSo.name()+"/"+menu+"/"+tab;}
    }
    private static final Map<String,List<Trang>> CAC_TRANG = new LinkedHashMap<>();
    private static String[] tach(String s) {return s==null||s.isBlank()?new String[0]:s.split("\\|",-1);}
    private static String[][] dong(String... rows){String[][] a=new String[rows.length][];for(int i=0;i<rows.length;i++)a[i]=tach(rows[i]);return a;}
    private static void them(CoSo coSo, String menu, String tab,String moTa, String cot,String fields,String actions,String... rows) {
        Trang p=new Trang(coSo,menu,tab,moTa,tach(cot),tach(fields),tach(actions),dong(rows));
        CAC_TRANG.computeIfAbsent(coSo.name()+"/"+menu,k->new ArrayList<>()).add(p);
    }
    public static List<Trang> lay(CoSo coSo,String menu){return CAC_TRANG.getOrDefault(coSo.name()+"/"+menu,List.of());}
    public static Collection<Trang> tatCa(){List<Trang> a=new ArrayList<>();for(List<Trang> l:CAC_TRANG.values())a.addAll(l);return Collections.unmodifiableList(a);}
    public static int tongSoTab(){return tatCa().size();}
    static {
        for(CoSo c:CoSo.values()) {
            boolean mn=c==CoSo.MAM_NON;
            String cs=mn?"Mầm non":"Trung tâm";
            them(c,"Quản lý học viên","Danh sách học viên","Hồ sơ học viên theo cơ sở, tìm kiếm và lọc trạng thái.",
                "Mã học viên|Họ và tên|Ngày sinh|Người liên hệ|SĐT liên hệ|Lớp đang học|Trạng thái",
                "Mã học viên|Họ và tên*|Ngày sinh*|Giới tính#Nam,Nữ,Khác|Người liên hệ*|SĐT liên hệ*|Địa chỉ|Ngày nhập học|Trạng thái#Đang học,Tạm nghỉ,Nghỉ hẳn|Ghi chú",
                mn?"Thêm học viên|Sửa|Xem hồ sơ|Xếp lớp|Chuyển lớp":"Thêm học viên|Sửa|Xem hồ sơ|Đăng ký khóa|Xếp lớp|Chuyển lớp",
                "HV001|Nguyễn Minh Anh|12/03/2018|Nguyễn Thị Thu|0901234567|"+(mn?"Lá 1":"A1 - Cô Trân")+"|Đang học",
                "HV002|Trần Bảo Ngọc|25/07/2017|Trần Quốc Anh|0912345678|"+(mn?"Chồi 1":"Kid 1 - Cô Mai")+"|Đang học");
            them(c,"Quản lý học viên","Hồ sơ & liên hệ","Thông tin liên hệ lưu trong hồ sơ học viên, không có module phụ huynh riêng.",
                "Mã học viên|Họ tên|Ngày sinh|Giới tính|Người liên hệ|Điện thoại|Địa chỉ|Trạng thái",
                "Mã học viên|Họ tên*|Ngày sinh*|Giới tính#Nam,Nữ,Khác|Người liên hệ*|Điện thoại*|Địa chỉ|Trạng thái#Đang học,Tạm nghỉ,Nghỉ hẳn|Ghi chú",
                "Sửa thông tin|Xem hồ sơ", "HV001|Nguyễn Minh Anh|12/03/2018|Nữ|Nguyễn Thị Thu|0901234567|Cần Thơ|Đang học");
            if(!mn){
                them(c,"Quản lý học viên","Môn & Khóa","Mỗi học viên học nhiều môn, nhiều khóa; số buổi theo từng đăng ký khóa.",
                    "Mã đăng ký|Học viên|Môn học|Khóa học|Lớp đang học|Số buổi chuẩn|Đã học|Kết thúc dự kiến|Trạng thái",
                    "Mã đăng ký|Học viên*|Môn học#Tiếng Anh,Toán tư duy,Abacus|Khóa học*|Lớp đang học|Số buổi chuẩn|Đã học|Ngày bắt đầu|Kết thúc dự kiến|Trạng thái#Đang học,Hoàn thành,Chưa bắt đầu",
                    "Đăng ký khóa|Xếp lớp|Xem tiến độ",
                    "DK001|Nguyễn Minh Anh|Tiếng Anh|Tiếng Anh A1|A1 - Cô Trân|36|12|20/02/2027|Đang học",
                    "DK002|Nguyễn Minh Anh|Toán tư duy|Kid 1|Kid 1 - Cô Mai|32|6|28/02/2027|Đang học");
            }
            them(c,"Quản lý học viên","Lịch sử học tập","Tra cứu lớp từng học, lịch sử đăng ký, thông tin thay đổi cần thiết.",
                "Mã|Học viên|Nội dung|Lớp/Khóa|Ngày|Người thực hiện",
                "Mã|Học viên|Nội dung|Lớp/Khóa|Ngày|Người thực hiện|Lý do",
                "Xem lịch sử", "LS001|Nguyễn Minh Anh|Xếp lớp|"+(mn?"Lá 1":"Tiếng Anh A1")+"|01/11/2026|Admin");

            them(c,"Quản lý lớp học","Danh sách lớp","Một lớp thuộc một cơ sở; lớp Trung tâm thuộc một khóa học và có lịch cố định.",
                "Mã lớp|Tên lớp|Khóa học/Năm học|Giáo viên|Lịch cố định|Sĩ số|Trạng thái",
                "Mã lớp|Tên lớp*|Khóa học/Năm học*|Giáo viên*|Thứ học trong tuần|Giờ bắt đầu|Giờ kết thúc|Sĩ số tối đa|Ngày bắt đầu|Trạng thái#Đang hoạt động,Đã đóng|Ghi chú",
                "Tạo lớp|Sửa lớp|Xem lớp|Xếp học viên|Phân công giáo viên|Chuyển lớp",
                mn?"LM001|Lá 1|2026–2027|Trần Thị Mai|T2–T7|28|Đang hoạt động":"LT001|Tiếng Anh A1 - Cô Trân|Tiếng Anh A1|Cô Trân|T2–T4–T6|15|Đang hoạt động",
                mn?"LM002|Chồi 1|2026–2027|Lê Thị Hạnh|T2–T7|26|Đang hoạt động":"LT002|Tiếng Anh A1 - Cô Mai|Tiếng Anh A1|Cô Mai|T3–T5–T7|16|Đang hoạt động");
            them(c,"Quản lý lớp học","Xếp lớp & chuyển lớp","Lưu thời điểm chuyển và lớp cũ/mới; Trung tâm tính lại lịch và giá trị còn lại.",
                "Mã|Học viên|Lớp cũ|Lớp mới|Ngày hiệu lực|Lý do|Trạng thái",
                "Học viên*|Lớp cũ|Lớp mới*|Ngày hiệu lực*|Lý do*|Số buổi còn lại|Học phí thực tế",
                "Xếp lớp|Chuyển lớp|Xem lịch sử", "XL001|Nguyễn Minh Anh|—|"+(mn?"Lá 1":"Tiếng Anh A1 - Cô Trân")+"|10/11/2026|Nhập học|Hiệu lực");
            them(c,"Quản lý lớp học","Phân công giáo viên","Giáo viên chỉ được xem lớp/buổi thuộc phạm vi phân công.",
                "Mã phân công|Lớp|Giáo viên|Thứ học|Thời gian|Hiệu lực",
                "Lớp học*|Giáo viên*|Thứ học|Giờ bắt đầu|Giờ kết thúc|Ngày hiệu lực|Ghi chú",
                "Phân công giáo viên|Điều chỉnh", "PC001|"+(mn?"Lá 1":"Tiếng Anh A1")+"|Trần Thị Mai|Thứ 2,4,6|17:00–18:30|Đang áp dụng");

            them(c,"Sắp lịch học","Lịch tuần","Có thể có nhiều lớp chung giờ nếu giáo viên và học viên không bị trùng bất hợp lệ.",
                "Thứ|Khung giờ|Lớp đồng thời|Giáo viên|Nội dung|Trạng thái",
                "Thứ#Thứ 2,Thứ 3,Thứ 4,Thứ 5,Thứ 6,Thứ 7|Giờ bắt đầu*|Giờ kết thúc*|Lớp đồng thời*|Giáo viên|Nội dung|Trạng thái#Đã xếp,Dự kiến,Nghỉ",
                "Tạo lịch|Đổi lịch lớp|Xem lớp trong giờ",
                mn?"Thứ 2|07:00–08:00|Đón học sinh|Giáo viên trực|Sinh hoạt|Đã xếp":"Thứ 2|08:00–09:30|Tiếng Anh A1 - Cô Trân; Toán tư duy Kid 1|Cô Trân; Cô Mai|Học theo buổi|Đã xếp",
                mn?"Thứ 2|09:00–10:00|Hoạt động học|Giáo viên lớp|Sinh hoạt|Đã xếp":"Thứ 3|16:00–17:30|Tiếng Anh A2 - Thầy Nam; Abacus - Cô Mai|Thầy Nam; Cô Mai|Học theo buổi|Đã xếp");
            if(mn){
                them(c,"Sắp lịch học","Ngày nghỉ chung","Ngày nghỉ loại khỏi số ngày học chuẩn tháng; chỉ Admin/Tổ trưởng chỉnh.",
                    "Ngày nghỉ|Lý do|Số ngày chuẩn trước|Số ngày chuẩn sau|Người cập nhật",
                    "Ngày nghỉ*|Lý do*|Số ngày chuẩn trước|Số ngày chuẩn sau|Ghi chú",
                    "Chọn ngày nghỉ|Điều chỉnh ngày chuẩn","20/11/2026|Ngày nghỉ chung cơ sở|26|25|Admin");
            }else{
                them(c,"Sắp lịch học","Buổi học","Mỗi buổi học xác định lớp, ngày, giáo viên và danh sách điểm danh riêng.",
                    "Mã buổi|Ngày|Giờ học|Lớp|Khóa|Giáo viên|Số học viên|Trạng thái",
                    "Mã buổi|Ngày*|Giờ bắt đầu*|Giờ kết thúc*|Lớp*|Khóa|Giáo viên*|Số học viên|Trạng thái#Dự kiến,Đã học,Nghỉ|Ghi chú",
                    "Thêm buổi|Xem buổi|Đổi buổi", "B001|10/11/2026|08:00–09:30|Tiếng Anh A1 - Cô Trân|Tiếng Anh A1|Cô Trân|15|Dự kiến");
                them(c,"Sắp lịch học","Đổi buổi học viên","Đổi buổi trong tuần là lịch thay thế, không tính là buổi học bù.",
                    "Mã đổi|Học viên|Khóa|Buổi cũ|Buổi mới|Lớp thay thế|Trạng thái",
                    "Học viên*|Khóa*|Buổi cũ*|Buổi mới*|Lớp thay thế*|Lý do*|Trạng thái#Chờ xếp,Đã đổi,Hủy",
                    "Đổi buổi|Xem lịch", "DB001|Nguyễn Minh Anh|Tiếng Anh A1|12/11/2026|13/11/2026|Tiếng Anh A1 - Cô Mai|Đã đổi");
            }
            them(c,"Sắp lịch học","Đổi lịch lớp","Đổi lịch của lớp sang ngày khác, tính lại dự kiến kết thúc Trung tâm.",
                "Mã|Lớp|Lịch cũ|Lịch mới|Ngày áp dụng|Lý do|Trạng thái",
                "Lớp*|Lịch cũ|Lịch mới*|Ngày áp dụng*|Lý do*|Ghi chú",
                "Đổi lịch lớp|Xem lịch hiện tại", "DL001|"+(mn?"Lá 1":"Tiếng Anh A1 - Cô Trân")+"|T2,4,6|T2,5,6|12/11/2026|Điều chỉnh lịch|Hiệu lực");

            them(c,"Điểm danh","Bảng điểm danh","Chọn ngày và lớp "+(mn?"để điểm danh một lần mỗi trẻ":"và buổi để điểm danh mỗi học viên theo buổi thực tế")+".",
                "Ngày|Mã học viên|Học viên|Lớp|Buổi|Trạng thái|Ghi chú",
                "Ngày*|Mã học viên*|Học viên*|Lớp*|Buổi|Trạng thái#Có mặt,Vắng có phép,Vắng không phép,Chưa điểm danh|Ghi chú",
                "Mở bảng điểm danh|Xem chi tiết|Xuất CSV",
                "10/11/2026|HV001|Nguyễn Minh Anh|"+(mn?"Lá 1":"Tiếng Anh A1 - Cô Trân")+"|"+(mn?"Cả ngày":"08:00–09:30")+"|Có mặt|Đã ghi nhận",
                "10/11/2026|HV002|Trần Bảo Ngọc|"+(mn?"Lá 1":"Tiếng Anh A1 - Cô Trân")+"|"+(mn?"Cả ngày":"08:00–09:30")+"|Vắng có phép|Có báo trước");
            them(c,"Điểm danh","Lịch sử điều chỉnh","Theo dõi trước/sau, lý do, tài khoản, thời điểm. Sau chốt Mầm non chỉ Admin sửa.",
                "Mã sửa|Ngày điểm danh|Học viên|Lớp/Buổi|Trước|Sau|Lý do|Người sửa|Thời điểm",
                "Ngày điểm danh*|Học viên*|Lớp/Buổi*|Trước#Có mặt,Vắng có phép,Vắng không phép,Chưa điểm danh|Sau#Có mặt,Vắng có phép,Vắng không phép,Chưa điểm danh|Lý do*|Người sửa|Thời điểm",
                "Xem lịch sử", "SD001|03/11/2026|Nguyễn Minh Anh|"+(mn?"Lá 1":"Tiếng Anh A1")+"|Vắng không phép|Vắng có phép|Gia đình bổ sung đơn|Admin|04/11/2026 09:15");

            them(c,"Tuyển sinh","Danh sách khách tiềm năng","Học thử chỉ theo dõi trạng thái, thực hiện bên ngoài; không điểm danh học thử.",
                "Mã tư vấn|Họ tên|Số điện thoại|Chương trình quan tâm|Trạng thái|Ngày tiếp nhận|Nhân viên",
                "Mã tư vấn|Họ tên*|Số điện thoại*|Chương trình quan tâm*|Trạng thái#Tiếp nhận,Học thử,Chờ xác nhận,Xếp lớp,Chính thức|Ngày tiếp nhận|Nhân viên|Ghi chú",
                "Thêm khách tiềm năng|Cập nhật trạng thái|Chuyển thành học viên|Xem chi tiết",
                "TV001|Nguyễn Anh Khoa|0901234000|"+(mn?"Mầm non":"Tiếng Anh")+"|Học thử|09/11/2026|Tư vấn A",
                "TV002|Lê Minh Phương|0912345000|"+(mn?"Mầm non":"Toán tư duy")+"|Chờ xác nhận|10/11/2026|Tư vấn B");
            them(c,"Tuyển sinh","Quy trình tuyển sinh","Tiếp nhận → Học thử → Chờ xác nhận → Xếp lớp → Chính thức → Xếp lịch.",
                "Khách tiềm năng|Bước đang xử lý|Bước kế tiếp|Người phụ trách|Ghi chú",
                "Khách tiềm năng*|Bước đang xử lý#Tiếp nhận,Học thử,Chờ xác nhận,Xếp lớp,Chính thức,Xếp lịch|Bước kế tiếp|Người phụ trách|Ghi chú",
                "Chuyển bước|Xếp lớp", "Nguyễn Anh Khoa|Học thử|Chờ xác nhận|Tư vấn A|Học thử ngoài hệ thống");

            String stockColumns="STT|Mã SP|Tên hàng hóa|ĐVT|Đơn giá|Tồn đầu|Nhập|Xuất HS|Xuất CS|Kiểm kê|Tồn cuối|Ghi chú";
            them(c,"Kho – Dụng cụ","Tồn kho theo tháng","Đúng cấu trúc Excel tồn kho; Xuất CS là xuất dùng tại cơ sở, không chuyển cơ sở.",
                stockColumns,"Mã SP|Tên hàng hóa*|ĐVT#Cái,Bộ,Quyển,Cây,Hộp|Đơn giá|Tồn đầu|Nhập|Xuất HS|Xuất CS|Kiểm kê|Ghi chú",
                "Thêm hàng hóa|Sửa hàng hóa|Nhập kho|Xuất HS|Xuất CS|Kiểm kê|Xuất CSV",
                "1|HH001|Vở học sinh|Quyển|12000|100|40|20|5|0|115|Dùng cho học viên",
                "2|HH002|Bút chì|Cây|5000|200|30|20|10|0|200|Còn tồn");
            them(c,"Kho – Dụng cụ","Lịch sử nhập xuất","Theo dõi thao tác, loại xuất, tài khoản thực hiện và cơ sở.",
                "Mã phiếu|Ngày|Mã SP|Tên hàng hóa|Loại|Số lượng|Cơ sở|Tài khoản|Ghi chú",
                "Mã phiếu|Ngày*|Mã SP*|Tên hàng hóa*|Loại#Nhập,Xuất HS,Xuất CS,Kiểm kê|Số lượng*|Cơ sở|Tài khoản|Ghi chú",
                "Xem phiếu|Xuất CSV", "PK001|10/11/2026|HH001|Vở học sinh|Nhập|40|"+cs+"|Admin|Nhập kho");
            them(c,"Kho – Dụng cụ","Kiểm kê","Kiểm kê theo tháng; không theo dõi giá vốn, nhà cung cấp hay phát quà theo trẻ.",
                "Tháng|Mã SP|Tên hàng hóa|Tồn sổ|Tồn thực tế|Chênh lệch|Người kiểm kê|Ghi chú",
                "Tháng*|Mã SP*|Tên hàng hóa|Tồn sổ|Tồn thực tế*|Chênh lệch|Người kiểm kê|Ghi chú",
                "Kiểm kê|Xem chi tiết", "11/2026|HH001|Vở học sinh|115|115|0|Tổ trưởng|Khớp sổ");

            them(c,"Báo cáo – Thống kê","Học viên & Lớp","Báo cáo chỉ theo cơ sở đang chọn, lọc kỳ và phân quyền.",
                "Kỳ báo cáo|Loại báo cáo|Tổng học viên|Tổng lớp|Đang học|Ghi chú",
                "Kỳ báo cáo*|Loại báo cáo#Học viên,Lớp,Khóa học|Tổng học viên|Tổng lớp|Đang học|Ghi chú",
                "Lọc báo cáo|Xem chi tiết|Xuất CSV", "11/2026|Tổng hợp lớp học|"+(mn?"128":"256")+"|"+(mn?"6":"18")+"|Đang hoạt động|Theo cơ sở");
            them(c,"Báo cáo – Thống kê","Điểm danh","Báo cáo theo ngày, lớp/buổi, phân biệt trạng thái.",
                "Ngày|Lớp/Buổi|Có mặt|Vắng có phép|Vắng không phép|Chưa điểm danh|Tỷ lệ có mặt",
                "Ngày*|Lớp/Buổi*|Có mặt|Vắng có phép|Vắng không phép|Chưa điểm danh|Tỷ lệ có mặt",
                "Lọc báo cáo|Xuất CSV", "10/11/2026|"+(mn?"Lá 1":"Tiếng Anh A1")+"|25|2|1|0|89,3%");
            them(c,"Báo cáo – Thống kê","Học phí & Công nợ","Tổng hợp phải thu, đã thu, còn nợ; tách học phí và tiền ăn nếu Mầm non.",
                "Kỳ thu|Học viên|Nội dung|Phải thu|Đã thu|Còn nợ|Trạng thái",
                "Kỳ thu*|Học viên|Nội dung|Phải thu|Đã thu|Còn nợ|Trạng thái#Đủ,Thiếu",
                "Lọc báo cáo|Xuất CSV", "11/2026|Nguyễn Minh Anh|"+(mn?"Học phí tháng":"Tiếng Anh A1")+"|3290000|2000000|1290000|Thiếu");
            them(c,"Báo cáo – Thống kê","Nhập xuất tồn","Báo cáo kho theo tháng và cơ sở.",
                "Tháng|Mã SP|Tên hàng hóa|Tồn đầu|Nhập|Xuất HS|Xuất CS|Tồn cuối",
                "Tháng*|Mã SP|Tên hàng hóa|Tồn đầu|Nhập|Xuất HS|Xuất CS|Tồn cuối",
                "Lọc báo cáo|Xuất CSV", "11/2026|HH001|Vở học sinh|100|40|20|5|115");

            them(c,"Hệ thống","Nhân viên","Ba vai trò duy nhất: Admin, Tổ trưởng, Giáo viên.",
                "Mã NV|Họ tên|Vai trò|Cơ sở được cấp|Số điện thoại|Trạng thái",
                "Mã NV|Họ tên*|Vai trò#Admin,Tổ trưởng,Giáo viên|Cơ sở được cấp#Mầm non,Trung tâm,Cả hai|Số điện thoại|Trạng thái#Hoạt động,Ngừng hoạt động",
                "Thêm nhân viên|Sửa nhân viên|Xem chi tiết", "NV001|Trần Thị Mai|Giáo viên|"+cs+"|0901234567|Hoạt động",
                "NV002|Nguyễn Văn A|Admin|Cả hai|0900000000|Hoạt động");
            them(c,"Hệ thống","Tài khoản & Phân quyền","Tài khoản Admin cấp quyền theo cơ sở, không tự đăng ký.",
                "Tên tài khoản|Nhân viên|Vai trò|Cơ sở được cấp|Trạng thái|Ghi chú",
                "Tên tài khoản*|Nhân viên*|Email|Vai trò#Admin,Tổ trưởng,Giáo viên|Cơ sở được cấp#Mầm non,Trung tâm,Cả hai|Trạng thái#Hoạt động,Khóa|Ghi chú",
                "Cấp quyền|Sửa quyền|Xem chi tiết", "admin_demo|Nguyễn Văn A|Admin|Cả hai|Hoạt động|Chưa đăng nhập thật");
            them(c,"Hệ thống","Cài đặt nhận diện","Chỉ Admin đổi tên ứng dụng, logo, tên/logo cơ sở; không lưu lịch sử đổi.",
                "Tên ứng dụng|Tên cơ sở|Logo ứng dụng|Logo cơ sở|Lần cập nhật",
                "Tên ứng dụng*|Tên cơ sở*|Logo ứng dụng|Logo cơ sở|Ghi chú",
                "Cài đặt nhận diện|Xem chi tiết", "VINABACUS|"+cs+"|vinabacus.png|"+(mn?"mamnon.png":"trungtam.png")+"|10/11/2026");
        }
        CoSo t=CoSo.TRUNG_TAM;
        them(t,"Quản lý khóa học","Sơ đồ môn – khóa – lớp", "Xem trực quan cấu trúc môn học, khóa học và các lớp cùng khóa.",
            "Môn học|Khóa học|Lớp học", "Môn học|Khóa học|Lớp học", "Xem môn|Xem khóa|Xem lớp",
            "Tiếng Anh|Tiếng Anh A1|A1 - Cô Trân");
        them(t,"Quản lý khóa học","Môn học","Một môn có nhiều khóa: Tiếng Anh → Tiếng Anh A1, A2, B1, Cambridge...",
            "Mã môn|Tên môn học|Số khóa|Trạng thái|Ghi chú",
            "Mã môn|Tên môn học*|Mô tả|Trạng thái#Đang hoạt động,Ngừng hoạt động|Ghi chú",
            "Thêm môn học|Sửa môn học|Xem các khóa", "MH001|Tiếng Anh|5|Đang hoạt động|Khóa A1/A2/Starters",
            "MH002|Toán tư duy|12|Đang hoạt động|Kid, Basic, Elementary");
        them(t,"Quản lý khóa học","Khóa học","Tổng số buổi và học phí mặc định thuộc khóa; mỗi khóa mở nhiều lớp.",
            "Mã khóa|Môn học|Tên khóa|Tổng số buổi|Học phí mặc định|Thời lượng (tuần)|Trạng thái",
            "Mã khóa|Môn học#Tiếng Anh,Toán tư duy,Abacus|Tên khóa*|Tổng số buổi*|Học phí mặc định*|Thời lượng (tuần)|Trạng thái#Đang mở,Tạm ngưng,Đã đóng|Mô tả",
            "Tạo khóa học|Sửa khóa học|Đăng ký khóa|Xem lớp của khóa",
            "KH001|Tiếng Anh|Tiếng Anh A1|36|3190000|16|Đang mở",
            "KH002|Tiếng Anh|Tiếng Anh A2|36|3290000|16|Đang mở",
            "KH003|Toán tư duy|Kid 1|32|3290000|16|Đang mở");
        them(t,"Quản lý khóa học","Đăng ký khóa","Học phí thực tế có thể điều chỉnh, một ưu đãi cao nhất tại lúc đăng ký.",
            "Mã ĐK|Học viên|Môn học|Khóa học|Lớp|Số buổi|Học phí thực tế|Giảm giá|Sau giảm|Trạng thái",
            "Mã ĐK|Học viên*|Môn học#Tiếng Anh,Toán tư duy,Abacus|Khóa học*|Lớp|Số buổi chuẩn|Học phí thực tế*|Chương trình giảm giá|Số tiền giảm|Ngày bắt đầu|Trạng thái#Đang học,Chưa bắt đầu,Hoàn thành",
            "Đăng ký khóa|Xem chi tiết|Sửa học phí","DK001|Nguyễn Minh Anh|Tiếng Anh|Tiếng Anh A1|A1 - Cô Trân|36|3190000|0|3190000|Đang học");
        them(t,"Quản lý khóa học","Gói học 1 năm","Nhiều đăng ký khóa cùng gói; giảm giá một lần, phân bổ học phí từng khóa.",
            "Mã gói|Học viên|Các khóa trong lộ trình|Ngày bắt đầu|Ngày kết thúc|Học phí gốc|Giảm giá|Phải thu|Trạng thái",
            "Mã gói|Học viên*|Các khóa trong lộ trình*|Ngày bắt đầu*|Ngày kết thúc*|Học phí gốc*|Chương trình ưu đãi|Giảm giá|Phải thu|Trạng thái#Đăng ký,Đang học,Hoàn thành|Ghi chú",
            "Đăng ký gói 1 năm|Xem gói|Sửa đăng ký gói", "GN001|Nguyễn Minh Anh|Kid 1, Kid 2, Basic A|10/11/2026|10/11/2027|10570000|1057000|9513000|Đang học");
        them(t,"Quản lý khóa học","Chương trình giảm giá","Giảm theo tiền/%; thời hạn hiệu lực; chỉ một chương trình có giá trị giảm cao nhất.",
            "Mã ưu đãi|Tên chương trình|Phạm vi|Hình thức|Giá trị|Ngày bắt đầu|Ngày kết thúc|Trạng thái",
            "Mã ưu đãi|Tên chương trình*|Phạm vi#Khóa học,Gói 1 năm,Tất cả khóa|Hình thức#Phần trăm,Số tiền|Giá trị*|Ngày bắt đầu*|Ngày kết thúc*|Trạng thái#Đang áp dụng,Chưa hiệu lực,Hết hạn|Ghi chú",
            "Thêm chương trình giảm giá|Sửa ưu đãi|Xem chi tiết", "GG001|Ưu đãi đăng ký năm|Gói 1 năm|Phần trăm|10|01/11/2026|31/12/2026|Đang áp dụng",
            "GG002|Giảm 500.000đ|Tất cả khóa|Số tiền|500000|01/11/2026|30/11/2026|Đang áp dụng");
        them(t,"Học bù / Đổi buổi","Xét học bù","Chỉ vắng có phép, tối đa 3 buổi hoàn thành / đăng ký khóa.",
            "Mã yêu cầu|Học viên|Đăng ký khóa|Buổi vắng|Buổi bù|Đã bù/3|Lý do|Trạng thái",
            "Mã yêu cầu|Học viên*|Đăng ký khóa*|Buổi vắng*|Trạng thái vắng#Vắng có phép,Vắng không phép|Buổi bù*|Số buổi đã bù|Lý do|Trạng thái#Chờ xét,Đã duyệt,Đã bù,Từ chối",
            "Xét học bù|Duyệt học bù|Xem số buổi", "HB001|Nguyễn Minh Anh|Tiếng Anh A1|03/11/2026|12/11/2026|1/3|Nghỉ có phép|Đã duyệt");
        them(t,"Học bù / Đổi buổi","Đổi buổi","Học viên đổi ngày trong tuần; đổi buổi không tiêu hao 3 buổi học bù.",
            "Mã yêu cầu|Học viên|Đăng ký khóa|Buổi gốc|Buổi thay thế|Lớp nhận|Trạng thái",
            "Mã yêu cầu|Học viên*|Đăng ký khóa*|Buổi gốc*|Buổi thay thế*|Lớp nhận*|Lý do*|Trạng thái#Đã đổi,Chờ xếp,Hủy",
            "Đổi buổi|Xem lịch tuần", "DB001|Nguyễn Minh Anh|Tiếng Anh A1|12/11/2026|13/11/2026|A1 - Cô Mai|Đã đổi");
        them(t,"Học bù / Đổi buổi","Buổi bổ sung","Học viên vào lớp giữa chừng được học đủ số buổi chuẩn; không tính vào học bù.",
            "Mã|Học viên|Đăng ký khóa|Buổi bổ sung|Lớp|Số buổi còn lại|Trạng thái",
            "Học viên*|Đăng ký khóa*|Buổi bổ sung*|Lớp*|Số buổi còn lại|Lý do|Trạng thái#Chưa học,Đã học",
            "Xếp buổi bổ sung|Xem tiến độ", "BS001|Trần Bảo Ngọc|Kid 1|14/11/2026|Kid 1 - Cô Mai|26|Chưa học");
        them(t,"Học bù / Đổi buổi","Chuyển lớp","Chuyển lớp giữa khóa bảo lưu giá trị chênh lệch, không hoàn tiền mặt.",
            "Mã|Học viên|Lớp cũ|Lớp mới|Buổi còn|Giá trị còn|Tín dụng|Ngày hiệu lực",
            "Học viên*|Lớp cũ*|Lớp mới*|Số buổi chưa học|Học phí lớp cũ|Học phí lớp mới|Tín dụng|Ngày hiệu lực*|Lý do*",
            "Chuyển lớp|Xem chi tiết", "CL001|Nguyễn Minh Anh|A1 - Cô Trân|A1 - Cô Mai|22|1950000|150000|12/11/2026");
        them(t,"Học phí","Khoản phải thu","Công nợ = phải thu − đã đóng. Khóa cũ còn nợ vẫn học khóa mới.",
            "Mã khoản|Học viên|Đăng ký khóa|Phải thu|Đã đóng|Còn nợ|Trạng thái",
            "Mã khoản|Học viên*|Đăng ký khóa*|Phải thu*|Đã đóng|Còn nợ|Trạng thái#Đủ,Thiếu|Ghi chú",
            "Lập phiếu thu|Xem công nợ|Xem chi tiết", "CN001|Nguyễn Minh Anh|Tiếng Anh A1|3190000|1000000|2190000|Thiếu");
        them(t,"Học phí","Phiếu thu","Cho phép thu nhiều khóa trong một phiếu; phân bổ đúng từng khóa.",
            "Số phiếu|Ngày thu|Học viên|Các khoản phân bổ|Số tiền thực thu|Hình thức|Người thu|Trạng thái",
            "Số phiếu|Ngày thu*|Học viên*|Các khoản phân bổ*|Số tiền thực thu*|Hình thức#Tiền mặt,Chuyển khoản|Người thu|Trạng thái#Đã thu,Đã hủy|Ghi chú",
            "Lập phiếu thu|Xem phiếu|Xuất CSV","PT001|10/11/2026|Nguyễn Minh Anh|Tiếng Anh A1, Toán tư duy Kid 1|4000000|Chuyển khoản|Admin|Đã thu");
        them(t,"Học phí","Tín dụng học phí","Chỉ tiền thực thu dư mới sinh tín dụng; không lấy giảm giá hay chênh lệch ảo tạo tín dụng.",
            "Mã|Học viên|Nguồn tín dụng|Số phát sinh|Đã sử dụng|Còn lại|Ngày cập nhật",
            "Mã|Học viên*|Nguồn tín dụng*|Số phát sinh*|Đã sử dụng|Còn lại|Ngày cập nhật|Ghi chú",
            "Xem tín dụng|Sử dụng tín dụng", "TD001|Nguyễn Minh Anh|Thu dư phiếu PT001|200000|0|200000|10/11/2026");
        CoSo m=CoSo.MAM_NON;
        them(m,"Học phí & Tiền ăn","Bảng tính phí tháng","Học phí theo lớp, nhập giữa tháng tính tỷ lệ; nghỉ thường không đổi học phí.",
            "Tháng|Học viên|Lớp|Ngày chuẩn|Học phí|Tiền ăn|Khấu trừ|Phải thu|Đã thu|Còn nợ",
            "Tháng*|Học viên*|Lớp*|Ngày học chuẩn|Học phí tháng*|Đơn giá ăn/ngày*|Số ngày ăn|Khấu trừ tiền ăn|Phải thu|Đã thu|Còn nợ",
            "Tính phí tháng|Học phí riêng|Lập phiếu thu|Chốt tháng",
            "11/2026|Nguyễn Minh Anh|Lá 1|25|3300000|875000|35000|4140000|2500000|1640000");
        them(m,"Học phí & Tiền ăn","Học phí riêng","Chỉ điều chỉnh phí tháng chưa chốt; lưu lý do, người thực hiện và lịch sử.",
            "Học viên|Lớp|Tháng hiệu lực|Mức mặc định|Mức riêng|Lý do|Người sửa",
            "Học viên*|Lớp*|Tháng hiệu lực*|Mức mặc định|Mức riêng*|Lý do*|Người sửa",
            "Đặt học phí riêng|Xem lịch sử", "Nguyễn Minh Anh|Lá 1|11/2026|3300000|3100000|Ưu đãi riêng|Admin");
        them(m,"Học phí & Tiền ăn","Khấu trừ & bảo lưu tiền ăn","Vắng có phép trừ tiền ăn kỳ sau, dư tiếp tục bảo lưu, không tạo khoản âm.",
            "Mã|Học viên|Tháng gốc|Vắng có phép|Tiền được trừ|Đã sử dụng|Còn bảo lưu|Tháng nhận",
            "Học viên*|Tháng gốc*|Số ngày vắng có phép*|Đơn giá ăn/ngày|Tiền được trừ|Đã sử dụng|Còn bảo lưu|Tháng nhận",
            "Điều chỉnh tiền ăn|Bảo lưu tiền ăn|Hoàn tiền ăn", "TA001|Nguyễn Minh Anh|10/2026|3|105000|70000|35000|11/2026");
        them(m,"Học phí & Tiền ăn","Phiếu thu","Thu học phí tháng và tiền ăn riêng; chỉnh sửa sau chốt không đổi chứng từ cũ.",
            "Số phiếu|Ngày thu|Học viên|Học phí tháng|Tiền ăn|Khấu trừ|Thực thu|Hình thức",
            "Số phiếu|Ngày thu*|Học viên*|Tháng|Học phí tháng|Tiền ăn|Khấu trừ|Thực thu*|Hình thức#Tiền mặt,Chuyển khoản|Ghi chú",
            "Lập phiếu thu|Xem phiếu", "PTMN001|10/11/2026|Nguyễn Minh Anh|3300000|875000|35000|4140000|Chuyển khoản");
        them(m,"Học phí & Tiền ăn","Chốt tháng","Chốt không cho sửa hồi tố; sau chốt chỉ Admin được sửa điểm danh có nhật ký.",
            "Tháng|Ngày học chuẩn|Số lớp|Số học viên|Tổng phải thu|Đã thu|Trạng thái|Người chốt",
            "Tháng*|Ngày học chuẩn|Số lớp|Số học viên|Tổng phải thu|Đã thu|Trạng thái#Chưa chốt,Đã chốt|Người chốt|Lý do",
            "Chốt tháng|Xem lịch sử", "11/2026|25|6|128|512000000|320000000|Chưa chốt|—");
    }
}
