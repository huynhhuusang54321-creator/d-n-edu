package vn.vinabacus.controller;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhachTiemNang;
import vn.vinabacus.service.TuyenSinhService;

/**
 * Điều phối toàn bộ nghiệp vụ màn Tuyển sinh.
 * Tải dữ liệu nền, lọc trong bộ nhớ, kiểm tra trạng thái, thêm/sửa và chuyển đổi hồ sơ.
 */
public final class TuyenSinhController {

    public static final String[] MA_TAB = {
        "TIEP_NHAN", "HOC_THU", "CHO_XAC_NHAN",
        "CHO_XEP_LOP", "DA_CHUYEN_DOI", "KHONG_DANG_KY"
    };

    public static final String[] TEN_TAB = {
        "Tiếp nhận", "Học thử", "Chờ xác nhận",
        "Chờ xếp lớp", "Đã chuyển đổi", "Không đăng ký"
    };

    public static final String[] MO_TA_TAB = {
        "Hồ sơ mới tiếp nhận. Có thể sửa thông tin hoặc chuyển sang trạng thái tiếp theo.",
        "Theo dõi hồ sơ học thử; tích 'Đã học thử' khi cập nhật thông tin phù hợp.",
        "Hồ sơ đang chờ xác nhận đăng ký.",
        "Hồ sơ đang chờ xếp lớp. Khi đủ điều kiện, dùng 'Chuyển thành học viên'.",
        "Hồ sơ đã được chuyển thành học viên. Cập nhật thông tin ở màn hình Học viên.",
        "Hồ sơ không đăng ký, vẫn được lưu để tra cứu lịch sử."
    };

    public static final String[] TRANG_THAI_SUA = {
        "TIEP_NHAN", "HOC_THU", "CHO_XAC_NHAN",
        "CHO_XEP_LOP", "KHONG_DANG_KY"
    };

    private static final Set<String> SET_TRANG_THAI_SUA = Set.of(TRANG_THAI_SUA);

    /** 0: tất cả, 1: đã học thử, 2: chưa học thử. */
    public static final String[] LOC_HOC_THU = {
        "Tất cả", "Đã học thử", "Chưa học thử"
    };

    private static final int[] TAB_CAN_XU_LY = {0, 1, 2, 3};
    private static final String CHUA_CHON = "Hãy chọn một hồ sơ trong tab hiện tại";

    private record MucKhach(KhachTiemNang khach, String tenKhongDau, String sdt) {
        boolean khop(String raw, String khongDau) {
            if (raw.isEmpty()) return true;
            if (String.valueOf(khach.maKhach()).contains(raw)) return true;
            if (!khongDau.isEmpty() && tenKhongDau.contains(khongDau)) return true;
            return !sdt.isEmpty() && sdt.contains(raw);
        }
    }

    public record DuLieuForm(
            String hoTen,
            String soDienThoai,
            String tenNguoiLienHe,
            String nhuCau,
            String trangThai,
            boolean daHocThu,
            String ghiChu) {
    }

    public interface View {
        void hienDanhSach(Map<String, List<KhachTiemNang>> theoTab, int canXuLy);
        void chonTab(String maTab);
        void chonDongTheoMa(int maKhach);
        void capNhatNut(boolean suaDuoc, boolean chuyenDuoc);
        void capNhatMenu(boolean suaDuoc, boolean chuyenDuoc, boolean copyDuoc);
        void hienThongBao(String noiDung);
        void hienThanhCong(String noiDung);
        void hienCanhBao(String noiDung);
        void hienLoi(String noiDung);
        boolean xacNhan(String noiDung);
        void moForm(KhachTiemNang cu);
        void hienHoSo(KhachTiemNang k);
        void datLaiBoLoc();
        void saoChepVaoClipboard(String noiDung);
        <T> void chayNen(String dangLam, Callable<T> viec, Consumer<T> khiXong, Consumer<Throwable> khiLoi);
    }

    private final CoSo coSo;
    private final View view;
    private final TuyenSinhService service = new TuyenSinhService();

    private List<MucKhach> goc = List.of();
    private String tuKhoa = "";
    private int locHocThu = 0;
    private long phien = 0;

    private int maCanChon = -1;
    private String tabCanChon;

    public TuyenSinhController(CoSo coSo, View view) {
        if (coSo == null) {
            throw new IllegalArgumentException("Cơ sở không được để trống");
        }
        if (view == null) {
            throw new IllegalArgumentException("View không được để trống");
        }
        this.coSo = coSo;
        this.view = view;
    }

