package MainPage.Icon;

import java.awt.*;
import java.awt.geom.*;

public final class Icon implements javax.swing.Icon {

    private final String key;
    private final int size;
    private final Color color;

    // ==================== CONSTRUCTOR ====================

    public Icon(String key, int size, Color color) {

        this.key = key == null
                ? ""
                : key.trim();

        this.size = Math.max(8, size);

        this.color = color;
    }

    // ==================== FACTORY ====================

    public static javax.swing.Icon create(
            String key,
            int size,
            Color color) {

        return new Icon(
                key,
                size,
                color
        );
    }

    // ==================== SIZE ====================

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    // ==================== RENDER ====================

    @Override
    public void paintIcon(
            Component c,
            Graphics g,
            int x,
            int y) {

        Graphics2D a =
                (Graphics2D) g.create();

        try {

            a.translate(x, y);

            a.scale(
                    size / 24.0,
                    size / 24.0
            );

            a.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            a.setColor(
                    color != null
                            ? color
                            : c != null
                                ? c.getForeground()
                                : Color.BLACK
            );

            a.setStroke(new BasicStroke(
                    1.8f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            ));

            switch (key) {

                case "TRANG_CHU":
                    drawHome(a);
                    break;

                case "HOC_VIEN":
                    drawUsers(a);
                    break;

                case "TUYEN_SINH":
                    drawUserPlus(a);
                    break;

                case "KHOA_HOC":
                    drawLayers(a);
                    break;

                case "THOI_KHOA_BIEU":
                case "LICH_HOC":
                    drawCalendar(a);
                    break;

                case "LOP_HOC":
                    drawClassroom(a);
                    break;

                case "DIEM_DANH":
                    drawCheck(a);
                    break;

                case "CHI_THU":
                case "HOC_PHI":
                    drawWallet(a);
                    break;

                case "KHO":
                    drawWarehouse(a);
                    break;

                case "CHUONG_TRINH":
                    drawGift(a);
                    break;

                case "NHAN_SU":
                    drawEmployee(a);
                    break;

                case "BAO_CAO":
                    drawChart(a);
                    break;

                case "HE_THONG":
                    drawSettings(a);
                    break;

                case "TIM_KIEM":
                    drawSearch(a);
                    break;

                case "THEM":
                    drawPlus(a);
                    break;

                case "SUA":
                    drawEdit(a);
                    break;

                case "XOA":
                    drawDelete(a);
                    break;

                case "LAM_MOI":
                    drawRefresh(a);
                    break;

                case "LUU":
                    drawSave(a);
                    break;

                case "DONG":
                    drawClose(a);
                    break;

                default:
                    drawDefault(a);
                    break;
            }

        } finally {

            a.dispose();
        }
    }

    // ==================== TRANG CHU ====================

    private void drawHome(Graphics2D a) {

        path(a, 2, 11, 12, 3, 22, 11);

        path(a,
                5, 9,
                5, 21,
                19, 21,
                19, 9
        );

        a.drawRect(10, 14, 4, 7);
    }

    // ==================== HOC VIEN ====================

    private void drawUsers(Graphics2D a) {

        a.drawOval(9, 3, 6, 6);

        a.drawArc(
                5, 11,
                14, 13,
                0, 180
        );

        a.drawArc(
                1, 5,
                6, 6,
                0, 270
        );

        a.drawArc(
                17, 5,
                6, 6,
                -90, 270
        );

        a.drawArc(
                0, 12,
                8, 10,
                65, 115
        );

        a.drawArc(
                16, 12,
                8, 10,
                0, 115
        );
    }

    // ==================== TUYEN SINH ====================

    private void drawUserPlus(Graphics2D a) {

        a.drawOval(6, 3, 7, 7);

        a.drawArc(
                2, 12,
                15, 12,
                0, 180
        );

        path(a, 19, 7, 19, 17);
        path(a, 14, 12, 24, 12);
    }

    // ==================== KHOA HOC ====================

    private void drawLayers(Graphics2D a) {

        path(a,
                2, 8,
                12, 3,
                22, 8,
                12, 13,
                2, 8
        );

        path(a, 3, 12, 12, 17, 21, 12);

        path(a, 3, 16, 12, 21, 21, 16);
    }

    // ==================== THOI KHOA BIEU ====================

    private void drawCalendar(Graphics2D a) {

        a.drawRoundRect(
                3, 5,
                18, 16,
                2, 2
        );

        path(a, 7, 2, 7, 8);
        path(a, 17, 2, 17, 8);

        path(a, 3, 10, 21, 10);

        for (int i = 7; i <= 17; i += 5) {

            a.fillOval(i, 13, 2, 2);
            a.fillOval(i, 17, 2, 2);
        }
    }

    // ==================== LOP HOC ====================

    private void drawClassroom(Graphics2D a) {

        a.drawRoundRect(
                2, 3,
                20, 13,
                2, 2
        );

        path(a, 12, 16, 12, 20);
        path(a, 7, 20, 17, 20);

        path(a, 6, 7, 17, 7);
        path(a, 6, 11, 13, 11);
    }

