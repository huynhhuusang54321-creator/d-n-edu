package vn.vinabacus.controller;

import java.sql.SQLException;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.HocVien;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.XepLopMamNon;
import vn.vinabacus.repository.CoSoRepository;
import vn.vinabacus.repository.HocVienRepository;
import vn.vinabacus.repository.KhoaHocRepository;
import vn.vinabacus.repository.LichHocLopRepository;
import vn.vinabacus.repository.LopTrungTamRepository;
import vn.vinabacus.repository.MamNonRepository;
import vn.vinabacus.service.HocVienService;
import vn.vinabacus.service.MamNonService;

/**
 * Toàn bộ logic vận hành màn Học viên / Học sinh: tải dữ liệu, lọc, tìm kiếm + gợi ý,
 * kiểm tra xếp lớp, thêm/sửa/xóa, tính tiến độ khóa học, ngày tốt nghiệp.
 * Panel chỉ dựng giao diện, bắt sự kiện gọi vào đây và hiển thị những gì controller đẩy ra qua {@link View}.
 *
 * Luồng lọc (một điểm vào duy nhất là hienKetQua):
 *   goc  --(trạng thái hồ sơ + xếp lớp)-->  sauLoc  --(tìm kiếm)-->  danh sách hiển thị
 *
 * Nguồn dữ liệu lớp phụ thuộc cơ sở:
 *  - Trung tâm: service.layLopDangHoc(coSo, maHocVien), hỏi từng học viên.
 *  - Mầm non: MamNonService.xepLop(null), một truy vấn cho cả cơ sở.
 */
public final class HocVienController {

    // ================= Hằng số =================

    public static final String[] GT_MA = {"", "NAM", "NU", "KHAC"};
    public static final String[] GT_TEN = {"(Chọn)", "Nam", "Nữ", "Khác"};
    public static final String[] TT_MA = {"HOAT_DONG", "NGUNG_SU_DUNG"};
    public static final String[] TT_TEN = {"Đang hoạt động", "Ngừng sử dụng"};
    /** Nhãn combo lọc trạng thái: chỉ số 0 = tất cả, i = TT_MA[i - 1]. */
    public static final String[] LOC_TEN = {"Tất cả", TT_TEN[0], TT_TEN[1]};
    /** Nhãn combo lọc xếp lớp; chỉ số khớp XEP_TAT_CA, XEP_CHUA, XEP_DA. */
    public static final String[] LOC_XEP_LOP = {"Tất cả", "Chưa xếp lớp", "Đã xếp lớp"};
    public static final int XEP_TAT_CA = 0, XEP_CHUA = 1, XEP_DA = 2;

    public static final DateTimeFormatter NGAY_VN =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter NGAY_NGAN = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");

    private static final String HIEU_LUC = "HIEU_LUC";
    private static final String CHUA_CHON = "Hãy chọn một học viên trong bảng";
    private static final String SAN_SANG = "Sẵn sàng";
    /** Số năm tối đa duyệt lịch để tìm đủ số buổi của khóa (chặn vòng lặp vô hạn). */
    private static final int SO_NAM_TOI_DA = 5;
    private static final int TOI_DA_GOI_Y = 10;

    private static final int LOAI_TEN = 0, LOAI_MA = 1, LOAI_SDT = 2;
    private static final String[] TEN_LOAI = {"tên", "mã", "SĐT"};
    private static final Pattern DAU = Pattern.compile("\\p{M}+");
    private static final Pattern KHOANG_TRANG = Pattern.compile("\\s+");
    private static final Pattern KY_TU_THUA = Pattern.compile("[\\s.\\-]");

    /** Ngày tốt nghiệp người dùng tự đặt. Chỉ lưu trong phiên làm việc; đọc ở luồng nền nên dùng map an toàn luồng. */
    private static final Map<KhoaHV, LocalDate> TOT_NGHIEP_TUY_CHINH = new ConcurrentHashMap<>();

    private record KhoaHV(int maHocVien, int maLop) { }

    // ================= Dữ liệu trao đổi với View =================

    /** Dữ liệu form thêm/sửa. Giới tính và trạng thái là chỉ số combo, controller tự đổi sang mã CSDL. */
    public record DuLieuForm(String hoTen, LocalDate ngaySinh, int chiSoGioiTinh, String tenNguoiLienHe,
                             String soDienThoai, String diaChi, String ghiChu, int chiSoTrangThai) { }

    /** Một gợi ý tìm kiếm: nhãn hiển thị + học viên tương ứng. */
    public record GoiY(HocVien hocVien, String nhan) { }

    /** Một dòng lớp đang học (dùng khi không có dữ liệu khóa). */
    public record DongLop(int maLop, String moTa) { }

    /** Trạng thái hiển thị của một buổi học. */
    public enum TrangThaiBuoi {
        CO_MAT("Có mặt"), VANG_CO_PHEP("Vắng có phép"), VANG_KHONG_PHEP("Vắng không phép"),
        CHUA_DIEM_DANH("Chưa điểm danh"), CHUA_DEN("Chưa đến");

        public final String ten;

        TrangThaiBuoi(String ten) {
            this.ten = ten;
        }

        /** Đổi mã điểm danh trong CSDL (có thể null) sang trạng thái; daQua = buổi đã diễn ra. */
        static TrangThaiBuoi tu(String ma, boolean daQua) {
            if ("CO_MAT".equals(ma)) return CO_MAT;
            if ("VANG_CO_PHEP".equals(ma)) return VANG_CO_PHEP;
            if (ma != null && ma.startsWith("VANG")) return VANG_KHONG_PHEP;
            return daQua ? CHUA_DIEM_DANH : CHUA_DEN;
        }
    }

    /** Một buổi học theo lịch. */
    public record BuoiHoc(int stt, LocalDate ngay, TrangThaiBuoi trangThai) {
        public String nhanNgay() {
            return NGAY_NGAN.format(ngay);
        }

        public String goiY() {
            return "Buổi " + stt + " · " + tenThu(ngay.getDayOfWeek()) + " " + NGAY_VN.format(ngay)
                    + " · " + trangThai.ten;
        }
    }

