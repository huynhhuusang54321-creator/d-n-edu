package vn.vinabacus.view;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.MamNonController;
import vn.vinabacus.model.LopMamNon;
import vn.vinabacus.model.NamHocMamNon;
import vn.vinabacus.repository.MamNonRepository.KetQuaSaoChep;
import vn.vinabacus.tv4.NamHocMacDinh;

/**
 * Quản lý năm học, chọn mặc định và sao chép lớp có chọn lọc sang năm học mới.
 */
public final class NamHocMamNonPanel extends JPanel {

    private final MamNonController ctrl = new MamNonController();
    private final DefaultTableModel m = MamNonUi.model("Mã", "Năm học", "Từ ngày", "Đến ngày", "Mặc định");
    private final JTable table = MamNonUi.bang(m);
    private final JTextField search = new JTextField(18);
    private final JLabel status = new JLabel("Sẵn sàng");
    private List<NamHocMamNon> years = List.of();
    private final PropertyChangeListener yearListener = e -> SwingUtilities.invokeLater(this::filter);

    public NamHocMamNonPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JButton refresh = MamNonUi.nut("Làm mới", false), add = MamNonUi.nut("+ Thêm năm học", true);
        JButton edit = MamNonUi.nut("Sửa", false), find = MamNonUi.nut("Tìm kiếm", false);
        JButton makeDefault = MamNonUi.nut("Đặt mặc định", true), auto = MamNonUi.nut("Bỏ mặc định", false), copy = MamNonUi.nut("Chuyển lớp sang năm mới", false);
        MamNonUi.coCongCu(search, 230);
        MamNonUi.coCongCu(find, 112);
        MamNonUi.coCongCu(refresh, 106);
        MamNonUi.coCongCu(add, 170);
        MamNonUi.coCongCu(edit, 90);
        MamNonUi.coCongCu(makeDefault, 150);
        MamNonUi.coCongCu(auto, 150);
        MamNonUi.coCongCu(copy, 245);
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.add(MamNonUi.tieuDe("QUẢN LÝ NĂM HỌC – MẦM NON"), BorderLayout.NORTH);
        JPanel tools = new JPanel(new GridBagLayout());
        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0;
        row.gridy = 0;
        row.weightx = 1;
        row.fill = GridBagConstraints.HORIZONTAL;
        tools.add(MamNonUi.thanhCongCu(new JLabel("Tên năm học:"), search, find, refresh, add, edit), row);
        row.gridy = 1;
        row.insets = new Insets(6, 0, 0, 0);
        tools.add(MamNonUi.thanhCongCu(makeDefault, auto, copy), row);
        top.add(tools, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        refresh.addActionListener(e -> load());
        find.addActionListener(e -> filter());
        search.addActionListener(e -> filter());
        add.addActionListener(e -> edit(null));
        edit.addActionListener(e -> {
            NamHocMamNon y = selected();
            if (y != null) {
                edit(y);
            }
        });
        makeDefault.addActionListener(e -> {
            NamHocMamNon y = selected();
            if (y != null) {
                NamHocMacDinh.chon(y.maNamHoc());
                filter();
            }
        });
        auto.addActionListener(e -> {
            NamHocMacDinh.chon(0);
            filter();
        });
        copy.addActionListener(e -> {
            NamHocMamNon y = selected();
            if (y != null) {
                chooseCopy(y);
            }
        });
        SwingUtilities.invokeLater(this::load);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        NamHocMacDinh.langNghe(yearListener);
    }

    @Override
    public void removeNotify() {
        NamHocMacDinh.ngungNghe(yearListener);
        super.removeNotify();
    }

