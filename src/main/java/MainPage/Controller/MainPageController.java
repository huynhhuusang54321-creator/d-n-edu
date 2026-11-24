package MainPage.Controller;

import MainPage.View.MainPage;

import Model.CoSoModel;
import Service.CoSoService;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Writer;

import java.io.IOException;
import java.io.Reader;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import java.util.*;
import java.util.function.Predicate;

import javax.swing.*;

public class MainPageController {

    // ==================== DOI TUONG ====================

    private final MainPage view;

    private final CoSoService coSoService =
            new CoSoService();

    // Một đường dẫn và một khóa chung cho mọi Controller trong JVM.
    private static final Path KEY_FILE = resolveKeyFile();
    private static final Object KEY_LOCK = new Object();
    private static final String BADGE_PREFIX = "badge.";
    private static final String BADGE_SECTION = "CACHE BADGE";
    private static final int BADGE_REFRESH_MS = 1000;
    private static final int BADGE_DISPLAY_MAX = 99;
    private final Path keyFile;

    private javax.swing.Timer badgeTimer;
    private boolean badgeReading;
    private boolean badgeReadFailed;
    private long menuVersion;

    private final Predicate<String> permission;

    private final Map<Integer, CoSoModel> branches =
            new LinkedHashMap<>();

    private final Map<String, MenuConfig> visibleMenus =
            new LinkedHashMap<>();

    private CoSoModel currentCoSo;

    private String activeKey;

    private boolean loading = false;
    private boolean initialized = false;

    // ====================================================
    // 01. CAU HINH DANH SACH NUT
    // ====================================================

    // Key | Tên hiển thị | Icon | Class | Loại cơ sở

    private static final String[][] DEFAULT_MENUS = {

        {
            "TRANG_CHU",
            "Trang chủ",
            "TRANG_CHU",
            "MainPage.View.TrangChuPage",
            "ALL"
        },
        {
            "HOC_VIEN",
            "Học viên",
            "HOC_VIEN",
            "Tv2.View.HocVienPage",
            "ALL"
        },
        {
            "TUYEN_SINH",
            "Tuyển sinh",
            "TUYEN_SINH",
            "Tv2.View.TuyenSinhPage",
            "ALL"
        },
        {
            "KHOA_HOC",
            "Khóa học",
            "KHOA_HOC",
            "Tv3.View.KhoaHocPage",
            "ALL"
        },
        {
            "THOI_KHOA_BIEU",
            "Thời khóa biểu",
            "THOI_KHOA_BIEU",
            "Tv4.View.ThoiKhoaBieuPage",
            "ALL"
        },
        {
            "LOP_HOC",
            "Lớp",
            "LOP_HOC",
            "Tv3.View.LopHocPage",
            "ALL"
        },
        {
            "DIEM_DANH",
            "Điểm danh",
            "DIEM_DANH",
            "Tv4.View.DiemDanhPage",
            "ALL"
        },
        {
            "CHI_THU",
            "Chi thu",
            "CHI_THU",
            "Tv5.View.ChiThuPage",
            "ALL"
        },
        {
            "KHO",
            "Kho",
            "KHO",
            "Tv5.View.KhoPage",
            "ALL"
        },
        {
            "CHUONG_TRINH",
            "Chương trình",
            "CHUONG_TRINH",
            "Tv6.View.ChuongTrinhPage",
            "ALL"
        },
        {
            "NHAN_SU",
            "Nhân sự",
            "NHAN_SU",
            "Tv1.View.NhanSuPage",
            "ALL"
        },
        {
            "BAO_CAO",
            "Báo cáo",
            "BAO_CAO",
            "Tv6.View.BaoCaoPage",
            "ALL"
        },
        {
            "HE_THONG",
            "Hệ thống",
            "HE_THONG",
            "Tv1.View.HeThongPage",
            "ALL"
        }
    };

    // ====================================================
    // 02. CAU HINH GIAO DIEN
    // Chỉnh khu vực này để thay đổi MainPage.
    // ====================================================