    /** Một buổi vắng (VANG_CO_PHEP hoặc VANG_KHONG_PHEP). */
    public record Vang(LocalDate ngay, TrangThaiBuoi trangThai) {
        public String moTa() {
            return tenThu(ngay.getDayOfWeek()) + " " + NGAY_VN.format(ngay) + " — " + trangThai.ten;
        }
    }

    /** Tiến độ của học viên trong một khóa/lớp đang học. Các hàm text là chuỗi sẵn để hiển thị. */
    public record KhoaTienDo(int maLop, String tenLop, String tenKhoaHoc, String tenMonHoc, int tongSoBuoi,
                             String lichHoc, LocalDate ngayNhapHoc, LocalDate ngayKetThuc,
                             LocalDate ngayTotNghiep, boolean totNghiepTuyChinh, int daHoc,
                             boolean coDiemDanh, List<BuoiHoc> buoi, List<Vang> vang, String ghiChu,
                             boolean lienTuc) {

        public int phanTram() {
            if (lienTuc) return 100;
            return tongSoBuoi <= 0 ? 0 : Math.min(100, daHoc * 100 / tongSoBuoi);
        }

        /** Tên khóa; nếu khóa không có tên thì dùng tên lớp. */
        public String tenChinh() {
            return tenKhoaHoc.isBlank() ? tenLop : tenKhoaHoc;
        }

        public String khoaVaMon() {
            return tenKhoaHoc + (tenMonHoc.isBlank() ? "" : " (" + tenMonHoc + ")");
        }

        public String textTienDo() {
            if (lienTuc) return daHoc + " buổi đã học";
            return daHoc + "/" + tongSoBuoi + " buổi · " + phanTram() + "%";
        }

        public String textTotNghiep() {
            return ngay(ngayTotNghiep) + (totNghiepTuyChinh ? " (tùy chỉnh)" : "");
        }

        public String nhanTotNghiep() {
            return totNghiepTuyChinh ? "(tùy chỉnh)" : ngayTotNghiep == null ? "" : "(tự tính)";
        }

        public String textVang() {
            return coDiemDanh ? String.valueOf(vang.size()) : "—";
        }

        /** Số buổi theo trạng thái; vắng tính từ danh sách vắng đầy đủ (kể cả buổi ngoài lịch). */
        public long dem(TrangThaiBuoi t) {
            return switch (t) {
                case VANG_CO_PHEP, VANG_KHONG_PHEP -> vang.stream().filter(v -> v.trangThai() == t).count();
                default -> buoi.stream().filter(b -> b.trangThai() == t).count();
            };
        }

        /** Buổi "chưa đến" đầu tiên theo lịch, null nếu không còn buổi nào hoặc học liên tục (Mầm non). */
        public BuoiHoc buoiTiepTheo() {
            if (lienTuc) return null;
            return buoi.stream().filter(b -> b.trangThai() == TrangThaiBuoi.CHUA_DEN).findFirst().orElse(null);
        }

        public String thongKe() {
            if (!coDiemDanh) {
                return "Chưa có dữ liệu điểm danh cho lớp này (chỉ hiển thị tiến độ theo lịch học)";
            }
            return "Có mặt: " + dem(TrangThaiBuoi.CO_MAT)
                    + "  ·  Vắng có phép: " + dem(TrangThaiBuoi.VANG_CO_PHEP)
                    + "  ·  Vắng không phép: " + dem(TrangThaiBuoi.VANG_KHONG_PHEP)
                    + "  ·  Chưa điểm danh: " + dem(TrangThaiBuoi.CHUA_DIEM_DANH);
        }

        public String danhSachVangText() {
            if (!coDiemDanh) return "Chưa có dữ liệu điểm danh.";
            if (vang.isEmpty()) return "Không có buổi vắng.";
            return vang.stream().map(Vang::moTa).collect(Collectors.joining("\n"));
        }

        KhoaTienDo voiTotNghiep(LocalDate ngay, boolean tuyChinh) {
            return new KhoaTienDo(maLop, tenLop, tenKhoaHoc, tenMonHoc, tongSoBuoi, lichHoc, ngayNhapHoc,
                    ngayKetThuc, ngay, tuyChinh, daHoc, coDiemDanh, buoi, vang, ghiChu, lienTuc);
        }
    }

    /** Việc controller yêu cầu giao diện làm. View không chứa logic nghiệp vụ. */
    public interface View {
        void hienDanhSach(List<HocVien> ds);
        void hienTongKet(int hoatDong, int ngungSuDung);
        void hienThongBao(String noiDung);
        void hienThanhCong(String noiDung);
        void hienCanhBao(String noiDung);
        void hienLoi(String noiDung);
        boolean xacNhan(String noiDung);
        void moForm(HocVien cu);
        /** Hiện hồ sơ. khoa không rỗng: hiện bảng khóa/lớp; ngược lại lop không rỗng: hiện danh sách lớp; cả hai rỗng: chưa xếp lớp. */
        void hienHoSo(HocVien h, List<DongLop> lop, List<KhoaTienDo> khoa, boolean coXemLop);
        /** Mở (hoặc làm mới) sơ đồ một khóa/lớp, đồng thời cập nhật dòng ngày tốt nghiệp trong hồ sơ. */
        void hienSoDo(HocVien h, KhoaTienDo k);
        /** Hiện popup gợi ý (ds rỗng = ẩn). conLai = số kết quả khớp nhưng không nằm trong ds. */
        void hienGoiY(List<GoiY> ds, int conLai);
        /** Điền ô tìm kiếm mà không kích hoạt gợi ý, và ẩn popup. */
        void datOTimKiem(String noiDung);
        /** Đưa ô tìm kiếm và các combo lọc về mặc định. */
        void datLaiBoLoc();
        void saoChepVaoClipboard(String noiDung);
        void capNhatMenu(boolean coSoDienThoai);
        /** Chạy viec ở luồng nền, rồi khiXong (hoặc khiLoi) ở luồng giao diện. */
        <T> void chayNen(String dangLam, Callable<T> viec, Consumer<T> khiXong, Consumer<Throwable> khiLoi);
    }

    // ================= Nội bộ: mô hình tìm kiếm =================

