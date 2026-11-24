package vn.vinabacus.view;

import static vn.vinabacus.view.HocVienPanel.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import vn.vinabacus.controller.HocVienController;
import vn.vinabacus.controller.HocVienController.BuoiHoc;
import vn.vinabacus.controller.HocVienController.KhoaTienDo;
import vn.vinabacus.controller.HocVienController.TrangThaiBuoi;
import vn.vinabacus.controller.HocVienController.Vang;
import vn.vinabacus.model.HocVien;

/**
 * Sơ đồ khóa học / lớp học thiết kế theo phong cách thẻ bo tròn dịu mắt & nút neon.
 * Chỉ chịu trách nhiệm hiển thị và gửi sự kiện lên {@link HocVienController}.
 */
public final class HocVien_SoDoKhoaHocPanel extends JPanel {

    // Bảng màu dịu mắt lấy cảm hứng từ ảnh mẫu: nền kem xanh nhạt, thẻ trắng ngà, nhấn than & neon-lime
    private static final Color NEN_DIU = new Color(0xF1F6E8);
    private static final Color THE_SANG = new Color(0xFAFDF5);
    private static final Color THE_TOI = new Color(0x181C17);
    private static final Color THE_TOI_PHU = new Color(0x252B23);
    private static final Color NEON_XANH = new Color(0x9EE83B);
    private static final Color NEON_SANG = new Color(0xB9FF5A);
    private static final Color NEON_VIEN = new Color(0x7CCF1E);
    private static final Color VIEN_DIU = new Color(0xD3E2C4);

    private static final Color O_CO_MAT = new Color(0xBCEE72);
    private static final Color O_VANG_KP = new Color(0xF6A5A0);
    private static final Color O_VANG_CP = new Color(0xFFD68A);
    private static final Color O_CHUA_DD = new Color(0xE6ECE0);
    private static final Color O_CHUA_DEN = new Color(0xFFFFFF);

    private static final Color CHU_DEN = new Color(0x161A15);
    private static final Color CHU_XAM = new Color(0x5B6458);
    private static final Color CHU_TRANG = new Color(0xF6FAEF);
    private static final Color CHU_TRANG_MO = new Color(0xB8C4B2);
    private static final Color DO_NHAT = new Color(0x9E2A22);

    private static final Font F_TIEU_DE = FONT_DAM.deriveFont(20f);
    private static final Font F_SO_LON = FONT_DAM.deriveFont(22f);
    private static final Font F_MUC = FONT_DAM.deriveFont(14f);
    private static final Font F_NHAN = FONT_DAM.deriveFont(12f);
    private static final Font F_NHO = FONT.deriveFont(11.5f);

    private static final int SO_COT_LUOI = 10;

    private final HocVienController controller;

    public HocVien_SoDoKhoaHocPanel(HocVienController controller, HocVien h, KhoaTienDo k, Runnable khiDong) {
        super(new BorderLayout(0, 12));
        this.controller = controller;
        setBackground(NEN_DIU);
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(780, 585));

        JPanel vungTren = new JPanel(new BorderLayout(0, 10));
        vungTren.setOpaque(false);
        vungTren.add(taoHeader(h, k), BorderLayout.NORTH);
        vungTren.add(taoThongKe(k), BorderLayout.CENTER);

