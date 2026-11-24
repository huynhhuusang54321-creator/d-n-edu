package Tv1.Controller;

import Tv1.View.HeThongPage;
import Model.*;
import Service.*;
import java.awt.event.HierarchyEvent;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Function;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** Controller TV1: đọc 12 Service, ánh xạ cột, lọc và điều khiển View. */
public class HeThongController {

    public static final String MENU_KEY = "HE_THONG";
    private final HeThongPage view;
    private final int maCoSo;
    private final Map<String, TableSpec> specs = new LinkedHashMap<>();
    private final Map<String, Result> results = new LinkedHashMap<>();
    private SwingWorker<Void, Result> worker;
    private boolean wasDisplayable;
    private boolean closed;

    private record TableSpec(String key, String[] columns,
                             Function<Boolean, List<Object[]>> loader) { }
    private record Result(String key, List<Object[]> rows, String error) { }

    // MainPageController khởi tạo đúng constructor này trên EDT.
    public HeThongController(HeThongPage view, int maCoSo) {
        this.view = Objects.requireNonNull(view);
        this.maCoSo = maCoSo;
        if (SwingUtilities.isEventDispatchThread()) init();
        else SwingUtilities.invokeLater(this::init);
    }

    public int getMaCoSo() { return maCoSo; }

    private void init() {
        view.setTitleText("QUẢN LÝ HỆ THỐNG");
        view.setContextText("TV1  |  Mã cơ sở: " + maCoSo
                + "  |  Thông tin và dữ liệu cơ sở");
        view.setActionLabels("Tải lại bảng", "Tải lại tất cả");
        view.setNoteText("Dữ liệu lấy qua Service; MatKhauBam được che. "
                + "Danh sách rỗng có thể do lỗi SQL.");
        registerTables();
        for (TableSpec spec : specs.values()) {
            view.addDataTable(spec.key(), spec.key(), spec.columns());
        }
        view.onReload(() -> {
            String key = view.getSelectedTableKey();
            if (key != null) load(List.of(key), true);
        });
        view.onReloadAll(() -> load(new ArrayList<>(specs.keySet()), true));
        view.onTableChanged(this::renderSelected);
        view.onBranchReload(() -> load(List.of("CoSo"), true));
        view.onCatalogReload(() -> load(List.of("DanhMuc"), true));
        view.onCatalogChanged(this::renderCatalog);
        view.onCatalogDetail(this::showCatalogDetail);
        view.onSearchChanged(this::renderSelected);
        // Khi trang bị tháo khỏi cửa sổ, ngừng tải các bảng còn lại.
        view.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                if (view.isDisplayable()) wasDisplayable = true;
                else if (wasDisplayable) {
                    closed = true;
                    if (worker != null) worker.cancel(true);
                }
            }
        });
        wasDisplayable = view.isDisplayable();
        if (maCoSo <= 0) {
            view.setReloadEnabled(false);
            view.setStatusText("Mã cơ sở không hợp lệ.");
            return;
        }
        load(new ArrayList<>(specs.keySet()), false);
    }

    // ==================== ĐỊNH NGHĨA 12 BẢNG ====================
    // Tên và thứ tự cột khớp COLUMNS trong Repository hiện tại.
    // Không dùng reflection, không gọi Repository hoặc SQL trực tiếp.
    private void registerTables() {
        register("CoSo", new String[] {
                        "MaCoSo", "MaCoSoHienThi", "TenCoSo", "LoaiCoSo", "DiaChi",
                        "SoDienThoai", "Logo", "TrangThai" },
                refresh -> (refresh ? new CoSoService().reload() : new CoSoService().selectAll()).stream()
                        .filter(x -> x.getMaCoSo() == maCoSo).toList(),
                x -> new Object[] {
                        x.getMaCoSo(), x.getMaCoSoHienThi(), x.getTenCoSo(), x.getLoaiCoSo(),
                        x.getDiaChi(), x.getSoDienThoai(), x.getLogo(), x.getTrangThai() });

        register("NhanSu", new String[] {
                        "MaNhanSu", "MaCoSo", "HoTen", "NgaySinh", "GioiTinh",
                        "SoDienThoai", "Email", "DiaChi", "AnhDaiDien", "TenDangNhap",
                        "MatKhauBam", "VaiTro", "ChoPhepDangNhap", "TrangThai", "NgayTao" },
                refresh -> (refresh ? new NhanSuService().reload(maCoSo) : new NhanSuService().selectAll(maCoSo)),
                x -> new Object[] {
                        x.getMaNhanSu(), x.getMaCoSo(), x.getHoTen(), x.getNgaySinh(),
                        x.getGioiTinh(), x.getSoDienThoai(), x.getEmail(), x.getDiaChi(),
                        x.getAnhDaiDien(), x.getTenDangNhap(), "********", x.getVaiTro(),
                        x.getChoPhepDangNhap(), x.getTrangThai(), x.getNgayTao() });

        register("DanhMuc", new String[] {
                        "MaDanhMuc", "MaCoSo", "NhomDanhMuc", "MaKyHieu", "TenDanhMuc",
                        "MaCha", "ThuTu", "CauHinh", "TrangThai" },
                refresh -> (refresh ? new DanhMucService().reload(maCoSo) : new DanhMucService().selectAll(maCoSo)),
                x -> new Object[] {
                        x.getMaDanhMuc(), x.getMaCoSo(), x.getNhomDanhMuc(), x.getMaKyHieu(),
                        x.getTenDanhMuc(), x.getMaCha(), x.getThuTu(), x.getCauHinh(),
                        x.getTrangThai() });

        register("HocVien", new String[] {
                        "MaHocVien", "MaCoSo", "MaHoSo", "HoTen", "NgaySinh",
                        "GioiTinh", "AnhDaiDien", "TenPhuHuynh", "SoDienThoai", "Email",
                        "DiaChi", "MaNguonTiepNhan", "MaNguoiTuVan", "MaNguoiTao", "NgayTao",
                        "NgayChinhThuc", "TrangThai", "GhiChu" },
                refresh -> (refresh ? new HocVienService().reload(maCoSo) : new HocVienService().selectAll(maCoSo)),
                x -> new Object[] {
                        x.getMaHocVien(), x.getMaCoSo(), x.getMaHoSo(), x.getHoTen(),
                        x.getNgaySinh(), x.getGioiTinh(), x.getAnhDaiDien(), x.getTenPhuHuynh(),
                        x.getSoDienThoai(), x.getEmail(), x.getDiaChi(), x.getMaNguonTiepNhan(),
                        x.getMaNguoiTuVan(), x.getMaNguoiTao(), x.getNgayTao(), x.getNgayChinhThuc(),
                        x.getTrangThai(), x.getGhiChu() });

        register("TuongTacHocVien", new String[] {
                        "MaTuongTac", "MaCoSo", "MaHocVien", "MaNhanSu", "LoaiTuongTac",
                        "MaKhoaHocMongMuon", "ThoiGianMongMuon", "NoiDung", "KetQua", "TrangThaiTruoc",
                        "TrangThaiSau", "NgayHenTiep", "NgayTao" },
                refresh -> (refresh ? new TuongTacHocVienService().reload(maCoSo) : new TuongTacHocVienService().selectAll(maCoSo)),
                x -> new Object[] {
                        x.getMaTuongTac(), x.getMaCoSo(), x.getMaHocVien(), x.getMaNhanSu(),
                        x.getLoaiTuongTac(), x.getMaKhoaHocMongMuon(), x.getThoiGianMongMuon(), x.getNoiDung(),
                        x.getKetQua(), x.getTrangThaiTruoc(), x.getTrangThaiSau(), x.getNgayHenTiep(),
                        x.getNgayTao() });

        register("LopHoc", new String[] {
                        "MaLop", "MaCoSo", "MaLopHienThi", "TenLop", "MaKhoaHoc",
                        "MaNamHoc", "MaGiaoVien", "SiSoToiDa", "NgayBatDau", "NgayKetThuc",
                        "HocPhiMacDinh", "TienAnMacDinh", "TrangThai", "GhiChu" },
                refresh -> (refresh ? new LopHocService().reload(maCoSo) : new LopHocService().selectAll(maCoSo)),
                x -> new Object[] {
                        x.getMaLop(), x.getMaCoSo(), x.getMaLopHienThi(), x.getTenLop(),
                        x.getMaKhoaHoc(), x.getMaNamHoc(), x.getMaGiaoVien(), x.getSiSoToiDa(),
                        x.getNgayBatDau(), x.getNgayKetThuc(), x.getHocPhiMacDinh(), x.getTienAnMacDinh(),
                        x.getTrangThai(), x.getGhiChu() });

        register("DangKyHoc", new String[] {
                        "MaDangKy", "MaCoSo", "MaHocVien", "MaKhoaHoc", "MaLop",
                        "MaDangKyGoc", "MaDangKyTruoc", "NgayDangKy", "NgayBatDau", "NgayKetThucDuKien",
                        "NgayKetThucThucTe", "TongSoBuoi", "HocPhiThoaThuan", "SoTienGiam", "MaNguoiDangKy",
                        "TrangThai", "GhiChu" },
                refresh -> (refresh ? new DangKyHocService().reload(maCoSo) : new DangKyHocService().selectAll(maCoSo)),
                x -> new Object[] {
                        x.getMaDangKy(), x.getMaCoSo(), x.getMaHocVien(), x.getMaKhoaHoc(),
                        x.getMaLop(), x.getMaDangKyGoc(), x.getMaDangKyTruoc(), x.getNgayDangKy(),
                        x.getNgayBatDau(), x.getNgayKetThucDuKien(), x.getNgayKetThucThucTe(), x.getTongSoBuoi(),
                        x.getHocPhiThoaThuan(), x.getSoTienGiam(), x.getMaNguoiDangKy(), x.getTrangThai(),
                        x.getGhiChu() });

        register("ThoiKhoaBieu", new String[] {
                        "MaThoiKhoaBieu", "MaCoSo", "MaLop", "ThuTrongTuan", "MaCaHoc",
                        "TuNgay", "DenNgay", "TrangThai" },
                refresh -> (refresh ? new ThoiKhoaBieuService().reload(maCoSo) : new ThoiKhoaBieuService().selectAll(maCoSo)),
                x -> new Object[] {
                        x.getMaThoiKhoaBieu(), x.getMaCoSo(), x.getMaLop(), x.getThuTrongTuan(),
                        x.getMaCaHoc(), x.getTuNgay(), x.getDenNgay(), x.getTrangThai() });

        register("BuoiHoc", new String[] {
                        "MaBuoiHoc", "MaCoSo", "MaLop", "MaThoiKhoaBieu", "NgayHoc",
                        "MaCaHoc", "GioBatDau", "GioKetThuc", "MaGiaoVien", "MaNguoiKichHoat",
                        "TrangThai", "NgayKichHoat", "GhiChu" },
                refresh -> (refresh ? new BuoiHocService().reload(maCoSo) : new BuoiHocService().selectAll(maCoSo)),
                x -> new Object[] {
                        x.getMaBuoiHoc(), x.getMaCoSo(), x.getMaLop(), x.getMaThoiKhoaBieu(),
                        x.getNgayHoc(), x.getMaCaHoc(), x.getGioBatDau(), x.getGioKetThuc(),
                        x.getMaGiaoVien(), x.getMaNguoiKichHoat(), x.getTrangThai(), x.getNgayKichHoat(),
                        x.getGhiChu() });

        register("DiemDanh", new String[] {
                        "MaDiemDanh", "MaCoSo", "MaHocVien", "MaBuoiHoc", "MaDangKy",
                        "LoaiThamGia", "TrangThai", "TinhBuoi", "MaDiemDanhNguon", "MaNguoiCapNhat",
                        "NgayCapNhat", "GhiChu" },
                refresh -> (refresh ? new DiemDanhService().reload(maCoSo) : new DiemDanhService().selectAll(maCoSo)),
                x -> new Object[] {
                        x.getMaDiemDanh(), x.getMaCoSo(), x.getMaHocVien(), x.getMaBuoiHoc(),
                        x.getMaDangKy(), x.getLoaiThamGia(), x.getTrangThai(), x.getTinhBuoi(),
                        x.getMaDiemDanhNguon(), x.getMaNguoiCapNhat(), x.getNgayCapNhat(), x.getGhiChu() });

        register("GiaoDichTaiChinh", new String[] {
                        "MaGiaoDich", "MaCoSo", "MaHocVien", "MaDangKy", "MaGiaoDichNguon",
                        "MaNhomChungTu", "LoaiGiaoDich", "MaLoaiPhi", "MaHinhThuc", "KyPhi",
                        "SoLuong", "DonGia", "SoTien", "NgayGiaoDich", "HanThanhToan",
                        "MaNhanSu", "TrangThai", "DienGiai" },
                refresh -> (refresh ? new GiaoDichTaiChinhService().reload(maCoSo) : new GiaoDichTaiChinhService().selectAll(maCoSo)),
                x -> new Object[] {
                        x.getMaGiaoDich(), x.getMaCoSo(), x.getMaHocVien(), x.getMaDangKy(),
                        x.getMaGiaoDichNguon(), x.getMaNhomChungTu(), x.getLoaiGiaoDich(), x.getMaLoaiPhi(),
                        x.getMaHinhThuc(), x.getKyPhi(), x.getSoLuong(), x.getDonGia(),
                        x.getSoTien(), x.getNgayGiaoDich(), x.getHanThanhToan(), x.getMaNhanSu(),
                        x.getTrangThai(), x.getDienGiai() });

        register("GiaoDichKho", new String[] {
                        "MaGiaoDichKho", "MaCoSo", "MaNhomChungTu", "MaHangHoa", "MaHocVien",
                        "MaGiaoDichTaiChinh", "LoaiGiaoDich", "SoLuong", "TonThucTe", "DonGia",
                        "NgayGiaoDich", "MaNhanSu", "TrangThai", "GhiChu" },
                refresh -> (refresh ? new GiaoDichKhoService().reload(maCoSo) : new GiaoDichKhoService().selectAll(maCoSo)),
                x -> new Object[] {
                        x.getMaGiaoDichKho(), x.getMaCoSo(), x.getMaNhomChungTu(), x.getMaHangHoa(),
                        x.getMaHocVien(), x.getMaGiaoDichTaiChinh(), x.getLoaiGiaoDich(), x.getSoLuong(),
                        x.getTonThucTe(), x.getDonGia(), x.getNgayGiaoDich(), x.getMaNhanSu(),
                        x.getTrangThai(), x.getGhiChu() });
    }

    private <T> void register(String key, String[] columns,
                              Function<Boolean, List<T>> supplier,
                              Function<T, Object[]> mapper) {
        specs.put(key, new TableSpec(key, columns, refresh -> {
            List<T> data = Objects.requireNonNull(supplier.apply(refresh), "Service trả về null");
            List<Object[]> rows = new ArrayList<>(data.size());
            for (T item : data) {
                Object[] row = mapper.apply(item);
                if (row.length != columns.length) {
                    throw new IllegalStateException("Sai số cột: " + key);
                }
                // Bảo vệ lớp hiển thị nếu Service trả nhầm cơ sở.
                int branchColumn = key.equals("CoSo") ? 0 : 1;
                if (!(row[branchColumn] instanceof Number n) || n.intValue() != maCoSo) continue;
                rows.add(row);
            }
            return rows;
        }));
    }

    // ==================== TẢI DỮ LIỆU NỀN ====================

    private void load(List<String> keys, boolean refresh) {
        if (closed || maCoSo <= 0 || worker != null) return;
        view.setReloadEnabled(false);
        for (String key : keys) {
            results.remove(key);
            if (key.equals("CoSo")) view.showBranch(null, "Đang tải...");
            if (key.equals("DanhMuc")) { view.setCatalogRows(List.of()); view.setCatalogStatus("Đang tải..."); }
            view.setRows(key, Collections.emptyList());
            view.setTabTitle(key, key + " (...)");
        }
        view.setStatusText("Đang tải " + keys.size() + " bảng từ Service...");
        view.setCountText("");
        worker = new SwingWorker<Void, Result>() {
            @Override protected Void doInBackground() {
                for (String key : keys) {
                    if (isCancelled()) break;
                    try {
                        List<Object[]> rows = specs.get(key).loader().apply(refresh);
                        publish(new Result(key, rows, null));
                    } catch (RuntimeException ex) {
                        publish(new Result(key, Collections.emptyList(),
                                ex.getClass().getSimpleName()));
                    }
                }
                return null;
            }

            @Override protected void process(List<Result> chunks) {
                if (closed || isCancelled()) return;
                for (Result result : chunks) {
                    results.put(result.key(), result);
                    view.setTabTitle(result.key(), result.key() + " ("
                            + (result.error() == null ? result.rows().size() : "Lỗi") + ")");
                    view.setRows(result.key(), result.rows());
                    if (result.key().equals("CoSo")) view.showBranch(result.rows().isEmpty() ? null : result.rows().get(0), result.error() == null ? "Không có dữ liệu cơ sở; kiểm tra kết nối nếu bất thường." : "Lỗi tải cơ sở.");
                    if (result.key().equals("DanhMuc")) {
                        view.setCatalogGroups(result.rows().stream().map(r -> String.valueOf(r[2])).distinct().sorted().toList());
                        renderCatalog();
                    }
                }
                renderSelected();
            }

            @Override protected void done() {
                try {
                    get();
                } catch (CancellationException ignored) {
                    // Trang đã đóng.
                } catch (Exception ex) {
                    if (!closed) view.setNoteText("Có lỗi khi thực hiện tải dữ liệu.");
                } finally {
                    worker = null;
                    if (!closed) {
                        view.setReloadEnabled(true);
                        renderSelected();
                    }
                }
            }
        };
        worker.execute();
    }

    // ==================== TÌM KIẾM / HIỂN THỊ ====================

    private void renderSelected() {
        String key = view.getSelectedTableKey();
        if (key == null || closed) return;
        Result result = results.get(key);
        if (result == null) {
            view.setCountText("");
            view.setStatusText(key + (worker != null ? " — Đang tải..." : " — Chưa tải."));
            return;
        }
        String query = normalize(view.getSearchText());
        List<Object[]> visible = new ArrayList<>();
        for (Object[] row : result.rows()) {
            if (matches(row, query)) visible.add(row);
        }
        view.setRows(key, visible);
        view.setCountText("Hiển thị " + visible.size() + " / " + result.rows().size() + " dòng");
        String message;
        if (result.error() != null) {
            message = key + " — Lỗi tải dữ liệu (" + result.error() + ").";
        } else if (result.rows().isEmpty()) {
            message = key + " — Service trả về 0 dòng; kiểm tra SQL nếu bất thường.";
        } else {
            message = key + " — Đã nhận dữ liệu từ Service.";
        }
        if (worker != null) message += "  |  Đang tải nền...";
        view.setStatusText(message);
    }

    private void renderCatalog() {
        Result result = results.get("DanhMuc");
        if (result == null || closed) return;
        String group = view.getCatalogGroup();
        int status = view.getCatalogStatusFilter();
        String query = normalize(view.getCatalogSearch());
        List<Object[]> rows = new ArrayList<>();
        for (Object[] r : result.rows()) {
            if (group != null && !group.equals(r[2])) continue;
            if (status == 1 && !Boolean.TRUE.equals(r[8])) continue;
            if (status == 2 && Boolean.TRUE.equals(r[8])) continue;
            if (!matches(new Object[]{r[0],r[2],r[3],r[4]}, query)) continue;
            String parent = "—";
            if (r[5] != null) {
                parent = String.valueOf(r[5]);
                for (Object[] p : result.rows()) if (Objects.equals(p[0], r[5])) { parent += " - " + p[4]; break; }
            }
            rows.add(new Object[]{r[0],r[3],r[4],r[2],parent,r[6],Boolean.TRUE.equals(r[8]) ? "Hoạt động" : "Ngừng hoạt động"});
        }
        view.setCatalogRows(rows);
        view.setCatalogStatus(result.error() != null ? "Lỗi tải danh mục." : result.rows().isEmpty() ? "Service trả về 0 dòng; kiểm tra kết nối nếu bất thường." : "Hiển thị " + rows.size() + " / " + result.rows().size() + " danh mục");
    }

    private void showCatalogDetail() {
        Object id = view.getSelectedCatalogId();
        Result result = results.get("DanhMuc");
        if (id == null || result == null || closed) return;
        for (Object[] r : result.rows()) if (Objects.equals(r[0], id)) { view.showCatalogDetail(r); return; }
    }

    private boolean matches(Object[] row, String query) {
        if (query.isEmpty()) return true;
        for (Object value : row) {
            if (value != null && normalize(String.valueOf(value)).contains(query)) return true;
        }
        return false;
    }

    private String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }
}