    /** Một học viên cùng khóa tìm kiếm đã chuẩn hóa (theo LOAI_TEN, LOAI_MA, LOAI_SDT), tính một lần khi tải. */
    private record Muc(HocVien hv, String[] khoa) { }

    /** Điều kiện tìm kiếm hiện tại. chinhXac = true khi chọn từ gợi ý. */
    private record TimKiem(int loai, String khoa, boolean chinhXac) {
        boolean khop(Muc m) {
            String g = m.khoa()[loai];
            if (chinhXac) return g.equals(khoa);
            return loai == LOAI_TEN ? g.contains(khoa) : g.startsWith(khoa);
        }
    }

    /** Dữ liệu hồ sơ: lớp (Mầm non) + tiến độ từng khóa/lớp + cảnh báo nếu có. */
    private record HoSoDuLieu(List<DongLop> lop, List<KhoaTienDo> khoa, String canhBao) { }

    // ================= Trạng thái =================

    private final CoSo coSo;
    private final boolean laMamNon;
    private final View view;
    private final IntConsumer khiXemLop;   // null nếu chưa có màn Lớp
    private final HocVienService service = new HocVienService();
    private final MamNonService mamNon = new MamNonService();
    private final CoSoRepository coSoRepo = new CoSoRepository();
    private final LopTrungTamRepository lopRepo = new LopTrungTamRepository();
    private final KhoaHocRepository khoaRepo = new KhoaHocRepository();
    private final LichHocLopRepository lichRepo = new LichHocLopRepository();
    /** Repo điểm danh dùng chung (đặt ở MamNonRepository). */
    private final MamNonRepository diemDanhRepo = new MamNonRepository();
    private volatile Integer maCoSoCache;

    private List<Muc> goc = List.of();       // toàn bộ học viên của cơ sở
    private List<Muc> sauLoc = List.of();    // sau lọc trạng thái + xếp lớp, trước tìm kiếm (nguồn gợi ý)
    private String trangThaiLoc;             // null = tất cả
    private int locXepLop = XEP_TAT_CA;
    private TimKiem tim;                     // null = không tìm kiếm
    private long phien = 0;                  // chặn kết quả cũ của tải danh sách

    /** Mã học viên -> đã có lớp đang học hay chưa. Chỉ truy cập trên luồng giao diện. */
    private final Map<Integer, Boolean> coLopCache = new HashMap<>();
    private long doiCache = 0;               // tăng mỗi lần xóa coLopCache để bỏ kết quả kiểm tra cũ
    private boolean dangKiemTra = false;     // chỉ cho một tác vụ kiểm tra lớp chạy cùng lúc

    /** Các khóa/lớp của hồ sơ đang mở (mã lớp -> tiến độ mới nhất). Chỉ truy cập trên luồng giao diện. */
    private final Map<Integer, KhoaTienDo> khoaHoSo = new HashMap<>();

    public HocVienController(CoSo coSo, View view, IntConsumer khiXemLop) {
        this.coSo = coSo;
        this.laMamNon = coSo == CoSo.MAM_NON;
        this.view = view;
        this.khiXemLop = khiXemLop;
    }

    // ================= Tải và lọc =================

    /** Tải danh sách từ CSDL (qua cache của service), rồi áp dụng bộ lọc hiện tại. */
    public void taiDanhSach(boolean lamMoi) {
        final long p = ++phien;
        if (lamMoi) xoaCacheLop();
        nen(lamMoi ? "Đang làm mới dữ liệu..." : "Đang tải dữ liệu...", () -> {
            if (lamMoi) service.lamMoi(coSo);
            List<Muc> ds = new ArrayList<>();
            for (HocVien h : service.timKiem(coSo, "", "TAT_CA")) ds.add(muc(h));
            return ds;
        }, ds -> {
            if (p != phien) return;
            goc = ds;
            hienKetQua();
        });
    }

    /** @param chiSo chỉ số trong LOC_TEN của combo trạng thái */
    public void apDungTrangThai(int chiSo) {
        trangThaiLoc = chiSo <= 0 ? null : TT_MA[chiSo - 1];
        hienKetQua();
    }

    /** @param chiSo XEP_TAT_CA, XEP_CHUA hoặc XEP_DA (chỉ số trong LOC_XEP_LOP) */
    public void apDungLocXepLop(int chiSo) {
        locXepLop = chiSo;
        // Mầm non chỉ tốn một truy vấn, nên mỗi lần chọn lọc xếp lớp đều đọc dữ liệu mới
        // (màn Xếp lớp có thể vừa thay đổi).
        if (laMamNon && chiSo != XEP_TAT_CA) xoaCacheLop();
        hienKetQua();
    }

    /** Nút Reset: đặt lại toàn bộ bộ lọc (kể cả ô giao diện) và tải lại từ CSDL. */
    public void taiLai() {
        trangThaiLoc = null;
        locXepLop = XEP_TAT_CA;
        tim = null;
        view.datLaiBoLoc();
        taiDanhSach(true);
    }

    /**
     * Điểm vào duy nhất để lọc và hiển thị. Nếu đang lọc xếp lớp mà còn học viên chưa biết
     * đã có lớp hay chưa thì kiểm tra họ trước (nền), xong tự gọi lại hàm này.
     */
    private void hienKetQua() {
        if (locXepLop != XEP_TAT_CA) {
            List<Integer> thieu = goc.stream().filter(this::khopTrangThai)
                    .map(m -> m.hv().maHocVien())
                    .filter(ma -> !coLopCache.containsKey(ma))
                    .toList();
            if (!thieu.isEmpty()) {
                if (!dangKiemTra) kiemTraLop(thieu);
                return;   // đang kiểm tra thì chờ, xong sẽ tự hienKetQua lại
            }
        }
        locVaHien();
    }