    /** Tải toàn bộ hồ sơ của cơ sở; lamMoi=true buộc xóa cache trước khi đọc. */
    public void taiDanhSach(boolean lamMoi) {
        final long phienTai = ++phien;
        nen(lamMoi ? "Đang làm mới dữ liệu tuyển sinh..." : "Đang tải hồ sơ tuyển sinh...",
                () -> {
                    if (lamMoi) {
                        service.lamMoi(coSo);
                    }
                    List<KhachTiemNang> ds = service.timKiem(coSo, "", "TAT_CA");
                    if (ds == null || ds.isEmpty()) {
                        return List.<MucKhach>of();
                    }
                    List<MucKhach> out = new ArrayList<>(ds.size());
                    for (KhachTiemNang k : ds) {
                        out.add(new MucKhach(k, chuanHoa(k.hoTen()), k.soDienThoai() == null ? "" : k.soDienThoai().trim()));
                    }
                    return List.copyOf(out);
                },
                ds -> {
                    if (phienTai != phien) return;
                    goc = ds;
                    apDungLocNoiBo(true);
                },
                ex -> {
                    if (phienTai == phien) baoLoi(ex);
                });
    }

    /** Tìm kiếm trong bộ nhớ theo mã khách, họ tên không dấu hoặc SĐT. */
    public void tim(String nhap) {
        tuKhoa = nhap == null ? "" : nhap.trim();
        apDungLocNoiBo(true);
    }

    /** Lọc theo trạng thái học thử trong bộ nhớ. */
    public void apDungLocHocThu(int chiSo) {
        locHocThu = (chiSo < 0 || chiSo >= LOC_HOC_THU.length) ? 0 : chiSo;
        apDungLocNoiBo(false);
    }

    /** Xóa bộ lọc hiện tại mà không gọi lại CSDL. */
    public void xoaLoc() {
        tuKhoa = "";
        locHocThu = 0;
        view.datLaiBoLoc();
        apDungLocNoiBo(true);
    }

    /** Đặt lại bộ lọc và làm mới dữ liệu từ CSDL. */
    public void taiLai() {
        tuKhoa = "";
        locHocThu = 0;
        view.datLaiBoLoc();
        taiDanhSach(true);
    }

    private void apDungLocNoiBo(boolean capNhatThongBao) {
        Map<String, List<KhachTiemNang>> theoTab = new LinkedHashMap<>();
        for (String maTab : MA_TAB) {
            theoTab.put(maTab, new ArrayList<>());
        }

        String raw = tuKhoa;
        String khongDau = chuanHoa(raw);
        int tongKhop = 0;

        for (MucKhach m : goc) {
            if (!m.khop(raw, khongDau)) continue;
            tongKhop++;

            KhachTiemNang k = m.khach();
            if (locHocThu == 1 && !k.daHocThu()) continue;
            if (locHocThu == 2 && k.daHocThu()) continue;

            List<KhachTiemNang> dsTab = theoTab.get(k.trangThai());
            if (dsTab != null) {
                dsTab.add(k);
            }
        }

        int canXuLy = 0;
        for (int viTri : TAB_CAN_XU_LY) {
            canXuLy += theoTab.get(MA_TAB[viTri]).size();
        }

        view.hienDanhSach(theoTab, canXuLy);
        view.capNhatNut(false, false);

        if (tabCanChon != null) {
            view.chonTab(tabCanChon);
            tabCanChon = null;
        }
        if (maCanChon > 0) {
            view.chonDongTheoMa(maCanChon);
            maCanChon = -1;
        }

        if (capNhatThongBao) {
            view.hienThongBao("Đã tải " + tongKhop + " hồ sơ"
                    + (raw.isBlank() ? "" : " theo từ khóa \"" + raw + "\""));
        }
    }

    public void tiepNhan() {
        view.moForm(null);
    }

    public void sua(KhachTiemNang khach) {
        if (!coChon(khach)) return;
        if ("DA_CHUYEN_DOI".equals(khach.trangThai())) {
            view.hienCanhBao("Hồ sơ đã chuyển đổi; hãy cập nhật thông tin ở màn hình Học viên.");
            return;
        }
        view.moForm(khach);
    }

    public void luu(KhachTiemNang cu, DuLieuForm duLieu) {
        if (duLieu == null) {
            view.hienCanhBao("Không nhận được dữ liệu hồ sơ.");
            return;
        }
        if (duLieu.hoTen() == null || duLieu.hoTen().isBlank()) {
            view.hienCanhBao("Bạn chưa nhập họ tên khách");
            return;
        }
        if (!SET_TRANG_THAI_SUA.contains(duLieu.trangThai())) {
            view.hienCanhBao("Trạng thái tuyển sinh không hợp lệ.");
            return;
        }
        if (cu != null && "DA_CHUYEN_DOI".equals(cu.trangThai())) {
            view.hienCanhBao("Hồ sơ đã chuyển đổi; không thể sửa tại màn hình Tuyển sinh.");
            return;
        }

        final KhachTiemNang moi = new KhachTiemNang(
                cu == null ? 0 : cu.maKhach(),
                0,
                duLieu.hoTen().trim(),
                rong(duLieu.soDienThoai()),
                rong(duLieu.tenNguoiLienHe()),
                rong(duLieu.nhuCau()),
                duLieu.trangThai(),
                duLieu.daHocThu(),
                rong(duLieu.ghiChu()));

        if (cu == null) {
            nen("Đang tiếp nhận hồ sơ...",
                    () -> service.them(coSo, moi),
                    maMoi -> {
                        maCanChon = maMoi;
                        tabCanChon = moi.trangThai();
                        view.hienThanhCong("Đã tiếp nhận hồ sơ #" + maMoi);
                        taiDanhSach(true);
                    });
        } else {
            final int maKhach = cu.maKhach();
            nen("Đang lưu hồ sơ...",
                    () -> {
                        service.sua(coSo, moi);
                        return maKhach;
                    },
                    maDaLuu -> {
                        maCanChon = maDaLuu;
                        tabCanChon = moi.trangThai();
                        view.hienThanhCong("Đã lưu hồ sơ. Hồ sơ được hiển thị tại tab '"
                                + tenTrangThai(moi.trangThai()) + "'.");
                        taiDanhSach(true);
                    });
        }
    }