    public void configureUI() {

        MainPage.UIConfig ui =
                new MainPage.UIConfig();

        // ---------- Thông tin ứng dụng ----------

        ui.windowTitle =
                "VINABACUS - Hệ thống quản lý học viên";

        ui.brandName = "vinabacus";
        ui.brandSubtitle = "CẦN THƠ";

        ui.logoPath = "/images/logo.png";

        ui.branchLabel = "Cơ sở:";

        // ---------- Màu sắc ----------

        ui.primaryColor =
                Color.decode("#05532B");

        ui.headerStartColor =
                Color.decode("#43A900");

        ui.headerEndColor =
                Color.decode("#05532B");

        ui.activeColor =
                Color.decode("#FFDE00");

        ui.activeTextColor =
                Color.decode("#05532B");

        ui.menuTextColor = Color.WHITE;

        ui.backgroundColor =
                Color.decode("#EEEEEE");

        ui.pageColor =
                Color.decode("#EEEEEE");

        ui.statusColor =
                Color.decode("#555555");

        // Badge: toàn bộ thông số hiển thị do Controller thiết lập.
        ui.badgeColor = Color.decode("#D32F2F");
        ui.badgeTextColor = Color.WHITE;
        ui.badgeFontSize = 12;
        ui.badgeHeight = 22;
        ui.badgePadding = 6;
        ui.badgeRightGap = 8;
        ui.badgeTextGap = 6;
        ui.showBadges = true;

        // ---------- Kích thước cửa sổ ----------

        ui.windowWidth = 1440;
        ui.windowHeight = 900;

        ui.maximized = true;

        // ---------- Kích thước giao diện ----------

        ui.headerHeight = 115;
        ui.sidebarWidth = 250;
        ui.logoWidth = 275;

        ui.menuHeight = 45;

        ui.iconSize = 23;
        ui.menuFontSize = 15;

        ui.comboWidth = 220;
        ui.comboHeight = 40;

        ui.contentPadding = 16;

        // ---------- Font ----------

        ui.fontName = "Segoe UI";

        // ---------- Thành phần hiển thị ----------

        ui.showHeader = true;
        ui.showSidebar = true;
        ui.showStatus = true;

        // Gửi toàn bộ thiết lập sang View.

        view.applyConfig(ui);
    }

    // ====================================================
    // 03. CONSTRUCTOR
    // ====================================================

    public MainPageController(MainPage view) {

        // Chế độ phát triển:
        // Chưa áp dụng quyền theo tài khoản.

        this(view, key -> true);
    }

    public MainPageController(
            MainPage view,
            Predicate<String> permission) {

        this.view = Objects.requireNonNull(view);

        this.permission =
                Objects.requireNonNull(permission);

        this.keyFile = KEY_FILE;
    }

    // ====================================================
    // 04. KHOI TAO
    // ====================================================

    public void init() {

        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(this::init);
            return;
        }

        if (initialized) return;

        initialized = true;

        configureUI();

        bindEvents();