    /** Hỏi nguồn lớp của cơ sở cho các học viên còn thiếu, ghi vào coLopCache rồi hiển thị lại. */
    private void kiemTraLop(List<Integer> thieu) {
        dangKiemTra = true;
        final long doi = doiCache;
        view.chayNen(laMamNon ? "Đang đọc xếp lớp Mầm non..." : "Đang kiểm tra lớp của " + thieu.size() + " học viên...",
                () -> {
                    Map<Integer, Boolean> kq = new HashMap<>();
                    if (laMamNon) {
                        // Một truy vấn cho cả cơ sở: ai có dòng xếp lớp còn hiệu lực là đã có lớp.
                        Set<Integer> dangHoc = new HashSet<>();
                        for (XepLopMamNon x : xepLopMamNonHieuLuc()) dangHoc.add(x.maHocVien());
                        for (int ma : thieu) kq.put(ma, dangHoc.contains(ma));
                    } else {
                        for (int ma : thieu) kq.put(ma, !service.layLopDangHoc(coSo, ma).isEmpty());
                    }
                    return kq;
                },
                kq -> {
                    dangKiemTra = false;
                    if (doi == doiCache) coLopCache.putAll(kq);   // cache chưa bị xóa giữa chừng
                    hienKetQua();
                },
                ex -> {
                    dangKiemTra = false;   // không gọi lại để tránh lặp vô hạn; chọn lại bộ lọc để thử lại
                    view.hienCanhBao("Không kiểm tra được xếp lớp: " + loiGoc(ex));
                });
    }

    /** Lọc trong bộ nhớ theo trạng thái, xếp lớp, tìm kiếm rồi đẩy ra giao diện. */
    private void locVaHien() {
        sauLoc = goc.stream().filter(this::khopTrangThai).filter(this::khopXepLop).toList();
        final TimKiem tk = tim;
        List<Muc> hien = tk == null ? sauLoc : sauLoc.stream().filter(tk::khop).toList();
        int ngung = (int) hien.stream().filter(m -> TT_MA[1].equals(m.hv().trangThaiHoSo())).count();

        view.hienDanhSach(hien.stream().map(Muc::hv).toList());
        view.hienTongKet(hien.size() - ngung, ngung);
        String hauXepLop = locXepLop == XEP_CHUA ? " (chưa xếp lớp)" : locXepLop == XEP_DA ? " (đã xếp lớp)" : "";
        String hauTim = tk == null ? "" : " · tìm theo " + TEN_LOAI[tk.loai()];
        view.hienThongBao("Hiển thị " + hien.size() + " / " + goc.size() + " học viên" + hauXepLop + hauTim);
    }

    private boolean khopTrangThai(Muc m) {
        return trangThaiLoc == null || trangThaiLoc.equals(m.hv().trangThaiHoSo());
    }

    private boolean khopXepLop(Muc m) {
        if (locXepLop == XEP_TAT_CA) return true;
        boolean daCoLop = Boolean.TRUE.equals(coLopCache.get(m.hv().maHocVien()));
        return (locXepLop == XEP_DA) == daCoLop;
    }

    private void xoaCacheLop() {
        coLopCache.clear();
        doiCache++;
    }

    // ================= Tìm kiếm và gợi ý =================

    /** Gõ vào ô tìm kiếm: tính tối đa 10 gợi ý trong danh sách đang lọc (chữ = tên, 1-9 = mã, 0 = SĐT). */
    public void goiY(String nhap) {
        String q = nhap == null ? "" : nhap.trim();
        int loai = loaiTimKiem(q);
        if (loai < 0) {
            view.hienGoiY(List.of(), 0);
            return;
        }
        TimKiem tk = new TimKiem(loai, khoaTim(loai, q), false);
        List<GoiY> ds = new ArrayList<>();
        int tong = 0;
        for (Muc m : sauLoc) {
            if (!tk.khop(m)) continue;
            if (ds.size() < TOI_DA_GOI_Y) ds.add(taoGoiY(m.hv()));
            tong++;
        }
        view.hienGoiY(ds, tong - ds.size());
    }

    /** Nút Tìm kiếm / Enter / "More": lọc mọi học viên khớp với nội dung đã nhập. */
    public void timKiem(String nhap) {
        view.hienGoiY(List.of(), 0);
        String q = nhap == null ? "" : nhap.trim();
        if (q.isEmpty()) {
            tim = null;
        } else {
            int loai = loaiTimKiem(q);
            if (loai < 0) {
                view.hienCanhBao("Nhập chữ để tìm theo tên, số 1-9 để tìm theo mã, số 0 để tìm theo SĐT.");
                return;
            }
            tim = new TimKiem(loai, khoaTim(loai, q), false);
        }
        hienKetQua();
    }

    /** Chọn một gợi ý: điền ô tìm kiếm bằng giá trị đầy đủ rồi lọc đúng học viên đó. */
    public void chonGoiY(String nhap, HocVien h) {
        int loai = loaiTimKiem(nhap == null ? "" : nhap.trim());
        if (loai < 0 || h == null) return;
        String giaTri = loai == LOAI_TEN ? h.hoTen()
                : loai == LOAI_MA ? String.valueOf(h.maHocVien()) : chuoi(h.soDienThoaiLienHe());
        view.datOTimKiem(giaTri);
        tim = new TimKiem(loai, khoaTim(loai, giaTri), true);
        hienKetQua();
    }

    private static GoiY taoGoiY(HocVien h) {
        String sdt = chuoi(h.soDienThoaiLienHe());
        return new GoiY(h, "#" + h.maHocVien() + "  •  " + h.hoTen() + (sdt.isBlank() ? "" : "  •  " + sdt));
    }

    /** 0 = tên (chữ), 1 = mã (số 1-9), 2 = SĐT (số 0), -1 = không hợp lệ. */
    private static int loaiTimKiem(String q) {
        if (q.isEmpty()) return -1;
        char c = q.charAt(0);
        if (c == '0') return LOAI_SDT;
        if (c >= '1' && c <= '9') return LOAI_MA;
        return Character.isLetter(c) ? LOAI_TEN : -1;
    }

    private static Muc muc(HocVien h) {
        return new Muc(h, new String[]{chuanHoaTen(h.hoTen()), String.valueOf(h.maHocVien()),
                khoaTim(LOAI_SDT, h.soDienThoaiLienHe())});
    }

    private static String khoaTim(int loai, String s) {
        return loai == LOAI_TEN ? chuanHoaTen(s) : KY_TU_THUA.matcher(chuoi(s)).replaceAll("");
    }

