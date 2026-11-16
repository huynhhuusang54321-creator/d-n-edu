package vn.vinabacus.view;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import vn.vinabacus.model.MucMenu;

/** Sidebar mo rong, cuon doc va theo dung co so, KHONG co thu gon. */
public final class MenuTrai extends JPanel {
    private final JPanel danhSach = new JPanel();
    private final Map<String, JButton> cacNut = new LinkedHashMap<>();

    public MenuTrai() {
        super(new BorderLayout());
        setBackground(GiaoDienMau.XANH_DAM);
        setPreferredSize(new Dimension(265, 0));
        setBorder(GiaoDienMau.pad(9, 12, 8, 9));
        danhSach.setOpaque(false);
        danhSach.setLayout(new BoxLayout(danhSach, BoxLayout.Y_AXIS));
        JScrollPane cuon = new JScrollPane(danhSach);
        cuon.setOpaque(false);
        cuon.getViewport().setOpaque(false);
        cuon.setBorder(null);
        cuon.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cuon.getVerticalScrollBar().setUnitIncrement(18);
        add(cuon, BorderLayout.CENTER);
    }

    public void hienThi(List<MucMenu> menu, Consumer<MucMenu> khiChon) {
        danhSach.removeAll();
        cacNut.clear();
        danhSach.add(Box.createVerticalStrut(5));
        for (MucMenu muc : menu) {
            JButton nut = new NutRoChu(muc.ten(), new BieuTuong(muc.bieuTuong(), 20));
            nut.setHorizontalAlignment(SwingConstants.LEFT);
            nut.setVerticalAlignment(SwingConstants.CENTER);
            nut.setIconTextGap(13);
            nut.setBackground(GiaoDienMau.XANH_DAM);
            nut.setForeground(Color.WHITE);
            nut.setFont(GiaoDienMau.font(Font.BOLD, 15));
            nut.setBorder(GiaoDienMau.pad(7, 10, 7, 15));
            nut.setPreferredSize(new Dimension(236, 43));
            nut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
            nut.setAlignmentX(LEFT_ALIGNMENT);
            nut.addActionListener(e -> khiChon.accept(muc));
            danhSach.add(nut);
            danhSach.add(Box.createVerticalStrut(2));
            cacNut.put(muc.ma(), nut);
        }
        danhSach.revalidate();
        danhSach.repaint();
    }

    public void danhDau(String maMenu) {
        cacNut.forEach((ma, nut) -> {
            boolean chon = ma.equals(maMenu);
            nut.setBackground(chon ? GiaoDienMau.VANG : GiaoDienMau.XANH_DAM);
            nut.setForeground(chon ? GiaoDienMau.XANH_DAM : Color.WHITE);
        });
    }
}