        add(vungTren, BorderLayout.NORTH);
        add(k.buoi().isEmpty() ? taoTrong(k) : taoVungLich(k), BorderLayout.CENTER);
        add(taoFooter(h, k, khiDong), BorderLayout.SOUTH);
    }

    // ================= 1. Khối đầu trang =================

    private static JComponent taoHeader(HocVien h, KhoaTienDo k) {
        TheTheToi card = new TheTheToi(new BorderLayout(18, 0), 22);
        card.setBorder(new EmptyBorder(14, 18, 14, 18));

        String tenKhoa = k.tenChinh() + (k.tenMonHoc().isBlank() ? "" : " (" + k.tenMonHoc() + ")");
        JLabel lblTen = new JLabel(tenKhoa);
        lblTen.setFont(F_TIEU_DE);
        lblTen.setForeground(NEON_SANG);

        JLabel lblHocVien = new JLabel("#" + h.maHocVien() + " · " + h.hoTen() + "    •    Lớp: " + k.tenLop());
        lblHocVien.setFont(FONT);
        lblHocVien.setForeground(CHU_TRANG);

        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        meta.setOpaque(false);
        meta.add(chiTietMeta("Lịch học", k.lichHoc()));
        meta.add(chiTietMeta(k.lienTuc() ? "Ngày bắt đầu / Xếp lớp" : "Ngày nhập học",
                HocVienController.ngay(k.ngayNhapHoc())));
        if (!k.lienTuc()) {
            meta.add(chiTietMeta("Kết thúc khóa", HocVienController.ngay(k.ngayKetThuc())));
            BuoiHoc tiep = k.buoiTiepTheo();
            if (tiep != null) {
                meta.add(chiTietMeta("Buổi tiếp theo",
                        "Buổi " + tiep.stt() + " (" + HocVienController.ngay(tiep.ngay()) + ")"));
            }
        }

        JPanel trai = new JPanel();
        trai.setLayout(new BoxLayout(trai, BoxLayout.Y_AXIS));
        trai.setOpaque(false);
        lblTen.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblHocVien.setAlignmentX(Component.LEFT_ALIGNMENT);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);

        trai.add(lblTen);
        trai.add(Box.createVerticalStrut(3));
        trai.add(lblHocVien);
        trai.add(Box.createVerticalStrut(10));
        trai.add(meta);

        card.add(trai, BorderLayout.CENTER);
        card.add(k.lienTuc() ? new HuyHieuMamNon(k) : new VongTienDoNeon(k), BorderLayout.EAST);
        return card;
    }

    private static JComponent chiTietMeta(String nhanText, String giaTri) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel a = new JLabel(nhanText);
        a.setFont(F_NHO);
        a.setForeground(CHU_TRANG_MO);

        JLabel b = new JLabel(giaTri == null || giaTri.isBlank() ? "—" : giaTri);
        b.setFont(FONT_DAM);
        b.setForeground(CHU_TRANG);

        p.add(a);
        p.add(Box.createVerticalStrut(2));
        p.add(b);
        return p;
    }

    // ================= 2. Hàng thẻ thống kê điểm danh =================

    private static JComponent taoThongKe(KhoaTienDo k) {
        if (!k.coDiemDanh()) {
            TheSang banner = new TheSang(new BorderLayout(), 16);
            banner.setBorder(new EmptyBorder(9, 14, 9, 14));
            JLabel l = new JLabel(k.thongKe());
            l.setFont(F_NHAN);
            l.setForeground(CHU_XAM);
            banner.add(l, BorderLayout.CENTER);
            return banner;
        }

        JPanel hang = new JPanel(new GridLayout(1, 4, 10, 0));
        hang.setOpaque(false);
        hang.add(oThongKe(TrangThaiBuoi.CO_MAT, k.dem(TrangThaiBuoi.CO_MAT), true));
        hang.add(oThongKe(TrangThaiBuoi.VANG_CO_PHEP, k.dem(TrangThaiBuoi.VANG_CO_PHEP), false));
        hang.add(oThongKe(TrangThaiBuoi.VANG_KHONG_PHEP, k.dem(TrangThaiBuoi.VANG_KHONG_PHEP), false));
        hang.add(oThongKe(TrangThaiBuoi.CHUA_DIEM_DANH, k.dem(TrangThaiBuoi.CHUA_DIEM_DANH), false));
        return hang;
    }

    private static JComponent oThongKe(TrangThaiBuoi t, long soBuoi, boolean noiBat) {
        TheSang card = new TheSang(new BorderLayout(10, 0), 18);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel so = new JLabel(String.valueOf(soBuoi));
        so.setFont(F_SO_LON);
        so.setForeground(noiBat && soBuoi > 0 ? new Color(0x3B6E0C) : CHU_DEN);

        JLabel ten = new JLabel(t.ten);
        ten.setFont(F_NHO);
        ten.setForeground(CHU_XAM);

        JPanel chu = new JPanel();
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        chu.setOpaque(false);
        chu.add(so);
        chu.add(ten);

        card.add(new JLabel(new BieuTuongTrangThai(t, 22)), BorderLayout.WEST);
        card.add(chu, BorderLayout.CENTER);
        return card;
    }

    // ================= 3. Lịch buổi học & phân trang 1 tháng cho Mầm non =================

    private static JComponent taoTrong(KhoaTienDo k) {
        TheSang card = new TheSang(new BorderLayout(), 20);
        JLabel l = new JLabel(k.ghiChu() == null ? "Chưa có buổi học nào để hiển thị." : k.ghiChu());
        l.setFont(FONT);
        l.setForeground(CHU_XAM);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(l, BorderLayout.CENTER);
        return card;
    }

    private static JComponent taoVungLich(KhoaTienDo k) {
        if (!k.lienTuc()) {
            return taoKhungLich(k, k.buoi(), null);
        }

        List<TrangThang> cacTrang = chiaTrangMotThang(k);
        if (cacTrang.isEmpty()) {
            return taoTrong(k);
        }

        final int[] idxTrang = {0};
        JPanel khayLich = new JPanel(new BorderLayout());
        khayLich.setOpaque(false);

        NutNeon btnTruoc = nutNeonPhu("◀ Tháng trước");
        NutNeon btnSau = nutNeonChinh("Tháng sau ▶");
        JLabel lblTrang = new JLabel("", SwingConstants.CENTER);
        lblTrang.setFont(F_NHAN);
        lblTrang.setForeground(CHU_DEN);

        Runnable capNhatTrang = () -> {
            TrangThang trang = cacTrang.get(idxTrang[0]);
            khayLich.removeAll();
            khayLich.add(taoKhungLich(k, trang.buoi(), trang.nhanTieuDe()), BorderLayout.CENTER);
            khayLich.revalidate();
            khayLich.repaint();

            btnTruoc.setEnabled(idxTrang[0] > 0);
            btnSau.setEnabled(idxTrang[0] < cacTrang.size() - 1);
            lblTrang.setText("Trang " + (idxTrang[0] + 1) + " / " + cacTrang.size()
                    + "   •   " + HocVienController.ngay(trang.tuNgay())
                    + " – " + HocVienController.ngay(trang.denNgay()));
        };

        btnTruoc.addActionListener(e -> {
            if (idxTrang[0] > 0) {
                idxTrang[0]--;
                capNhatTrang.run();
            }
        });
        btnSau.addActionListener(e -> {
            if (idxTrang[0] < cacTrang.size() - 1) {
                idxTrang[0]++;
                capNhatTrang.run();
            }
        });

        JPanel thanhTrang = new JPanel(new BorderLayout(12, 0));
        thanhTrang.setOpaque(false);
        thanhTrang.setBorder(new EmptyBorder(8, 2, 0, 2));
        thanhTrang.add(btnTruoc, BorderLayout.WEST);
        thanhTrang.add(lblTrang, BorderLayout.CENTER);
        thanhTrang.add(btnSau, BorderLayout.EAST);

        capNhatTrang.run();

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(khayLich, BorderLayout.CENTER);
        if (cacTrang.size() > 1) {
            wrap.add(thanhTrang, BorderLayout.SOUTH);
        }
        return wrap;
    }

    private record TrangThang(int soTrang, LocalDate tuNgay, LocalDate denNgay, List<BuoiHoc> buoi) {
        String nhanTieuDe() {
            return "Tháng " + soTrang + " · " + buoi.size() + " buổi";
        }
    }

    /** Chia buổi học liên tục thành từng khoảng 1 tháng tính từ ngày bắt đầu học. */
    private static List<TrangThang> chiaTrangMotThang(KhoaTienDo k) {
        List<BuoiHoc> ds = k.buoi();
        if (ds.isEmpty() || k.ngayNhapHoc() == null) return List.of();

        List<TrangThang> out = new ArrayList<>();
        LocalDate mocDau = k.ngayNhapHoc();
        LocalDate ngayCuoi = ds.get(ds.size() - 1).ngay();
        int i = 0;
        int trang = 1;

        while (!mocDau.isAfter(ngayCuoi) && i < ds.size()) {
            LocalDate hanThang = mocDau.plusMonths(1);
            LocalDate den = hanThang.isAfter(ngayCuoi) ? ngayCuoi : hanThang;

            List<BuoiHoc> buoiTrang = new ArrayList<>();
            while (i < ds.size() && !ds.get(i).ngay().isAfter(hanThang)) {
                buoiTrang.add(ds.get(i));
                i++;
            }
            if (!buoiTrang.isEmpty()) {
                out.add(new TrangThang(trang++, mocDau, den, buoiTrang));
            }
            mocDau = hanThang.plusDays(1);
        }
        return out;
    }

    private static JComponent taoKhungLich(KhoaTienDo k, List<BuoiHoc> dsBuoi, String phuDeTrang) {
        BuoiHoc tiep = k.buoiTiepTheo();
        Map<YearMonth, List<BuoiHoc>> theoThang = dsBuoi.stream().collect(Collectors.groupingBy(
                b -> YearMonth.from(b.ngay()), LinkedHashMap::new, Collectors.toList()));

        JPanel cot = new JPanel(new GridBagLayout());
        cot.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.insets = new Insets(0, 0, 12, 0);

        int row = 0;
        for (Map.Entry<YearMonth, List<BuoiHoc>> e : theoThang.entrySet()) {
            g.gridy = row++;
            cot.add(nhomThang(e.getKey(), e.getValue(), tiep, k), g);
        }

        JPanel boc = new JPanel(new BorderLayout());
        boc.setOpaque(false);
        boc.add(cot, BorderLayout.NORTH);

        JScrollPane cuon = new JScrollPane(boc);
        cuon.setBorder(null);
        cuon.setOpaque(false);
        cuon.getViewport().setOpaque(false);
        cuon.getVerticalScrollBar().setUnitIncrement(18);

        TheSang card = new TheSang(new BorderLayout(0, 10), 20);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.add(taoThanhTieuDeLich(k, phuDeTrang), BorderLayout.NORTH);
        card.add(cuon, BorderLayout.CENTER);
        return card;
    }

    private static JComponent taoThanhTieuDeLich(KhoaTienDo k, String phuDeTrang) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);

        String title = k.lienTuc()
                ? "Buổi đã học" + (phuDeTrang == null ? "" : "  •  " + phuDeTrang)
                : "Các buổi học theo lịch";
        JLabel lbl = new JLabel(title);
        lbl.setFont(F_MUC);
        lbl.setForeground(CHU_DEN);

        JPanel chuThich = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        chuThich.setOpaque(false);
        for (TrangThaiBuoi t : TrangThaiBuoi.values()) {
            if (k.lienTuc() && t == TrangThaiBuoi.CHUA_DEN) continue;
            JLabel item = new JLabel(t.ten, new BieuTuongTrangThai(t, 14), SwingConstants.LEFT);
            item.setFont(F_NHO);
            item.setForeground(CHU_DEN);
            item.setIconTextGap(5);
            chuThich.add(item);
        }

        p.add(lbl, BorderLayout.WEST);
        p.add(chuThich, BorderLayout.EAST);
        return p;
    }

    private static JComponent nhomThang(YearMonth ym, List<BuoiHoc> ds, BuoiHoc tiep, KhoaTienDo k) {
        JLabel lbl = new JLabel(String.format("Tháng %02d/%d  •  %d buổi", ym.getMonthValue(), ym.getYear(), ds.size()));
        lbl.setFont(F_NHAN);
        lbl.setForeground(CHU_XAM);

        JPanel luoi = new JPanel(new GridLayout(0, SO_COT_LUOI, 7, 7));
        luoi.setOpaque(false);
        for (BuoiHoc b : ds) {
            String note = b.stt() == 1 ? (k.lienTuc() ? " · Buổi đầu (bắt đầu học)" : " · Buổi đầu (nhập học)")
                    : (!k.lienTuc() && k.ngayKetThuc() != null && b.ngay().equals(k.ngayKetThuc()))
                            ? " · Buổi cuối (kết thúc khóa)" : "";
            boolean isTiep = b.equals(tiep);
            luoi.add(new OBuoiNeon(b, isTiep, b.goiY() + note + (isTiep ? " · Buổi tiếp theo" : "")));
        }

        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.add(lbl, BorderLayout.NORTH);
        p.add(luoi, BorderLayout.CENTER);
        return p;
    }

    // ================= 4. Footer: buổi vắng + ngày tốt nghiệp / chế độ Mầm non + Đóng =================

    private JComponent taoFooter(HocVien h, KhoaTienDo k, Runnable khiDong) {
        JPanel haiCot = new JPanel(new GridLayout(1, 2, 10, 0));
        haiCot.setOpaque(false);
        haiCot.add(taoTheVang(k));
        haiCot.add(k.lienTuc() ? taoTheLienTuc() : taoTheTotNghiep(h, k));

        NutNeon btnDong = nutNeonChinh("Đóng");
        btnDong.addActionListener(e -> khiDong.run());

        JPanel hangNut = new JPanel(new BorderLayout(12, 0));
        hangNut.setOpaque(false);
        if (!k.lienTuc() && k.ghiChu() != null && !k.buoi().isEmpty()) {
            JLabel gc = new JLabel(k.ghiChu());
            gc.setFont(F_NHO);
            gc.setForeground(DO_NHAT);
            hangNut.add(gc, BorderLayout.CENTER);
        }
        hangNut.add(btnDong, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.add(haiCot, BorderLayout.CENTER);
        footer.add(hangNut, BorderLayout.SOUTH);
        return footer;
    }

    private static JComponent taoTheVang(KhoaTienDo k) {
        TheSang card = new TheSang(new BorderLayout(0, 6), 18);
        card.setBorder(new EmptyBorder(11, 15, 11, 15));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Buổi vắng");
        title.setFont(F_MUC);
        title.setForeground(CHU_DEN);
        top.add(title, BorderLayout.WEST);

        if (k.coDiemDanh()) {
            JLabel badge = new JLabel(String.valueOf(k.vang().size()));
            badge.setOpaque(true);
            badge.setBackground(k.vang().isEmpty() ? O_CO_MAT : O_VANG_KP);
            badge.setForeground(CHU_DEN);
            badge.setFont(F_NHAN);
            badge.setBorder(new EmptyBorder(2, 9, 2, 9));
            top.add(badge, BorderLayout.EAST);
        }

        JComponent body;
        if (k.vang().isEmpty()) {
            JLabel l = new JLabel(k.danhSachVangText());
            l.setFont(F_NHAN);
            l.setForeground(CHU_XAM);
            l.setVerticalAlignment(SwingConstants.TOP);
            body = l;
        } else {
            JPanel list = new JPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            list.setOpaque(false);
            for (Vang v : k.vang()) {
                JLabel item = new JLabel(v.moTa(), new BieuTuongTrangThai(v.trangThai(), 14), SwingConstants.LEFT);
                item.setFont(F_NHO);
                item.setForeground(CHU_DEN);
                item.setIconTextGap(6);
                item.setBorder(new EmptyBorder(1, 0, 1, 0));
                list.add(item);
            }
            JScrollPane sp = new JScrollPane(list);
            sp.setBorder(null);
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setPreferredSize(new Dimension(0, 64));
            body = sp;
        }

        card.add(top, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private static JComponent taoTheLienTuc() {
        TheSang card = new TheSang(new BorderLayout(0, 4), 18);
        card.setBorder(new EmptyBorder(11, 15, 11, 15));

        JLabel title = new JLabel("Chế độ học Mầm non");
        title.setFont(F_MUC);
        title.setForeground(CHU_DEN);

        JLabel desc = new JLabel("<html>Học liên tục Thứ 2 – Thứ 7, không có ngày kết thúc khóa.<br>"
                + "Lật từng trang 1 tháng để theo dõi các buổi đã học.</html>");
        desc.setFont(F_NHO);
        desc.setForeground(CHU_XAM);

        card.add(title, BorderLayout.NORTH);
        card.add(desc, BorderLayout.CENTER);
        return card;
    }

    private JComponent taoTheTotNghiep(HocVien h, KhoaTienDo k) {
        TheSang card = new TheSang(new BorderLayout(0, 6), 18);
        card.setBorder(new EmptyBorder(11, 15, 11, 15));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Ngày tốt nghiệp");
        title.setFont(F_MUC);
        title.setForeground(CHU_DEN);
        top.add(title, BorderLayout.WEST);

        if (!k.nhanTotNghiep().isBlank()) {
            JLabel badge = new JLabel(k.nhanTotNghiep());
            badge.setOpaque(true);
            badge.setBackground(k.totNghiepTuyChinh() ? O_VANG_CP : O_CO_MAT);
            badge.setForeground(CHU_DEN);
            badge.setFont(F_NHO);
            badge.setBorder(new EmptyBorder(2, 9, 2, 9));
            top.add(badge, BorderLayout.EAST);
        }

        JTextField tf = styleField(new JTextField(HocVienController.ngayNhap(k.ngayTotNghiep()), 9));
        tf.setToolTipText("dd/MM/yyyy");
        NutNeon btnLuu = nutNeonChinh("Lưu");
        NutNeon btnTuTinh = nutNeonPhu("Tự tính");
        btnTuTinh.setEnabled(k.ngayKetThuc() != null);
        btnLuu.addActionListener(e -> controller.datNgayTotNghiep(h, k.maLop(), tf.getText()));
        btnTuTinh.addActionListener(e -> controller.tuTinhNgayTotNghiep(h, k.maLop()));

        JPanel hangNhap = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        hangNhap.setOpaque(false);
        hangNhap.add(tf);
        hangNhap.add(btnLuu);
        hangNhap.add(btnTuTinh);

        JLabel hint = new JLabel("Định dạng dd/MM/yyyy · Tự tính theo buổi cuối khóa");
        hint.setFont(F_NHO);
        hint.setForeground(CHU_XAM);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        hangNhap.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(hangNhap);
        body.add(Box.createVerticalStrut(4));
        body.add(hint);

        card.add(top, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // ================= Nút neon & vẽ tùy chỉnh =================

    private static NutNeon nutNeonChinh(String text) {
        return new NutNeon(text, NEON_XANH, NEON_SANG, NEON_VIEN, CHU_DEN);
    }

    private static NutNeon nutNeonPhu(String text) {
        return new NutNeon(text, THE_TOI_PHU, THE_TOI, NEON_VIEN, CHU_TRANG);
    }

    private static final class NutNeon extends JButton {
        private final Color nenThuong, nenHover, vienNeon, mauChu;
        private boolean hover;

        NutNeon(String text, Color nenThuong, Color nenHover, Color vienNeon, Color mauChu) {
            super(text);
            this.nenThuong = nenThuong;
            this.nenHover = nenHover;
            this.vienNeon = vienNeon;
            this.mauChu = mauChu;
            setFont(F_NHAN);
            setForeground(mauChu);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 16, 8, 16));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = muot(g0);
            int w = getWidth(), h = getHeight();
            boolean active = isEnabled() && hover;

            if (active) {
                g.setColor(new Color(NEON_SANG.getRed(), NEON_SANG.getGreen(), NEON_SANG.getBlue(), 75));
                g.fillRoundRect(0, 0, w, h, h, h);
            }

            g.setColor(!isEnabled() ? new Color(0xE2E8DA) : (active ? nenHover : nenThuong));
            g.fillRoundRect(2, 2, w - 4, h - 4, h - 4, h - 4);

            g.setStroke(new BasicStroke(active ? 1.8f : 1.2f));
            g.setColor(!isEnabled() ? VIEN_DIU : (active ? NEON_SANG : vienNeon));
            g.drawRoundRect(2, 2, w - 5, h - 5, h - 4, h - 4);

            setForeground(!isEnabled() ? CHU_XAM : mauChu);
            super.paintComponent(g);
            g.dispose();
        }
    }

    private static Color mauO(TrangThaiBuoi t) {
        return switch (t) {
            case CO_MAT -> O_CO_MAT;
            case VANG_CO_PHEP -> O_VANG_CP;
            case VANG_KHONG_PHEP -> O_VANG_KP;
            case CHUA_DIEM_DANH -> O_CHUA_DD;
            case CHUA_DEN -> O_CHUA_DEN;
        };
    }

    private static void veKyHieu(Graphics2D g, TrangThaiBuoi t, int x, int y, int s) {
        g.setColor(CHU_DEN);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (t) {
            case CO_MAT -> {
                g.drawLine(x, y + s / 2, x + s / 3, y + s - 1);
                g.drawLine(x + s / 3, y + s - 1, x + s, y + 1);
            }
            case VANG_KHONG_PHEP -> {
                g.drawLine(x, y, x + s, y + s);
                g.drawLine(x + s, y, x, y + s);
            }
            case VANG_CO_PHEP -> {
                g.setFont(FONT_DAM.deriveFont(s + 2f));
                g.drawString("P", x, y + s);
            }
            case CHUA_DIEM_DANH -> g.drawLine(x, y + s / 2, x + s, y + s / 2);
            case CHUA_DEN -> { }
        }
    }

    private static Graphics2D muot(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    private static final class TheSang extends JPanel {
        private final int boGoc;

        TheSang(LayoutManager lm, int boGoc) {
            super(lm);
            this.boGoc = boGoc;
            setOpaque(false);
            setBorder(new EmptyBorder(12, 14, 12, 14));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = muot(g0);
            g.setColor(THE_SANG);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, boGoc, boGoc);
            g.setColor(VIEN_DIU);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, boGoc, boGoc);
            g.dispose();
        }
    }

    private static final class TheTheToi extends JPanel {
        private final int boGoc;

        TheTheToi(LayoutManager lm, int boGoc) {
            super(lm);
            this.boGoc = boGoc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = muot(g0);
            g.setColor(THE_TOI);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, boGoc, boGoc);
            g.setColor(new Color(NEON_XANH.getRed(), NEON_XANH.getGreen(), NEON_XANH.getBlue(), 95));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, boGoc, boGoc);
            g.dispose();
        }
    }

    private static final class HuyHieuMamNon extends JPanel {
        HuyHieuMamNon(KhoaTienDo k) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(NEON_VIEN, 1, true),
                    new EmptyBorder(10, 16, 10, 16)));

            JLabel so = new JLabel(String.valueOf(k.daHoc()));
            so.setFont(F_SO_LON);
            so.setForeground(NEON_SANG);
            so.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel chu = new JLabel("buổi đã học");
            chu.setFont(F_NHAN);
            chu.setForeground(CHU_TRANG);
            chu.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel sub = new JLabel("Không kết thúc");
            sub.setFont(F_NHO);
            sub.setForeground(CHU_TRANG_MO);
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);

            add(so);
            add(chu);
            add(sub);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = muot(g0);
            g.setColor(THE_TOI_PHU);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    private static final class VongTienDoNeon extends JComponent {
        private final int phanTram, daHoc, tong;

        VongTienDoNeon(KhoaTienDo k) {
            this.phanTram = k.phanTram();
            this.daHoc = k.daHoc();
            this.tong = k.tongSoBuoi();
            setPreferredSize(new Dimension(96, 96));
            setToolTipText(k.textTienDo());
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = muot(g0);
            int w = getWidth(), h = getHeight(), d = Math.min(w, h) - 10;
            int x = (w - d) / 2, y = (h - d) / 2;

            g.setStroke(new BasicStroke(8.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(THE_TOI_PHU);
            g.drawOval(x, y, d, d);
            if (phanTram > 0) {
                g.setColor(NEON_SANG);
                g.drawArc(x, y, d, d, 90, -Math.round(360f * phanTram / 100f));
            }

            g.setColor(CHU_TRANG);
            g.setFont(F_SO_LON);
            FontMetrics fm = g.getFontMetrics();
            String s1 = phanTram + "%";
            g.drawString(s1, (w - fm.stringWidth(s1)) / 2, h / 2 + 2);

            g.setFont(F_NHO);
            fm = g.getFontMetrics();
            g.setColor(CHU_TRANG_MO);
            String s2 = daHoc + "/" + tong;
            g.drawString(s2, (w - fm.stringWidth(s2)) / 2, h / 2 + 17);
            g.dispose();
        }
    }

    private static final class OBuoiNeon extends JComponent {
        private final BuoiHoc b;
        private final boolean tiep;
        private boolean hover;

        OBuoiNeon(BuoiHoc b, boolean tiep, String goiY) {
            this.b = b;
            this.tiep = tiep;
            setPreferredSize(new Dimension(64, 48));
            setToolTipText(goiY);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = muot(g0);
            int w = getWidth(), h = getHeight();

            if (hover || tiep) {
                g.setColor(new Color(NEON_XANH.getRed(), NEON_XANH.getGreen(), NEON_XANH.getBlue(), hover ? 90 : 50));
                g.fillRoundRect(0, 0, w, h, 12, 12);
            }

            g.setColor(mauO(b.trangThai()));
            g.fillRoundRect(2, 2, w - 4, h - 4, 11, 11);

            g.setStroke(new BasicStroke(tiep ? 2.2f : (hover ? 1.6f : 1f)));
            g.setColor(tiep ? NEON_VIEN : (hover ? CHU_DEN : VIEN_DIU));
            g.drawRoundRect(2, 2, w - 5, h - 5, 11, 11);

            g.setColor(CHU_DEN);
            g.setFont(FONT_DAM.deriveFont(13.5f));
            g.drawString(String.valueOf(b.stt()), 8, 19);

            g.setFont(F_NHO);
            g.drawString(b.nhanNgay(), 8, h - 8);

            veKyHieu(g, b.trangThai(), w - 20, 8, 10);
            g.dispose();
        }
    }

    private record BieuTuongTrangThai(TrangThaiBuoi t, int kichThuoc) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = muot(g0);
            g.setColor(mauO(t));
            g.fillRoundRect(x, y, kichThuoc - 1, kichThuoc - 1, 5, 5);
            g.setStroke(new BasicStroke(1f));
            g.setColor(VIEN_DIU);
            g.drawRoundRect(x, y, kichThuoc - 1, kichThuoc - 1, 5, 5);
            int s = Math.max(6, kichThuoc / 2);
            veKyHieu(g, t, x + (kichThuoc - s) / 2, y + (kichThuoc - s) / 2, s);
            g.dispose();
        }

        @Override
        public int getIconWidth() {
            return kichThuoc;
        }

        @Override
        public int getIconHeight() {
            return kichThuoc;
        }
    }
}