        loadCoSo();
        startBadgeTimer();
    }

    private void bindEvents() {

        view.onMenuClick(e -> {

            String key = e.getActionCommand();

            openPage(key);
        });

        view.onCoSoChanged(e -> {

            if (!loading) {
                changeCoSo();
            }
        });
    }

    // ====================================================
    // 05. QUAN LY KEY.TXT
    // ====================================================

    private static Path resolveKeyFile() {

        // Cho phép chỉ định đường dẫn riêng khi chạy JAR.

        String override =
                System.getProperty("vinabacus.keyFile");

        if (override != null
                && !override.trim().isEmpty()) {

            return Paths.get(override)
                    .toAbsolutePath()
                    .normalize();
        }

        Path root = Paths.get("")
                .toAbsolutePath();

        // Maven

        Path maven = root.resolve(
                "src/main/java/MainPage/Controller");

        if (Files.isDirectory(maven)) {

            return maven.resolve("Key.txt");
        }

        // Ant

        Path ant = root.resolve(
                "src/MainPage/Controller");

        if (Files.isDirectory(ant)) {

            return ant.resolve("Key.txt");
        }

        // Khi đóng gói thành JAR.

        return Paths.get(
                System.getProperty("user.home"),
                ".vinabacus",
                "MainPage",
                "Controller",
                "Key.txt"
        );
    }

    public Path getKeyFile() {
        return keyFile;
    }

    // Các phương thức ghi/đọc đều dùng chung khóa KEY_LOCK.
    // Không gọi SQL và không tạo Controller khác chỉ để ghi badge.
    private static void ensureKeyFile() throws IOException {
        synchronized (KEY_LOCK) {
            if (Files.exists(KEY_FILE)) {
                // Nâng cấp Key.txt cũ mà không xóa cấu hình đã có.
                if (!Files.readString(KEY_FILE, StandardCharsets.UTF_8)
                        .contains("#" + BADGE_SECTION)) {
                    writeConfigLocked(loadConfigLocked());
                }
                return;
            }
            Properties defaults = new Properties();
            int order = 10;
            for (String[] menu : DEFAULT_MENUS) {
                String key = menu[0];
                defaults.setProperty(key, menu[3]);
                defaults.setProperty(key + ".title", menu[1]);
                defaults.setProperty(key + ".icon", menu[2]);
                defaults.setProperty(key + ".visible", "true");
                defaults.setProperty(key + ".branches", menu[4]);
                defaults.setProperty(key + ".order", String.valueOf(order));
                order += 10;
            }
            writeConfigLocked(defaults);
        }
    }

    private static Properties loadConfigLocked() throws IOException {
        Properties config = new Properties();
        try (Reader reader = Files.newBufferedReader(KEY_FILE,
                StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        return config;
    }

    private static Properties readConfig() throws IOException {
        synchronized (KEY_LOCK) {
            // Chỉ kiểm tra/nâng cấp vùng nhớ khi khởi tạo hoặc mất file.
            if (!Files.exists(KEY_FILE)) ensureKeyFile();
            return loadConfigLocked();
        }
    }

    private static void writeConfigLocked(Properties config) throws IOException {
        Properties routing = new Properties();
        Properties badges = new Properties();
        for (String key : config.stringPropertyNames()) {
            Properties target = key.startsWith(BADGE_PREFIX) ? badges : routing;
            target.setProperty(key, config.getProperty(key));
        }
        Files.createDirectories(KEY_FILE.getParent());
        // File tạm chỉ tồn tại lúc ghi; Key.txt là file lưu duy nhất.
        Path temp = Files.createTempFile(KEY_FILE.getParent(), "Key-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                routing.store(writer, "VINABACUS - CAU HINH MENU VA DIEU HUONG");
                writer.write(System.lineSeparator());
                badges.store(writer, BADGE_SECTION
                        + "\nbadge.<MaCoSo>.<MenuKey>.<Nguon>=SoLuong"
                        + "\nKhong co key / gia tri 0 = khong hien thi");
            }
            try {
                Files.move(temp, KEY_FILE, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, KEY_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    // ====================================================
    // 05A. API VUNG NHO DEM BADGE - CONTROLLER CON SU DUNG
    // ====================================================

    private static String badgeKey(int maCoSo, String menuKey, String source) {
        if (maCoSo <= 0) throw new IllegalArgumentException("MaCoSo phải > 0");
        if (menuKey == null || !menuKey.matches("[A-Z0-9_]+")) {
            throw new IllegalArgumentException("MenuKey không hợp lệ");
        }
        boolean known = false;
        for (String[] menu : DEFAULT_MENUS) {
            if (menu[0].equals(menuKey)) { known = true; break; }
        }
        if (!known) throw new IllegalArgumentException("MenuKey chưa đăng ký: " + menuKey);
        if (source == null || !source.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Nguồn badge không hợp lệ");
        }
        return BADGE_PREFIX + maCoSo + "." + menuKey + "." + source;
    }

    /** Ghi số lượng hiện tại của MỘT nguồn, không cộng dồn theo lần gọi. */
    public static void setBadgeCount(int maCoSo, String menuKey,
            String source, int count) throws IOException {
        String key = badgeKey(maCoSo, menuKey, source);
        if (count < 0) throw new IllegalArgumentException("Số lượng phải >= 0");
        synchronized (KEY_LOCK) {
            Properties config = readConfig();
            String old = config.getProperty(key);
            if (count == 0) {
                if (old == null) return;
                config.remove(key);
            } else {
                String value = Integer.toString(count);
                if (value.equals(old)) return;
                config.setProperty(key, value);
            }
            writeConfigLocked(config);
        }
    }

    /** Chỉ xóa đánh dấu của nguồn gọi, không ảnh hưởng nguồn khác. */
    public static void clearBadge(int maCoSo, String menuKey,
            String source) throws IOException {
        setBadgeCount(maCoSo, menuKey, source, 0);
    }

    /** Tổng các nguồn thuộc từng nút, chỉ trong cơ sở được chỉ định. */
    public static Map<String, Integer> readBadgeCounts(int maCoSo) throws IOException {
        if (maCoSo <= 0) return Collections.emptyMap();
        return aggregateBadges(readConfig(), maCoSo);
    }

    public static int getBadgeCount(int maCoSo, String menuKey) throws IOException {
        return readBadgeCounts(maCoSo).getOrDefault(menuKey, 0);
    }

    private static Map<String, Integer> aggregateBadges(Properties config, int maCoSo) {
        Map<String, Integer> counts = new HashMap<>();
        String prefix = BADGE_PREFIX + maCoSo + ".";
        for (String key : config.stringPropertyNames()) {
            if (!key.startsWith(prefix)) continue;
            String[] parts = key.substring(prefix.length()).split("\\.", -1);
            if (parts.length != 2 || !parts[0].matches("[A-Z0-9_]+")
                    || !parts[1].matches("[A-Za-z0-9_-]+")) continue;
            try {
                int count = Integer.parseInt(config.getProperty(key).trim());
                if (count > 0) {
                    counts.merge(parts[0], count, (a, b) ->
                            (int) Math.min(Integer.MAX_VALUE, (long) a + b));
                }
            } catch (NumberFormatException ignored) {
                // Một giá trị cache lỗi không làm hỏng menu.
            }
        }
        return counts;
    }

    // ====================================================
    // 05B. MAINPAGE DOC CACHE VA DIEU KHIEN HIEN THI
    // ====================================================

    private void startBadgeTimer() {
        badgeTimer = new javax.swing.Timer(BADGE_REFRESH_MS, e -> refreshBadges());
        badgeTimer.start();
        view.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                badgeTimer.stop();
            }
        });
    }

    /** Có thể yêu cầu đọc lại ngay; không truy vấn Database. */
    public void refreshBadges() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshBadges);
            return;
        }
        if (loading || badgeReading || currentCoSo == null) return;
        final int branchId = getMaCoSo();
        final long version = menuVersion;
        badgeReading = true;
        new SwingWorker<Map<String, Integer>, Void>() {
            @Override protected Map<String, Integer> doInBackground() throws Exception {
                return readBadgeCounts(branchId);
            }
            @Override protected void done() {
                try {
                    Map<String, Integer> counts = get();
                    if (!loading && getMaCoSo() == branchId && menuVersion == version) {
                        applyBadges(counts);
                    }
                    badgeReadFailed = false;
                } catch (Exception e) {
                    // Giữ badge cuối cùng khi file tạm thời không đọc được.
                    if (!badgeReadFailed) {
                        System.err.println("[MainPage badge] " + errorMessage(e));
                        badgeReadFailed = true;
                    }
                } finally {
                    badgeReading = false;
                }
            }
        }.execute();
    }

    private void applyBadges(Map<String, Integer> counts) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (String key : visibleMenus.keySet()) {
            int count = counts.getOrDefault(key, 0);
            if (count > 0) {
                labels.put(key, count > BADGE_DISPLAY_MAX
                        ? BADGE_DISPLAY_MAX + "+" : Integer.toString(count));
            }
        }
        // View chỉ nhận chuỗi hiển thị, không biết nguồn dữ liệu hay số tổng.
        view.setMenuBadges(labels);
    }

    // ====================================================
    // 06. DOC DU LIEU CO SO
    // ====================================================

    public void loadCoSo() {

        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(this::loadCoSo);
            return;
        }

        if (loading) return;

        loading = true;

        int previousId = getMaCoSo();

        currentCoSo = null;

        visibleMenus.clear();

        view.setNavigationEnabled(false);

        view.setMenuItems(
                Collections.emptyList());

        view.setCoSoItems(
                Collections.emptyList());

        view.setStatus("Đang tải dữ liệu cơ sở...");

        view.showPage(view.createEmptyPage(
                "VINABACUS",
                "Đang tải dữ liệu từ SQL Server..."
        ));

        SwingWorker<List<CoSoModel>, Void> worker =
                new SwingWorker<List<CoSoModel>, Void>() {

            @Override
            protected List<CoSoModel> doInBackground()
                    throws Exception {

                ensureKeyFile();

                return coSoService.selectAll();
            }

            @Override
            protected void done() {

                try {

                    List<CoSoModel> data =
                            new ArrayList<>(get());

                    data.sort(
                            Comparator.comparingInt(
                                    CoSoModel::getMaCoSo));

                    branches.clear();

                    List<MainPage.CoSoItem> items =
                            new ArrayList<>();

                    for (CoSoModel model : data) {

                        if (!model.isTrangThai()) {
                            continue;
                        }

                        branches.put(
                                model.getMaCoSo(),
                                model
                        );

                        items.add(
                                new MainPage.CoSoItem(
                                        model.getMaCoSo(),
                                        model.getTenCoSo() + " ["
                                                + model.getMaCoSoHienThi() + "]"
                                ));
                    }

                    view.setCoSoItems(items);

                    if (items.isEmpty()) {

                        showError(
                                "CHƯA CÓ CƠ SỞ",
                                "Không có cơ sở đang hoạt động."
                                + " Kiểm tra kết nối SQL Server."
                        );

                        return;
                    }

                    view.selectCoSo(previousId);

                    view.setNavigationEnabled(true);

                    changeCoSo();

                } catch (Exception e) {

                    showError(
                            "LỖI KHỞI TẠO",
                            errorMessage(e)
                    );

                } finally {

                    loading = false;
                }
            }
        };

        worker.execute();
    }

    public int getMaCoSo() {

        return currentCoSo == null
                ? 0
                : currentCoSo.getMaCoSo();
    }

    public CoSoModel getCurrentCoSo() {
        return currentCoSo;
    }

    // ====================================================
    // 07. THAY DOI CO SO
    // ====================================================

    private void changeCoSo() {

        MainPage.CoSoItem item =
                view.getSelectedCoSo();

        currentCoSo = item == null
                ? null
                : branches.get(item.maCoSo);

        try {

            rebuildMenu();

            if (visibleMenus.isEmpty()) {

                activeKey = null;

                view.showPage(view.createEmptyPage(
                        "VINABACUS",
                        "Không có chức năng khả dụng."
                ));

                view.setStatus(
                        "Không có chức năng được phép mở.");

                return;
            }

            String key;

            // Giữ trang hiện tại nếu cơ sở mới
            // vẫn được phép mở trang đó.

            if (visibleMenus.containsKey(activeKey)) {

                key = activeKey;

            } else if (
                    visibleMenus.containsKey("TRANG_CHU")) {

                key = "TRANG_CHU";

            } else {

                key = visibleMenus.keySet()
                        .iterator().next();
            }

            openPageInternal(key);

        } catch (Exception e) {

            showError(
                    "LỖI CẤU HÌNH",
                    errorMessage(e)
            );
        }
    }

    // ====================================================
    // 08. MODEL CAU HINH MENU
    // ====================================================

    private static class MenuConfig {

        String key;
        String title;
        String icon;
        String className;
        int order;

        MenuConfig(
                String key,
                String title,
                String icon,
                String className,
                int order) {

            this.key = key;
            this.title = title;
            this.icon = icon;
            this.className = className;
            this.order = order;
        }
    }

    // ====================================================
    // 09. LOC DANH SACH NUT
    // ====================================================

    private void rebuildMenu() throws IOException {

        menuVersion++;
        visibleMenus.clear();

        List<MainPage.MenuItem> viewItems =
                new ArrayList<>();

        if (currentCoSo == null) {

            view.setMenuItems(viewItems);
            return;
        }

        Properties config = readConfig();

        List<MenuConfig> allowed =
                new ArrayList<>();

        String loaiCoSo =
                currentCoSo.getLoaiCoSo();

        int defaultOrder = 10;

        for (String[] def : DEFAULT_MENUS) {

            String key = def[0];

            int order = readOrder(
                    config,
                    key,
                    defaultOrder
            );

            defaultOrder += 10;

            // Điều kiện 1: chức năng được bật.

            boolean visible = Boolean.parseBoolean(
                    config.getProperty(
                            key + ".visible",
                            "true"
                    ).trim()
            );

            if (!visible) continue;

            // Điều kiện 2: phù hợp loại cơ sở.

            String branchRule =
                    config.getProperty(
                            key + ".branches",
                            def[4]
                    );

            if (!matchesBranch(
                    branchRule,
                    loaiCoSo)) {

                continue;
            }

            // Điều kiện 3: quyền người dùng.

            if (!permission.test(key)) {
                continue;
            }

            // Controller quyết định tên và icon mặc định.
            // Key.txt có thể ghi đè cấu hình.

            String title =
                    config.getProperty(
                            key + ".title",
                            def[1]
                    );

            String icon =
                    config.getProperty(
                            key + ".icon",
                            def[2]
                    );

            String className =
                    config.getProperty(
                            key,
                            def[3]
                    ).trim();

            allowed.add(new MenuConfig(
                    key,
                    title,
                    icon,
                    className,
                    order
            ));
        }

        allowed.sort(
                Comparator.comparingInt(
                        menu -> menu.order));

        for (MenuConfig menu : allowed) {

            visibleMenus.put(
                    menu.key,
                    menu
            );

            viewItems.add(
                    new MainPage.MenuItem(
                            menu.key,
                            menu.title,
                            menu.icon
                    ));
        }

        // View chỉ nhận kết quả cuối cùng.

        view.setMenuItems(viewItems);

        view.setActiveMenu(activeKey);
        applyBadges(aggregateBadges(config, getMaCoSo()));
    }

    private boolean matchesBranch(
            String rule,
            String branchType) {

        if (rule == null) return false;

        String[] values = rule.split(",");

        for (String value : values) {

            String token = value.trim();

            if ("ALL".equalsIgnoreCase(token)) {
                return true;
            }

            if (token.equalsIgnoreCase(branchType)) {
                return true;
            }
        }

        return false;
    }

    private int readOrder(
            Properties config,
            String key,
            int fallback) {

        try {

            return Integer.parseInt(
                    config.getProperty(
                            key + ".order",
                            String.valueOf(fallback)
                    ).trim()
            );

        } catch (NumberFormatException e) {

            return fallback;
        }
    }

    // ====================================================
    // 10. DIEU HUONG TRANG
    // ====================================================

    public void openPage(String key) {

        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(
                    () -> openPage(key));

            return;
        }

        if (loading || currentCoSo == null) {
            return;
        }

        try {

            // Đọc lại Key.txt khi mở chức năng.

            rebuildMenu();

            openPageInternal(key);

        } catch (Exception e) {

            showError(
                    "LỖI ĐIỀU HƯỚNG",
                    errorMessage(e)
            );
        }
    }

    private void openPageInternal(String key) {

        MenuConfig menu =
                visibleMenus.get(key);

        if (menu == null || !permission.test(key)) {

            activeKey = null;

            view.setActiveMenu(null);

            view.showPage(view.createEmptyPage(
                    "KHÔNG KHẢ DỤNG",
                    "Chức năng này không được phép mở."
            ));

            view.setStatus(
                    "Không có quyền mở chức năng.");

            return;
        }

        activeKey = key;

        view.setActiveMenu(key);

        try {

            JComponent page =
                    createPage(menu);

            view.showPage(page);

            view.setStatus(
                    currentCoSo.getTenCoSo()
                    + "  |  "
                    + menu.title
            );

        } catch (Exception e) {

            view.showPage(view.createEmptyPage(
                    menu.title.toUpperCase(Locale.ROOT),
                    "Không thể khởi tạo trang."
            ));

            view.setStatus(errorMessage(e));
        }
    }

    // ====================================================
    // 11. KHOI TAO TRANG TU KEY.TXT
    // ====================================================

    private JComponent createPage(MenuConfig menu)
            throws Exception {

        String className = menu.className;

        if (className == null
                || className.isEmpty()) {

            return view.createEmptyPage(
                    menu.title,
                    "Chưa thiết lập class giao diện."
            );
        }

        // Chỉ nạp class từ các package View
        // đã được định nghĩa trong dự án.

        if (!className.matches(
                "(MainPage|Tv[1-6])\\.View\\."
                + "[A-Za-z_$][A-Za-z0-9_$]*"
                + "(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {

            throw new IllegalArgumentException(
                    "Đường dẫn class không hợp lệ: "
                    + className);
        }

        Class<?> pageClass;

        try {

            pageClass = Class.forName(
                    className,
                    false,
                    getClass().getClassLoader()
            );

        } catch (ClassNotFoundException e) {

            // Trang chưa được thành viên phát triển.

            if ("TRANG_CHU".equals(menu.key)) {

                return view.createEmptyPage(
                        "TRANG CHỦ",
                        "Chào mừng đến với VINABACUS"
                        + " - "
                        + currentCoSo.getTenCoSo()
                );
            }

            return view.createEmptyPage(
                    menu.title.toUpperCase(Locale.ROOT),
                    "Trang đang được phát triển."
            );
        }

        if (!JPanel.class.isAssignableFrom(pageClass)) {

            throw new IllegalArgumentException(
                    "Trang phải kế thừa JPanel: "
                    + className
            );
        }

        // Hợp đồng Passive View mới: MainPageController tạo View,
        // tìm Controller cùng TV theo tên, rồi truyền View và mã cơ sở.
        // View không import hoặc tự tạo Controller.
        java.lang.reflect.Constructor<?> viewConstructor;
        try {
            viewConstructor = pageClass.getConstructor();
        } catch (NoSuchMethodException legacy) {
            // Tương thích với trang cũ có constructor public (int maCoSo).
            return (JPanel) pageClass.getConstructor(int.class)
                    .newInstance(getMaCoSo());
        }

        String simpleName = pageClass.getSimpleName();
        if (!simpleName.endsWith("Page")) {
            throw new IllegalArgumentException("Tên View phải kết thúc bằng Page: " + className);
        }
        String owner = className.substring(0, className.indexOf('.'));
        String controllerName = owner + ".Controller."
                + simpleName.substring(0, simpleName.length() - 4) + "Controller";
        Class<?> controllerClass = Class.forName(controllerName, false,
                getClass().getClassLoader());
        JPanel page = (JPanel) viewConstructor.newInstance();
        Object childController;
        java.lang.reflect.Constructor<?> contextConstructor = null;
        try {
            contextConstructor = controllerClass.getConstructor(pageClass, int.class, String.class);
        } catch (NoSuchMethodException ignored) {
            // Các TV cũ vẫn dùng hợp đồng (View, int).
        }
        if (contextConstructor != null) {
            childController = contextConstructor.newInstance(page, getMaCoSo(), currentCoSo.getTenCoSo());
        } else {
            childController = controllerClass.getConstructor(pageClass, int.class).newInstance(page, getMaCoSo());
        }
        // Giữ Controller cùng vòng đời của View.
        page.putClientProperty("vinabacus.controller", childController);
        return page;
    }

    // ====================================================
    // 12. LAM MOI CAU HINH
    // ====================================================

    public void reloadConfig() {

        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(
                    this::reloadConfig);

            return;
        }

        if (loading) return;

        configureUI();

        changeCoSo();
    }

    // ====================================================
    // 13. XU LY LOI
    // ====================================================

    private void showError(
            String title,
            String message) {

        currentCoSo = null;

        activeKey = null;

        visibleMenus.clear();

        view.setMenuItems(
                Collections.emptyList());

        view.setNavigationEnabled(false);

        view.showPage(view.createEmptyPage(
                title,
                message
        ));

        view.setStatus(message);
    }

    private String errorMessage(Throwable error) {

        Throwable cause = error;

        while (cause.getCause() != null
                && cause.getCause() != cause) {

            cause = cause.getCause();
        }

        return cause.getMessage() == null
                ? cause.getClass().getSimpleName()
                : cause.getMessage();
    }
}