    private NamHocMamNon selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn năm học trong bảng.");
            return null;
        }
        int id = (int) m.getValueAt(table.convertRowIndexToModel(row), 0);
        return years.stream().filter(y -> y.maNamHoc() == id).findFirst().orElse(null);
    }

    private void filter() {
        int old = -1;
        int row = table.getSelectedRow();
        if (row >= 0) {
            old = (int) m.getValueAt(table.convertRowIndexToModel(row), 0);
        }
        String q = search.getText().trim().toLowerCase();
        m.setRowCount(0);
        for (NamHocMamNon y : years) {
            if (y.tenNamHoc().toLowerCase().contains(q)) {
                m.addRow(new Object[]{y.maNamHoc(), y.tenNamHoc(), y.tuNgay(), y.denNgay(),
                    NamHocMacDinh.maDaChon() == y.maNamHoc() ? "Đang chọn" : ""});
            }
        }
        for (int i = 0; i < m.getRowCount(); i++) {
            if ((int) m.getValueAt(i, 0) == old) {
                table.setRowSelectionInterval(i, i);
                break;
            }
        }
        NamHocMamNon effective = NamHocMacDinh.chonHieuLuc(years);
        status.setText("Có " + years.size() + " năm học · Đang áp dụng: " + (effective == null ? "Chưa có" : effective.tenNamHoc())
                + (NamHocMacDinh.maDaChon() == 0 ? " (tự chọn năm mới nhất)" : " (mặc định trên máy này)"));
    }

    private void load() {
        MamNonUi.tai(this, ctrl::namHoc, list -> {
            years = list;
            filter();
        });
    }

    private void edit(NamHocMamNon y) {
        JTextField name = new JTextField(y == null ? "" : y.tenNamHoc());
        MamNonNgayPicker start = new MamNonNgayPicker(y == null ? LocalDate.now() : y.tuNgay());
        MamNonNgayPicker end = new MamNonNgayPicker(y == null ? LocalDate.now().plusYears(1) : y.denNgay());
        JPanel form = MamNonUi.form("Tên năm học (*)", name, "Từ ngày", start, "Đến ngày", end);
        if (!MamNonUi.xacNhan(this, form, y == null ? "Thêm năm học" : "Sửa năm học", false)) {
            return;
        }
        try {
            LocalDate tu = start.getDate(), den = end.getDate();
            MamNonUi.ghi(this, () -> ctrl.luuNamHoc(y == null ? 0 : y.maNamHoc(), name.getText(), tu, den), this::load);
        } catch (Exception ex) {
            MamNonUi.loi(this, ex);
        }
    }

    private void chooseCopy(NamHocMamNon source) {
        List<NamHocMamNon> newer = years.stream().filter(y -> y.tuNgay().isAfter(source.tuNgay())).toList();
        if (newer.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Hãy tạo năm học mới trước khi chuyển tên lớp.");
            return;
        }
        MamNonUi.tai(this, () -> ctrl.lop(source.maNamHoc(), ""), classes -> {
            if (classes.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Năm học này chưa có lớp để chuyển.");
                return;
            }
            JComboBox<NamHocMamNon> target = new JComboBox<>(newer.toArray(new NamHocMamNon[0]));
            JCheckBox[] choices = new JCheckBox[classes.size()];
            JPanel list = new JPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            for (int i = 0; i < classes.size(); i++) {
                LopMamNon l = classes.get(i);
                choices[i] = new JCheckBox(l.tenLop() + (l.trangThai() ? "" : " · Ngừng sử dụng"));
                choices[i].setFont(choices[i].getFont().deriveFont(Font.PLAIN, 14));
                list.add(choices[i]);
            }
            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(475, Math.min(300, classes.size() * 32 + 12)));
            JPanel form = new JPanel(new BorderLayout(8, 8));
            form.add(MamNonUi.dong(new JLabel("Năm học nguồn: " + source.tenNamHoc()), new JLabel("Năm học mới:"), target), BorderLayout.NORTH);
            form.add(scroll, BorderLayout.CENTER);
            form.add(new JLabel("Chỉ sao chép tên lớp; không chuyển học sinh, giáo viên hay điểm danh."), BorderLayout.SOUTH);
            if (!MamNonUi.xacNhan(this, form, "Chọn lớp chuyển sang năm học mới", false)) {
                return;
            }
            List<Integer> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (int i = 0; i < choices.length; i++) {
                if (choices[i].isSelected()) {
                    ids.add(classes.get(i).maLop());
                    names.add(classes.get(i).tenLop());
                }
            }
            if (ids.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Bạn chưa chọn lớp nào.");
                return;
            }
            NamHocMamNon dest = (NamHocMamNon) target.getSelectedItem();
            if (dest == null) {
                return;
            }
            String msg = "Chuyển " + ids.size() + " tên lớp từ " + source.tenNamHoc() + " sang " + dest.tenNamHoc() + "?\n"
                    + String.join(", ", names) + "\nLớp đã tồn tại sẽ được bỏ qua. Không chuyển học sinh.";
            if (!MamNonUi.xacNhan(this, msg, "Xác nhận tạo lớp mới", false)) {
                return;
            }
            MamNonUi.tai(this, () -> ctrl.saoChepLop(source.maNamHoc(), dest.maNamHoc(), ids), result -> {
                JOptionPane.showMessageDialog(this, "Đã tạo: " + result.daTao() + " lớp. Bỏ qua trùng: " + result.boQua() + " lớp.");
                load();
            });
        });
    }
}