    // ==================== DIEM DANH ====================

    private void drawCheck(Graphics2D a) {

        a.drawRoundRect(
                3, 3,
                18, 18,
                2, 2
        );

        path(a,
                7, 12,
                11, 16,
                18, 8
        );
    }

    // ==================== CHI THU ====================

    private void drawWallet(Graphics2D a) {

        a.drawRoundRect(
                2, 5,
                20, 15,
                3, 3
        );

        a.drawRoundRect(
                14, 10,
                9, 6,
                2, 2
        );

        a.fillOval(17, 12, 2, 2);

        path(a, 5, 5, 17, 2, 19, 5);
    }

    // ==================== KHO ====================

    private void drawWarehouse(Graphics2D a) {

        path(a, 2, 10, 12, 3, 22, 10);

        path(a,
                4, 10,
                4, 21,
                20, 21,
                20, 10
        );

        a.drawRect(8, 12, 8, 9);

        path(a, 8, 15, 16, 15);
        path(a, 8, 18, 16, 18);
    }

    // ==================== CHUONG TRINH ====================

    private void drawGift(Graphics2D a) {

        a.drawRect(4, 9, 16, 12);

        a.drawRect(2, 6, 20, 4);

        path(a, 12, 6, 12, 21);

        a.drawOval(6, 2, 6, 4);
        a.drawOval(12, 2, 6, 4);
    }

    // ==================== NHAN SU ====================

    private void drawEmployee(Graphics2D a) {

        a.drawRoundRect(
                3, 4,
                18, 17,
                2, 2
        );

        a.drawOval(6, 7, 5, 5);

        a.drawArc(
                4, 13,
                9, 8,
                0, 180
        );

        path(a, 14, 8, 18, 8);
        path(a, 14, 12, 18, 12);
        path(a, 14, 16, 18, 16);
    }

    // ==================== BAO CAO ====================

    private void drawChart(Graphics2D a) {

        path(a,
                3, 3,
                3, 21,
                22, 21
        );

        a.drawRect(6, 13, 3, 8);

        a.drawRect(11, 8, 3, 13);

        a.drawRect(16, 3, 3, 18);
    }

    // ==================== HE THONG ====================

    private void drawSettings(Graphics2D a) {

        a.drawOval(6, 6, 12, 12);

        a.drawOval(10, 10, 4, 4);

        for (int i = 0; i < 8; i++) {

            double angle =
                    i * Math.PI / 4;

            a.draw(new Line2D.Double(

                    12 + 8 * Math.cos(angle),
                    12 + 8 * Math.sin(angle),

                    12 + 11 * Math.cos(angle),
                    12 + 11 * Math.sin(angle)
            ));
        }
    }

    // ==================== ICON DUNG CHUNG ====================

    private void drawSearch(Graphics2D a) {

        a.drawOval(3, 3, 13, 13);

        path(a, 14, 14, 21, 21);
    }

    private void drawPlus(Graphics2D a) {

        path(a, 12, 4, 12, 20);

        path(a, 4, 12, 20, 12);
    }

    private void drawEdit(Graphics2D a) {

        path(a,
                4, 16,
                16, 4,
                20, 8,
                8, 20,
                3, 21,
                4, 16
        );

        path(a, 13, 7, 17, 11);
    }

    private void drawDelete(Graphics2D a) {

        path(a, 3, 6, 21, 6);

        path(a, 8, 6, 8, 3, 16, 3, 16, 6);

        path(a,
                5, 6,
                6, 21,
                18, 21,
                19, 6
        );

        path(a, 10, 10, 10, 17);
        path(a, 14, 10, 14, 17);
    }

    private void drawRefresh(Graphics2D a) {

        a.drawArc(
                4, 4,
                16, 16,
                35, 290
        );

        path(a, 15, 4, 21, 4, 21, 10);
    }

    private void drawSave(Graphics2D a) {

        path(a,
                3, 3,
                17, 3,
                21, 7,
                21, 21,
                3, 21,
                3, 3
        );

        a.drawRect(7, 3, 9, 6);

        a.drawRect(7, 13, 10, 8);
    }

    private void drawClose(Graphics2D a) {

        path(a, 5, 5, 19, 19);

        path(a, 19, 5, 5, 19);
    }

    private void drawDefault(Graphics2D a) {

        a.drawRoundRect(
                3, 3,
                18, 18,
                3, 3
        );

        path(a, 7, 12, 17, 12);
    }

    // ==================== TIEN ICH ====================

    private static void path(
            Graphics2D g,
            double... points) {

        Path2D path = new Path2D.Double();

        path.moveTo(
                points[0],
                points[1]
        );

        for (int i = 2;
                i < points.length;
                i += 2) {

            path.lineTo(
                    points[i],
                    points[i + 1]
            );
        }

        g.draw(path);
    }
}