    /** Bỏ dấu, thường hóa, gộp khoảng trắng: "Nguyễn  Đạt" -> "nguyen dat". */
    private static String chuanHoaTen(String s) {
        if (s == null) return "";
        String t = DAU.matcher(Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("");
        return KHOANG_TRANG.matcher(t.trim()).replaceAll(" ").toLowerCase(Locale.ROOT).replace('đ', 'd');
    }

    // ================= Thêm / sửa / xóa / tiện ích dòng =================

    public void them() {
        view.moForm(null);
    }

    public void sua(HocVien h) {
        if (coChon(h)) view.moForm(h);
    }

    /** Lưu từ form. cu == null là thêm mới. */
    public void luu(HocVien cu, DuLieuForm d) {
        if (d.hoTen() == null || d.hoTen().isBlank()) {
            view.hienCanhBao("Bạn chưa nhập họ tên học viên");
            return;
        }
        final boolean them = cu == null;
        final HocVien h = new HocVien(
                them ? 0 : cu.maHocVien(), 0,
                d.hoTen().trim(), d.ngaySinh(),
                rong(GT_MA[d.chiSoGioiTinh()]), rong(d.tenNguoiLienHe()), rong(d.soDienThoai()),
                rong(d.diaChi()), rong(d.ghiChu()), TT_MA[d.chiSoTrangThai()]);

        nen(them ? "Đang thêm học viên..." : "Đang lưu học viên...", () -> {
            if (them) return "Đã thêm học viên mã " + service.them(coSo, h);
            service.sua(coSo, h);
            return "Đã lưu thay đổi học viên mã " + h.maHocVien();
        }, thongBao -> {
            view.hienThanhCong(thongBao);
            taiDanhSach(false);
        });
    }

    public void xoa(HocVien h) {
        if (!coChon(h)) return;
        if (!view.xacNhan("Xóa học viên \"" + h.hoTen() + "\" (mã " + h.maHocVien() + ")?\n"
                + "Hành động này không thể hoàn tác.")) {
            return;
        }
        nen("Đang xóa học viên...", () -> {
            service.xoa(coSo, h.maHocVien());
            return h.hoTen();
        }, ten -> {
            view.hienThanhCong("Đã xóa học viên " + ten);
            coLopCache.remove(h.maHocVien());
            taiDanhSach(false);
        });
    }

    public void saoChepSdt(HocVien h) {
        if (h == null || !coSoDienThoai(h)) {
            view.hienCanhBao("Học viên chưa có số điện thoại");
            return;
        }
        view.saoChepVaoClipboard(h.soDienThoaiLienHe());
        view.hienThongBao("Đã sao chép SĐT " + h.soDienThoaiLienHe());
    }

    /** Khi chuột phải: controller quyết định mục nào được dùng. */
    public void khiChuotPhai(HocVien h) {
        view.capNhatMenu(h != null && coSoDienThoai(h));
    }

    /** Nút Xem lớp: chuyển sang màn Lớp. */
    public void xemLop(int maLop) {
        if (khiXemLop == null) {
            view.hienCanhBao("Màn Lớp chưa được kết nối");
            return;
        }
        khiXemLop.accept(maLop);
    }

    /**
     * Nút Xếp lớp: chưa có hành vi.
     * Mầm non sẽ nối với MamNonService.xepHocSinh(lop, hocSinh, tuNgay) sau khi có hộp chọn lớp và ngày.
     * Khi xếp lớp thành công, gọi taiLai() để bộ lọc xếp lớp đọc lại dữ liệu mới.
     */
    public void xepLop(HocVien h) {
        if (coChon(h)) view.hienThongBao("Chức năng xếp lớp đang được phát triển");
    }

    // ================= Hồ sơ, sơ đồ khóa, ngày tốt nghiệp =================

    /** Mở hồ sơ: tải lớp đang học (+ tiến độ từng khóa/lớp) rồi hiển thị. */
    public void xemHoSo(HocVien h) {
        if (!coChon(h)) return;
        nen("Đang tải hồ sơ...", () -> taiHoSo(h), d -> {
            coLopCache.put(h.maHocVien(), !d.lop().isEmpty() || !d.khoa().isEmpty());   // dữ liệu mới nhất
            khoaHoSo.clear();
            for (KhoaTienDo k : d.khoa()) khoaHoSo.put(k.maLop(), k);
            view.hienThongBao(d.canhBao() == null ? SAN_SANG : d.canhBao());
            view.hienHoSo(h, d.lop(), d.khoa(), khiXemLop != null);
        });
    }

    /** Nút Sơ đồ: hiển thị sơ đồ buổi học của một khóa/lớp trong hồ sơ đang mở. */
    public void xemSoDo(HocVien h, int maLop) {
        KhoaTienDo k = khoaHoSo.get(maLop);
        if (h == null || k == null) {
            view.hienCanhBao("Không có dữ liệu khóa học để hiển thị");
            return;
        }
        view.hienSoDo(h, k);
    }

    /** Nút Lưu ở sơ đồ: đặt ngày tốt nghiệp thủ công (chỉ lưu trong phiên làm việc). */
    public void datNgayTotNghiep(HocVien h, int maLop, String nhap) {
        KhoaTienDo k = khoaHoSo.get(maLop);
        if (h == null || k == null || k.lienTuc()) return;
        if (nhap == null || nhap.isBlank()) {
            view.hienCanhBao("Bạn chưa nhập ngày tốt nghiệp");
            return;
        }
        final LocalDate ngay;
        try {
            ngay = LocalDate.parse(nhap.trim(), NGAY_VN);
        } catch (DateTimeParseException ex) {
            view.hienCanhBao("Ngày tốt nghiệp phải theo định dạng dd/MM/yyyy");
            return;
        }
        if (k.ngayNhapHoc() != null && ngay.isBefore(k.ngayNhapHoc())) {
            view.hienCanhBao("Ngày tốt nghiệp không được trước ngày nhập học");
            return;
        }
        TOT_NGHIEP_TUY_CHINH.put(new KhoaHV(h.maHocVien(), maLop), ngay);
        capNhatKhoa(h, k.voiTotNghiep(ngay, true));
        view.hienThongBao("Đã đặt ngày tốt nghiệp tùy chỉnh (lưu trong phiên làm việc hiện tại)");
    }

    /** Nút Tự tính ở sơ đồ: ngày tốt nghiệp = ngày kết thúc khóa (buổi học cuối theo lịch). */
    public void tuTinhNgayTotNghiep(HocVien h, int maLop) {
        KhoaTienDo k = khoaHoSo.get(maLop);
        if (h == null || k == null || k.lienTuc()) return;
        if (k.ngayKetThuc() == null) {
            view.hienCanhBao(k.ghiChu() == null ? "Chưa xác định được ngày kết thúc khóa" : k.ghiChu());
            return;
        }
        TOT_NGHIEP_TUY_CHINH.remove(new KhoaHV(h.maHocVien(), maLop));
        capNhatKhoa(h, k.voiTotNghiep(k.ngayKetThuc(), false));
        view.hienThongBao("Đã tự tính ngày tốt nghiệp: ngày học buổi cuối của khóa");
    }

    private void capNhatKhoa(HocVien h, KhoaTienDo k) {
        khoaHoSo.put(k.maLop(), k);
        view.hienSoDo(h, k);
    }

    // ================= Tải hồ sơ và tính tiến độ (chạy ở luồng nền) =================

    private HoSoDuLieu taiHoSo(HocVien h) throws SQLException {
        LocalDate homNay = LocalDate.now();
        if (laMamNon) {
            List<XepLopMamNon> dsLop = xepLopMamNonCuaHocVien(h.maHocVien());
            if (dsLop.isEmpty()) return new HoSoDuLieu(List.of(), List.of(), null);

            List<DongLop> lop = new ArrayList<>();
            LocalDate som = null;
            for (XepLopMamNon x : dsLop) {
                lop.add(new DongLop(x.maLop(), x.tenLop() + " • Đang học • từ " + ngay(x.tuNgay())));
                LocalDate batDau = x.tuNgay() == null ? homNay : x.tuNgay();
                if (som == null || batDau.isBefore(som)) som = batDau;
            }

            int ma = maCoSo();
            List<MamNonRepository.LichSuDiemDanh> diemDanh = List.of();
            String canhBao = null;
            if (som != null && !som.isAfter(homNay)) {
                try {
                    diemDanh = diemDanhRepo.lichSuDiemDanh(ma, null, h.maHocVien(), som, homNay);
                } catch (SQLException ex) {
                    canhBao = "Không đọc được điểm danh: " + loiGoc(ex);
                }
            }

            List<KhoaTienDo> khoa = new ArrayList<>();
            for (XepLopMamNon x : dsLop) {
                khoa.add(tinhKhoaMamNon(x, diemDanh, homNay));
            }
            return new HoSoDuLieu(lop, khoa, canhBao);
        }

        List<HocVienRepository.LopDangHoc> lop = service.layLopDangHoc(coSo, h.maHocVien());
        if (lop.isEmpty()) return new HoSoDuLieu(List.of(), List.of(), null);

        int ma = maCoSo();
        Set<Integer> cacLop = new HashSet<>();
        LocalDate som = null;   // ngày xếp lớp sớm nhất, làm mốc đọc điểm danh
        for (HocVienRepository.LopDangHoc l : lop) {
            cacLop.add(l.maLop());
            if (l.tuNgay() != null && (som == null || l.tuNgay().isBefore(som))) som = l.tuNgay();
        }

        // Lớp -> khóa -> tổng số buổi
        Map<Integer, Integer> khoaCuaLop = new HashMap<>();
        for (LopTrungTam x : lopRepo.layDanhSach(ma)) {
            if (cacLop.contains(x.maLop())) khoaCuaLop.put(x.maLop(), x.maKhoaHoc());
        }
        Map<Integer, Integer> tongBuoi = new HashMap<>();
        for (KhoaHoc k : khoaRepo.layDanhSach(ma)) tongBuoi.put(k.maKhoaHoc(), k.tongSoBuoi());

        // Lịch học hiệu lực của các lớp này
        Map<Integer, List<LichHocLop>> lichCuaLop = new HashMap<>();
        for (LichHocLop x : lichRepo.layDanhSach(ma)) {
            if (cacLop.contains(x.maLop()) && HIEU_LUC.equals(x.trangThai())) {
                lichCuaLop.computeIfAbsent(x.maLop(), q -> new ArrayList<>()).add(x);
            }
        }

        // Điểm danh qua repo dùng chung; lỗi hoặc không có dữ liệu thì chỉ hiện tiến độ theo lịch.
        List<MamNonRepository.LichSuDiemDanh> diemDanh = List.of();
        String canhBao = null;
        if (som != null && !som.isAfter(homNay)) {
            try {
                diemDanh = diemDanhRepo.lichSuDiemDanh(ma, null, h.maHocVien(), som, homNay);
            } catch (SQLException ex) {
                canhBao = "Không đọc được điểm danh: " + loiGoc(ex);
            }
        }

        List<KhoaTienDo> khoa = new ArrayList<>();
        for (HocVienRepository.LopDangHoc l : lop) {
            Integer maKhoa = khoaCuaLop.get(l.maLop());
            int tong = maKhoa == null ? 0 : tongBuoi.getOrDefault(maKhoa, 0);
            khoa.add(tinhKhoa(h, l, tong, lichCuaLop.getOrDefault(l.maLop(), List.of()), diemDanh, homNay));
        }
        return new HoSoDuLieu(List.of(), khoa, canhBao);
    }

    /**
     * Mầm non: học Thứ 2 - Thứ 7, không có ngày kết thúc khóa, duyệt tăng dần từ ngày học đầu tiên
     * (ngày xếp lớp) và giới hạn trong 1 tháng đếm (tối đa tới ngày hiện tại).
     */
/**
     * Mầm non: học Thứ 2 - Thứ 7, không có ngày kết thúc khóa, duyệt tăng dần từ ngày học đầu tiên
     * (ngày xếp lớp) tới ngày hiện tại. Giao diện sơ đồ sẽ phân trang theo từng khoảng 1 tháng để lật xem.
     */
    private KhoaTienDo tinhKhoaMamNon(XepLopMamNon x, List<MamNonRepository.LichSuDiemDanh> diemDanh,
                                      LocalDate homNay) {
        LocalDate batDau = x.tuNgay() == null ? homNay : x.tuNgay();
        List<LocalDate> ngay = layNgayHocMamNon(batDau, homNay);
        LocalDate nhapHoc = ngay.isEmpty() ? null : ngay.get(0);

        Map<LocalDate, String> theoNgay = new HashMap<>();
        List<Vang> vang = new ArrayList<>();
        boolean coDiemDanh = false;
        for (MamNonRepository.LichSuDiemDanh d : diemDanh) {
            if (d.ngayHoc() == null || !chuoi(x.tenLop()).equals(d.tenLop())) continue;
            if (nhapHoc != null && d.ngayHoc().isBefore(nhapHoc)) continue;
            coDiemDanh = true;
            theoNgay.put(d.ngayHoc(), d.trangThai());
            TrangThaiBuoi t = TrangThaiBuoi.tu(d.trangThai(), true);
            if (t == TrangThaiBuoi.VANG_CO_PHEP || t == TrangThaiBuoi.VANG_KHONG_PHEP) {
                vang.add(new Vang(d.ngayHoc(), t));
            }
        }
        vang.sort(Comparator.comparing(Vang::ngay));

        List<BuoiHoc> buoi = new ArrayList<>();
        for (int i = 0; i < ngay.size(); i++) {
            LocalDate d = ngay.get(i);
            buoi.add(new BuoiHoc(i + 1, d, TrangThaiBuoi.tu(theoNgay.get(d), true)));
        }

        String ghiChu = ngay.isEmpty()
                ? "Chưa có buổi học nào kể từ ngày bắt đầu."
                : "Hiển thị các buổi đã học tăng dần từ ngày bắt đầu (T2–T7, mỗi trang 1 tháng, không kết thúc khóa).";

        return new KhoaTienDo(x.maLop(), chuoi(x.tenLop()), chuoi(x.tenLop()), "", 0,
                "T2 – T7", nhapHoc, null, null, false,
                buoi.size(), coDiemDanh, buoi, vang, ghiChu, true);
    }

    /** Duyệt tăng dần từ tu đến homNay, chỉ lấy ngày T2..T7. */
    private static List<LocalDate> layNgayHocMamNon(LocalDate tu, LocalDate homNay) {
        List<LocalDate> ngay = new ArrayList<>();
        if (tu == null || homNay == null || tu.isAfter(homNay)) return ngay;
        for (LocalDate d = tu; !d.isAfter(homNay); d = d.plusDays(1)) {
            if (laBuoiHocMamNon(d)) ngay.add(d);
        }
        return ngay;
    }

    /** Thứ 2 đến Thứ 7 (sau này có thể lọc thêm ngày lễ nghỉ tại đây). */
    private static boolean laBuoiHocMamNon(LocalDate ngay) {
        return ngay.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    /**
     * Công thức Trung tâm: từ ngày xếp lớp, duyệt từng ngày; ngày nào khớp thứ + khoảng ngày của một lịch HIEU_LUC
     * là một buổi. Buổi đầu = ngày nhập học (không xét vắng). Buổi thứ TongSoBuoi = ngày kết thúc khóa.
     */
    private KhoaTienDo tinhKhoa(HocVien h, HocVienRepository.LopDangHoc l, int tong, List<LichHocLop> lich,
                                List<MamNonRepository.LichSuDiemDanh> diemDanh, LocalDate homNay) {
        List<LocalDate> ngay = layNgayHoc(lich, tong, l.tuNgay() == null ? homNay : l.tuNgay());
        LocalDate nhapHoc = ngay.isEmpty() ? null : ngay.get(0);
        LocalDate ketThuc = tong > 0 && ngay.size() == tong ? ngay.get(tong - 1) : null;

        // Điểm danh của đúng lớp này, từ ngày nhập học trở đi
        Map<LocalDate, String> theoNgay = new HashMap<>();
        List<Vang> vang = new ArrayList<>();
        boolean coDiemDanh = false;
        for (MamNonRepository.LichSuDiemDanh d : diemDanh) {
            if (d.ngayHoc() == null || !l.tenLop().equals(d.tenLop())) continue;
            if (nhapHoc != null && d.ngayHoc().isBefore(nhapHoc)) continue;
            coDiemDanh = true;
            theoNgay.put(d.ngayHoc(), d.trangThai());
            TrangThaiBuoi t = TrangThaiBuoi.tu(d.trangThai(), true);
            if (t == TrangThaiBuoi.VANG_CO_PHEP || t == TrangThaiBuoi.VANG_KHONG_PHEP) {
                vang.add(new Vang(d.ngayHoc(), t));
            }
        }
        vang.sort(Comparator.comparing(Vang::ngay));

        List<BuoiHoc> buoi = new ArrayList<>();
        int daHoc = 0;
        for (int i = 0; i < ngay.size(); i++) {
            LocalDate d = ngay.get(i);
            boolean daQua = !d.isAfter(homNay);
            if (daQua) daHoc++;
            buoi.add(new BuoiHoc(i + 1, d, TrangThaiBuoi.tu(theoNgay.get(d), daQua)));
        }

        String ghiChu = null;
        if (lich.isEmpty()) ghiChu = "Lớp chưa có lịch học hiệu lực nên chưa tính được ngày học.";
        else if (tong <= 0) ghiChu = "Khóa học chưa khai báo tổng số buổi.";
        else if (ngay.size() < tong) {
            ghiChu = "Lịch học hiện chỉ đủ " + ngay.size() + "/" + tong
                    + " buổi (lịch có ngày kết thúc), chưa xác định được ngày kết thúc khóa.";
        }

        LocalDate tuyChinh = TOT_NGHIEP_TUY_CHINH.get(new KhoaHV(h.maHocVien(), l.maLop()));
        return new KhoaTienDo(l.maLop(), l.tenLop(), chuoi(l.tenKhoaHoc()), chuoi(l.tenMonHoc()), tong,
                moTaLich(lich), nhapHoc, ketThuc, tuyChinh != null ? tuyChinh : ketThuc, tuyChinh != null,
                daHoc, coDiemDanh, buoi, vang, ghiChu, false);
    }

    /** Các ngày học theo lịch kể từ ngày tu, tối đa tong buổi. */
    private static List<LocalDate> layNgayHoc(List<LichHocLop> lich, int tong, LocalDate tu) {
        List<LocalDate> ngay = new ArrayList<>();
        if (tong <= 0 || lich.isEmpty()) return ngay;
        LocalDate gioiHan = tu.plusYears(SO_NAM_TOI_DA);
        for (LocalDate d = tu; ngay.size() < tong && !d.isAfter(gioiHan); d = d.plusDays(1)) {
            if (coBuoiHoc(lich, d)) ngay.add(d);
        }
        return ngay;
    }

    private static boolean coBuoiHoc(List<LichHocLop> lich, LocalDate ngay) {
        for (LichHocLop x : lich) {
            if (doiThu(x.thuTrongTuan()) != ngay.getDayOfWeek()) continue;
            if (x.tuNgay() != null && ngay.isBefore(x.tuNgay())) continue;
            if (x.denNgay() != null && ngay.isAfter(x.denNgay())) continue;
            return true;
        }
        return false;
    }

    /**
     * Đổi mã thứ trong CSDL sang DayOfWeek. Quy ước đang dùng: 2 = Thứ Hai ... 7 = Thứ Bảy,
     * 8 hoặc 1 = Chủ nhật. Nếu CSDL dùng quy ước khác, chỉ cần sửa hàm này.
     */
    private static DayOfWeek doiThu(int thu) {
        if (thu == 1 || thu == 8) return DayOfWeek.SUNDAY;
        return thu >= 2 && thu <= 7 ? DayOfWeek.of(thu - 1) : null;
    }

    private static String moTaLich(List<LichHocLop> lich) {
        return lich.stream().sorted(Comparator.comparingInt(LichHocLop::thuTrongTuan)).map(x -> {
            DayOfWeek d = doiThu(x.thuTrongTuan());
            String gio = x.gioBatDau() == null ? ""
                    : " " + GIO.format(x.gioBatDau()) + (x.gioKetThuc() == null ? "" : "–" + GIO.format(x.gioKetThuc()));
            return (d == null ? "Thứ " + x.thuTrongTuan() : tenThu(d)) + gio;
        }).collect(Collectors.joining("; "));
    }

    // ================= Nguồn lớp Mầm non =================

    /** Mọi dòng xếp lớp Mầm non còn hiệu lực của cơ sở (đọc qua MamNonService). */
    private List<XepLopMamNon> xepLopMamNonHieuLuc() throws SQLException {
        LocalDate homNay = LocalDate.now();
        List<XepLopMamNon> ds = new ArrayList<>();
        for (XepLopMamNon x : mamNon.xepLop(null)) {
            if (HIEU_LUC.equals(x.trangThai()) && (x.denNgay() == null || !x.denNgay().isBefore(homNay))) ds.add(x);
        }
        return ds;
    }

    private List<XepLopMamNon> xepLopMamNonCuaHocVien(int maHocVien) throws SQLException {
        List<XepLopMamNon> dong = new ArrayList<>();
        for (XepLopMamNon x : xepLopMamNonHieuLuc()) {
            if (x.maHocVien() == maHocVien) dong.add(x);
        }
        dong.sort(Comparator.comparing(XepLopMamNon::tuNgay, Comparator.nullsLast(Comparator.reverseOrder())));
        return dong;
    }

    private int maCoSo() throws SQLException {
        Integer ma = maCoSoCache;
        if (ma == null) {
            ma = coSoRepo.timMaCoSo(coSo);
            maCoSoCache = ma;
        }
        return ma;
    }

    // ================= Tiện ích tĩnh (View dùng để hiển thị) =================

    public static String tenGioiTinh(String ma) {
        return ma == null || ma.isEmpty() ? "" : GT_TEN[chiSo(GT_MA, ma)];
    }

    public static String tenTrangThai(String ma) {
        return TT_TEN[chiSo(TT_MA, ma)];
    }

    public static int chiSoGioiTinh(String ma) {
        return chiSo(GT_MA, ma);
    }

    public static int chiSoTrangThai(String ma) {
        return chiSo(TT_MA, ma);
    }

    /** T2..T7, CN. */
    public static String tenThu(DayOfWeek d) {
        return d == DayOfWeek.SUNDAY ? "CN" : "T" + (d.getValue() + 1);
    }

    /** dd/MM/yyyy, hoặc "—" nếu chưa có. */
    public static String ngay(LocalDate d) {
        return d == null ? "—" : NGAY_VN.format(d);
    }

    /** dd/MM/yyyy, hoặc chuỗi rỗng nếu chưa có (dùng cho ô nhập). */
    public static String ngayNhap(LocalDate d) {
        return d == null ? "" : NGAY_VN.format(d);
    }

    // ================= Nội bộ =================

    private <T> void nen(String dangLam, Callable<T> viec, Consumer<T> khiXong) {
        view.chayNen(dangLam, viec, khiXong, this::baoLoi);
    }

    private void baoLoi(Throwable ex) {
        view.hienLoi(loiGoc(ex));
    }

    private boolean coChon(HocVien h) {
        if (h == null) view.hienCanhBao(CHUA_CHON);
        return h != null;
    }

    private static String loiGoc(Throwable ex) {
        Throwable goc = ex;
        while (goc.getCause() != null && goc.getCause() != goc) goc = goc.getCause();
        return goc.getMessage() == null ? "lỗi không xác định" : goc.getMessage();
    }

    private static boolean coSoDienThoai(HocVien h) {
        return h.soDienThoaiLienHe() != null && !h.soDienThoaiLienHe().isBlank();
    }

    private static int chiSo(String[] mang, String giaTri) {
        if (giaTri == null) return 0;
        for (int i = 0; i < mang.length; i++) {
            if (mang[i].equals(giaTri)) return i;
        }
        return 0;
    }

    private static String rong(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String chuoi(String s) {
        return s == null ? "" : s;
    }
    
}