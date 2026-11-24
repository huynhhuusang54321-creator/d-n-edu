package vn.vinabacus.view;

import java.awt.*;
import java.awt.geom.Path2D;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import vn.vinabacus.controller.MenuController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.MucMenu;

/** Cua so GUI chung: logo, chon co so, menu trai va man hinh module. */
public final class MainFrame extends JFrame {
    private final MenuController controller;
    private final MenuTrai menuTrai = new MenuTrai();
    private final JComboBox<CoSo> cbCoSo = new JComboBox<>(CoSo.values());
    private final JPanel vungGiua = new JPanel(new BorderLayout());

    public MainFrame(MenuController controller) {
        super("VINABACUS - Quản lý giáo dục");
        this.controller = Objects.requireNonNull(controller);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1360, 850);
        setMinimumSize(new Dimension(980, 630));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setIconAnh();

        JPanel thanhTren = taoThanhTren();
        add(thanhTren, BorderLayout.NORTH);

        JPanel than = new JPanel(new BorderLayout());
        than.add(menuTrai, BorderLayout.WEST);
        vungGiua.setBackground(GiaoDienMau.NEN);
        than.add(vungGiua, BorderLayout.CENTER);
        add(than, BorderLayout.CENTER);

        cbCoSo.addActionListener(e -> chuyenCoSo((CoSo) cbCoSo.getSelectedItem()));
        cbCoSo.setSelectedItem(controller.coSoHienTai());
        chuyenCoSo(controller.coSoHienTai());
    }

    private void setIconAnh() {
        ImageIcon logo = docLogo();
        if (logo != null) setIconImage(logo.getImage());
    }

    private ImageIcon docLogo() {
        URL u = getClass().getResource("/vn/vinabacus/assets/logo_vinabacus_can_tho.png");
        return u == null ? null : new ImageIcon(u);
    }

    private JPanel taoThanhTren() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g.setPaint(new GradientPaint(0, 0, GiaoDienMau.XANH_SANG,
                        w * .76f, h, GiaoDienMau.XANH_DAM));
                g.fillRect(0, 0, w, h);
                // Dai vang luon song o chan header.
                Path2D.Double song = new Path2D.Double();
                song.moveTo(270, h); song.curveTo(w * .52, h - 3, w * .83, h - 10, w, h - 24);
                song.lineTo(w, h); song.closePath();
                g.setColor(GiaoDienMau.VANG); g.fill(song);
                // Nen trang nhan logo, vong cong vang theo mau duoc duyet.
                Path2D.Double nenLogo = new Path2D.Double();
                nenLogo.moveTo(0, 0); nenLogo.lineTo(292, 0);
                nenLogo.curveTo(294, 41, 282, 85, 244, h);
                nenLogo.lineTo(0, h); nenLogo.closePath();
                g.setColor(GiaoDienMau.VANG);
                g.translate(4, 0); g.fill(nenLogo); g.translate(-4, 0);
                g.setColor(Color.WHITE); g.fill(nenLogo);
                g.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 119));
        header.setOpaque(true);

        JPanel benTrai = new JPanel(new GridBagLayout());
        benTrai.setOpaque(false);
        benTrai.setPreferredSize(new Dimension(265, 119));
        ImageIcon original = docLogo();
        JLabel logo;
        if (original != null) {
            Image scaled = original.getImage().getScaledInstance(221, 101, Image.SCALE_SMOOTH);
            logo = new JLabel(new ImageIcon(scaled));
        } else {
            logo = new JLabel("VINABACUS CẦN THƠ");
            logo.setForeground(GiaoDienMau.XANH_DAM);
            logo.setFont(GiaoDienMau.font(Font.BOLD, 22));
        }
        benTrai.add(logo);
        header.add(benTrai, BorderLayout.WEST);

        JPanel benPhai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        benPhai.setOpaque(false);
        JLabel nhan = new JLabel("Cơ sở:");
        nhan.setFont(GiaoDienMau.font(Font.BOLD, 17));
        nhan.setForeground(Color.WHITE);
        cbCoSo.setPreferredSize(new Dimension(182, 40));
        cbCoSo.setFont(GiaoDienMau.font(Font.BOLD, 15));
        cbCoSo.setBackground(Color.WHITE);
        cbCoSo.setForeground(GiaoDienMau.XANH_DAM);
        cbCoSo.setFocusable(false);
        benPhai.add(nhan);
        benPhai.add(cbCoSo);
        JPanel baoNgoai = new JPanel(new GridBagLayout());
        baoNgoai.setOpaque(false);
        baoNgoai.setBorder(new EmptyBorder(0, 20, 0, 25));
        baoNgoai.add(benPhai);
        header.add(baoNgoai, BorderLayout.EAST);
        return header;
    }

    private void chuyenCoSo(CoSo moi) {
        if (moi == null) return;
        controller.chonCoSo(moi);
        List<MucMenu> danhSach = controller.layMenu();
        menuTrai.hienThi(danhSach, this::moMenu);
        if (!danhSach.isEmpty()) moMenu(danhSach.get(0));
    }

    private void moMenu(MucMenu muc) {
        MucMenu hopLe = controller.chonMenu(muc.ma());
        menuTrai.danhDau(hopLe.ma());
        vungGiua.removeAll();
        // KhungNoiDungPanel da doc DangKyManHinh, khong can sua MainFrame khi them module.
        vungGiua.add(new KhungNoiDungPanel(hopLe), BorderLayout.CENTER);
        vungGiua.revalidate();
        vungGiua.repaint();
    }
}