    public void chuyenDoi(KhachTiemNang khach) {
        if (!coChon(khach)) return;
        if (!chuyenDuoc(khach)) {
            view.hienCanhBao("Trạng thái hồ sơ này không cho phép chuyển đổi.");
            return;
        }
        if (!view.xacNhan("Chuyển hồ sơ #" + khach.maKhach()
                + " thành học viên và tạo yêu cầu xếp lớp?")) {
            return;
        }

        final int maKhach = khach.maKhach();
        nen("Đang chuyển đổi hồ sơ...",
                () -> service.chuyenDoi(coSo, maKhach),
                maHocVien -> {
                    maCanChon = maKhach;
                    tabCanChon = "DA_CHUYEN_DOI";
                    view.hienThanhCong("Đã tạo học viên #" + maHocVien
                            + ". Hãy bổ sung ngày sinh/giới tính tại màn hình Học viên.");
                    taiDanhSach(true);
                });
    }

    public void xemHoSo(KhachTiemNang khach) {
        if (coChon(khach)) {
            view.hienHoSo(khach);
        }
    }

    public void saoChepSdt(KhachTiemNang khach) {
        if (!coChon(khach)) return;
        if (!coSoDienThoai(khach)) {
            view.hienCanhBao("Hồ sơ chưa có số điện thoại");
            return;
        }
        view.saoChepVaoClipboard(khach.soDienThoai());
        view.hienThongBao("Đã sao chép SĐT " + khach.soDienThoai());
    }

    public void khiChonDong(KhachTiemNang khach) {
        if (khach == null) {
            view.capNhatNut(false, false);
            return;
        }
        view.capNhatNut(suaDuoc(khach), chuyenDuoc(khach));
    }

    public void khiChuotPhai(KhachTiemNang khach) {
        if (khach == null) {
            view.capNhatMenu(false, false, false);
            return;
        }
        view.capNhatMenu(suaDuoc(khach), chuyenDuoc(khach), coSoDienThoai(khach));
    }

    public static String tenTrangThai(String ma) {
        if (ma == null) return "";
        for (int i = 0; i < MA_TAB.length; i++) {
            if (MA_TAB[i].equals(ma)) return TEN_TAB[i];
        }
        return ma;
    }

    private <T> void nen(String dangLam, Callable<T> viec, Consumer<T> khiXong) {
        view.chayNen(dangLam, viec, khiXong, this::baoLoi);
    }

    private <T> void nen(String dangLam, Callable<T> viec, Consumer<T> khiXong, Consumer<Throwable> khiLoi) {
        view.chayNen(dangLam, viec, khiXong, khiLoi);
    }

    private void baoLoi(Throwable ex) {
        view.hienLoi(loiGoc(ex));
    }

    private boolean coChon(KhachTiemNang khach) {
        if (khach == null) {
            view.hienCanhBao(CHUA_CHON);
        }
        return khach != null;
    }

    private static boolean suaDuoc(KhachTiemNang khach) {
        return khach != null && !"DA_CHUYEN_DOI".equals(khach.trangThai());
    }

    private static boolean chuyenDuoc(KhachTiemNang khach) {
        return khach != null
                && !"DA_CHUYEN_DOI".equals(khach.trangThai())
                && !"KHONG_DANG_KY".equals(khach.trangThai());
    }

    private static boolean coSoDienThoai(KhachTiemNang khach) {
        return khach.soDienThoai() != null && !khach.soDienThoai().isBlank();
    }

    private static String rong(String giaTri) {
        return giaTri == null || giaTri.trim().isEmpty() ? null : giaTri.trim();
    }

    private static String chuanHoa(String s) {
        if (s == null || s.isBlank()) return "";
        String khongDau = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return khongDau.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }

    private static String loiGoc(Throwable ex) {
        Throwable goc = ex;
        while (goc.getCause() != null && goc.getCause() != goc) {
            goc = goc.getCause();
        }
        return goc.getMessage() == null ? "lỗi không xác định" : goc.getMessage();
    }
}