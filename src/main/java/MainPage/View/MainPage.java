package MainPage.View;

import java.awt.*;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import static MainPage.Icon.Icon.create;

public class MainPage extends JFrame {

    // ==================== COMPONENT ====================

    private final JPanel header = new JPanel();
    private final JPanel menuPanel = new JPanel();
    private final JPanel pageArea = new JPanel(new BorderLayout());

    private final JLabel lblLogo = new JLabel();
    private final JLabel lblCoSo = new JLabel();
    private final JLabel lblStatus = new JLabel();

    private final JComboBox<CoSoItem> cboCoSo = new JComboBox<>();

    private final Map<String, JButton> buttons =
            new LinkedHashMap<>();

    private final List<ActionListener> menuListeners =
            new ArrayList<>();

    private final JScrollPane menuScroll;

    private UIConfig config = new UIConfig();

    private boolean navigationEnabled = true;

    // ==================== CAU HINH GIAO DIEN ====================

    public static class UIConfig {

        public String windowTitle = "";
        public String brandName = "";
        public String brandSubtitle = "";
        public String logoPath = "";

        public String branchLabel = "";
        public String fontName = "SansSerif";

        public Color primaryColor = Color.DARK_GRAY;
        public Color headerStartColor = Color.GRAY;
        public Color headerEndColor = Color.DARK_GRAY;

        public Color activeColor = Color.YELLOW;
        public Color menuTextColor = Color.WHITE;
        public Color activeTextColor = Color.BLACK;

        public Color backgroundColor = Color.WHITE;
        public Color pageColor = Color.WHITE;
        public Color statusColor = Color.GRAY;

        public Color badgeColor = Color.RED;
        public Color badgeTextColor = Color.WHITE;
        public int badgeFontSize = 12;
        public int badgeHeight = 22;
        public int badgePadding = 6;
        public int badgeRightGap = 8;
        public int badgeTextGap = 6;
        public boolean showBadges = true;

        public int windowWidth = 1280;
        public int windowHeight = 800;

        public int headerHeight = 110;
        public int sidebarWidth = 250;
        public int logoWidth = 270;

        public int menuHeight = 45;
        public int menuFontSize = 15;
        public int iconSize = 22;

        public int comboWidth = 220;
        public int comboHeight = 38;
        public int contentPadding = 15;

        public boolean maximized = true;
        public boolean showHeader = true;
        public boolean showSidebar = true;
        public boolean showStatus = true;
    }

    // ==================== DU LIEU HIEN THI ====================

    public static class MenuItem {

        public final String key;
        public final String title;
        public final String iconKey;

        public MenuItem(
                String key,
                String title,
                String iconKey) {

            this.key = key;
            this.title = title;
            this.iconKey = iconKey;
        }
    }

    public static class CoSoItem {

        public final int maCoSo;
        public final String tenCoSo;

        public CoSoItem(int maCoSo, String tenCoSo) {

            this.maCoSo = maCoSo;
            this.tenCoSo = tenCoSo;
        }

        @Override
        public String toString() {
            return tenCoSo;
        }
    }

    // ==================== CONSTRUCTOR ====================

    public MainPage() {

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        menuPanel.setLayout(
                new BoxLayout(menuPanel, BoxLayout.Y_AXIS));

        menuScroll = new JScrollPane(
                menuPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        menuScroll.setBorder(null);
        menuScroll.getVerticalScrollBar()
                .setUnitIncrement(18);

        add(header, BorderLayout.NORTH);
        add(menuScroll, BorderLayout.WEST);
        add(pageArea, BorderLayout.CENTER);
        add(lblStatus, BorderLayout.SOUTH);

        applyConfig(config);
    }

    // ==================== NHAN CAU HINH ====================

    public final void applyConfig(UIConfig config) {

        this.config = config;

        setTitle(config.windowTitle);

        setSize(
                config.windowWidth,
                config.windowHeight
        );

        setLocationRelativeTo(null);

        if (config.maximized) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            setExtendedState(JFrame.NORMAL);
        }

        getContentPane().setBackground(
                config.backgroundColor);

        header.setVisible(config.showHeader);
        menuScroll.setVisible(config.showSidebar);
        lblStatus.setVisible(config.showStatus);

        buildHeader();
        styleSidebar();

        pageArea.setBackground(config.pageColor);

        pageArea.setBorder(new EmptyBorder(
                config.contentPadding,
                config.contentPadding,
                config.contentPadding,
                config.contentPadding
        ));

        lblStatus.setFont(new Font(
                config.fontName,
                Font.PLAIN,
                12
        ));

        lblStatus.setForeground(config.statusColor);

        lblStatus.setBorder(
                new EmptyBorder(5, 12, 5, 12));

        // Áp dụng lại cấu hình cho các nút đã tồn tại.
        for (JButton button : buttons.values()) {

            styleButton(button);

            boolean active = Boolean.TRUE.equals(
                    button.getClientProperty("active"));

            paintButton(button, active);
        }

        revalidate();
        repaint();
    }

    // ==================== HEADER ====================

    private void buildHeader() {

        header.removeAll();
        header.setLayout(new BorderLayout());

        header.setPreferredSize(
                new Dimension(0, config.headerHeight));

        JPanel background = new JPanel(
                new BorderLayout()) {

            @Override
            protected void paintComponent(Graphics g) {

                super.paintComponent(g);

                Graphics2D g2 =
                        (Graphics2D) g.create();

                GradientPaint gradient =
                        new GradientPaint(
                                0, 0,
                                config.headerStartColor,
                                getWidth(), 0,
                                config.headerEndColor
                        );

                g2.setPaint(gradient);

                g2.fillRect(
                        0, 0,
                        getWidth(),
                        getHeight()
                );

                g2.setColor(config.activeColor);

                g2.fillPolygon(
                        new int[]{
                            0,
                            getWidth(),
                            getWidth()
                        },
                        new int[]{
                            getHeight(),
                            getHeight() - 20,
                            getHeight()
                        },
                        3
                );

                g2.dispose();
            }
        };

        background.add(createLogoPanel(),
                BorderLayout.WEST);

        background.add(createBranchPanel(),
                BorderLayout.CENTER);

        header.add(background, BorderLayout.CENTER);
    }

    private JPanel createLogoPanel() {

        JPanel panel = new JPanel(
                new BorderLayout()) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 =
                        (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                g2.setColor(config.activeColor);

                g2.fillRoundRect(
                        -80, -80,
                        getWidth() + 80,
                        getHeight() + 80,
                        100, 100
                );

                g2.setColor(Color.WHITE);

                g2.fillRoundRect(
                        -84, -84,
                        getWidth() + 80,
                        getHeight() + 80,
                        100, 100
                );

                g2.dispose();
            }
        };

        panel.setOpaque(false);

        panel.setPreferredSize(
                new Dimension(
                        config.logoWidth,
                        config.headerHeight
                ));

        lblLogo.setHorizontalAlignment(
                SwingConstants.CENTER);

        lblLogo.setVerticalAlignment(
                SwingConstants.CENTER);

        lblLogo.setIcon(null);
        lblLogo.setText("");

        if (config.logoPath != null
                && !config.logoPath.isEmpty()) {

            URL url = getClass().getResource(
                    config.logoPath);

            if (url != null) {

                ImageIcon original =
                        new ImageIcon(url);

                if (original.getIconWidth() > 0
                        && original.getIconHeight() > 0) {

                    double scale = Math.min(
                            (config.logoWidth - 30.0)
                            / original.getIconWidth(),

                            (config.headerHeight - 20.0)
                            / original.getIconHeight()
                    );

                    Image image =
                            original.getImage()
                                    .getScaledInstance(
                                            Math.max(1,
                                                (int) (
                                                    original.getIconWidth()
                                                    * scale)),
                                            Math.max(1,
                                                (int) (
                                                    original.getIconHeight()
                                                    * scale)),
                                            Image.SCALE_SMOOTH
                                    );

                    lblLogo.setIcon(
                            new ImageIcon(image));
                }
            }
        }

        if (lblLogo.getIcon() == null) {

            lblLogo.setText(
                    "<html><center>"
                    + "<span style='font-size:28pt;"
                    + "font-weight:bold;color:#05532b'>"
                    + escapeHtml(config.brandName)
                    + "</span><br>"
                    + "<span style='font-size:13pt;"
                    + "font-weight:bold;color:#05532b'>"
                    + escapeHtml(config.brandSubtitle)
                    + "</span></center></html>"
            );
        }

        panel.add(lblLogo, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBranchPanel() {

        JPanel outer = new JPanel(
                new BorderLayout());

        outer.setOpaque(false);

        JPanel panel = new JPanel(
                new GridBagLayout());

        panel.setOpaque(false);

        panel.setBorder(
                new EmptyBorder(0, 0, 0, 30));

        lblCoSo.setText(config.branchLabel);
        lblCoSo.setForeground(Color.WHITE);

        lblCoSo.setFont(new Font(
                config.fontName,
                Font.BOLD,
                15
        ));

        cboCoSo.setPreferredSize(
                new Dimension(
                        config.comboWidth,
                        config.comboHeight
                ));

        cboCoSo.setFont(new Font(
                config.fontName,
                Font.BOLD,
                14
        ));

        cboCoSo.setForeground(config.primaryColor);

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets = new Insets(0, 6, 0, 6);

        gbc.gridx = 0;
        panel.add(lblCoSo, gbc);

        gbc.gridx = 1;
        panel.add(cboCoSo, gbc);

        outer.add(panel, BorderLayout.EAST);

        return outer;
    }

    // ==================== SIDEBAR ====================

    private void styleSidebar() {

        menuPanel.setBackground(
                config.primaryColor);

        menuPanel.setBorder(
                new EmptyBorder(8, 10, 8, 6));

        menuScroll.setPreferredSize(
                new Dimension(
                        config.sidebarWidth,
                        0
                ));

        menuScroll.getViewport().setBackground(
                config.primaryColor);
    }

    public void setMenuItems(List<MenuItem> items) {

        buttons.clear();
        menuPanel.removeAll();

        for (MenuItem item : items) {

            JButton button = new BadgeButton(item.title);

            button.setActionCommand(item.key);

            button.putClientProperty(
                    "iconKey",
                    item.iconKey
            );

            button.putClientProperty("active", false);

            styleButton(button);
            paintButton(button, false);

            button.addActionListener(e -> {

                for (ActionListener listener
                        : new ArrayList<>(menuListeners)) {

                    listener.actionPerformed(e);
                }
            });

            buttons.put(item.key, button);

            menuPanel.add(button);

            menuPanel.add(
                    Box.createVerticalStrut(3));
        }

        menuPanel.add(Box.createVerticalGlue());

        menuPanel.revalidate();
        menuPanel.repaint();
    }

    private void styleButton(JButton button) {

        button.setHorizontalAlignment(
                SwingConstants.LEFT);

        button.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        button.setFont(new Font(
                config.fontName,
                Font.BOLD,
                config.menuFontSize
        ));

        button.setIconTextGap(14);

        if (button instanceof BadgeButton badgeButton) {
            badgeButton.updateBadgeBorder();
        } else {
            button.setBorder(new EmptyBorder(8, 12, 8, 6));
        }

        button.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        config.menuHeight
                ));

        button.setPreferredSize(
                new Dimension(
                        config.sidebarWidth - 20,
                        config.menuHeight
                ));

        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR));

        button.setEnabled(navigationEnabled);
    }

    private void paintButton(
            JButton button,
            boolean active) {

        Color foreground = active
                ? config.activeTextColor
                : config.menuTextColor;

        Color background = active
                ? config.activeColor
                : config.primaryColor;

        button.putClientProperty("active", active);

        button.setForeground(foreground);
        button.setBackground(background);

        String iconKey = (String)
                button.getClientProperty("iconKey");

        button.setIcon(create(
                iconKey,
                config.iconSize,
                foreground
        ));
    }

    public void setActiveMenu(String key) {

        for (Map.Entry<String, JButton> entry
                : buttons.entrySet()) {

            paintButton(
                    entry.getValue(),
                    entry.getKey().equals(key)
            );
        }
    }

    // ==================== BADGE - CHI HIEN THI ====================

    /** Nhận snapshot nhãn từ Controller; thiếu key nghĩa là ẩn badge. */
    public void setMenuBadges(Map<String, String> labels) {
        for (Map.Entry<String, JButton> entry : buttons.entrySet()) {
            if (entry.getValue() instanceof BadgeButton button) {
                button.setBadgeText(labels.get(entry.getKey()));
            }
        }
    }

    private final class BadgeButton extends JButton {
        private String badgeText = "";

        BadgeButton(String title) {
            super(title);
        }

        void setBadgeText(String text) {
            String next = text == null ? "" : text;
            if (badgeText.equals(next)) return;
            badgeText = next;
            updateBadgeBorder();
            revalidate();
            repaint();
        }

        private Font badgeFont() {
            return new Font(config.fontName, Font.BOLD, config.badgeFontSize);
        }

        private boolean hasBadge() {
            return config.showBadges && !badgeText.isEmpty();
        }

        private int badgeWidth(FontMetrics fm) {
            return Math.max(config.badgeHeight,
                    fm.stringWidth(badgeText) + config.badgePadding * 2);
        }

        void updateBadgeBorder() {
            int right = 6;
            if (hasBadge()) {
                right = config.badgeRightGap + badgeWidth(getFontMetrics(badgeFont()))
                        + config.badgeTextGap;
            }
            // Dành chỗ cho badge để không đè lên tên nút.
            setBorder(new EmptyBorder(8, 12, 8, right));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!hasBadge()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(badgeFont());
                FontMetrics fm = g2.getFontMetrics();
                int width = badgeWidth(fm);
                int height = config.badgeHeight;
                int x = getWidth() - config.badgeRightGap - width;
                int y = (getHeight() - height) / 2;
                g2.setColor(config.badgeColor);
                g2.fillRoundRect(x, y, width, height, height, height);
                g2.setColor(config.badgeTextColor);
                g2.drawString(badgeText, x + (width - fm.stringWidth(badgeText)) / 2,
                        y + (height - fm.getHeight()) / 2 + fm.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }

    // ==================== COMBOBOX ====================

    public void setCoSoItems(List<CoSoItem> items) {

        DefaultComboBoxModel<CoSoItem> model =
                new DefaultComboBoxModel<>();

        for (CoSoItem item : items) {
            model.addElement(item);
        }

        cboCoSo.setModel(model);
    }

    public CoSoItem getSelectedCoSo() {

        return (CoSoItem)
                cboCoSo.getSelectedItem();
    }

    public void selectCoSo(int maCoSo) {

        for (int i = 0;
                i < cboCoSo.getItemCount();
                i++) {

            if (cboCoSo.getItemAt(i).maCoSo
                    == maCoSo) {

                cboCoSo.setSelectedIndex(i);
                return;
            }
        }
    }

    // ==================== VUNG HIEN THI ====================

    public void showPage(JComponent page) {

        pageArea.removeAll();

        if (page != null) {
            pageArea.add(
                    page,
                    BorderLayout.CENTER);
        }

        pageArea.revalidate();
        pageArea.repaint();
    }

    public JPanel createEmptyPage(
            String title,
            String message) {

        JPanel panel = new JPanel(
                new BorderLayout(0, 15));

        panel.setOpaque(false);

        JLabel heading = new JLabel(title);

        heading.setFont(new Font(
                config.fontName,
                Font.BOLD,
                23
        ));

        JPanel body = new JPanel(
                new GridBagLayout());

        body.setBackground(Color.WHITE);

        body.setBorder(
                BorderFactory.createLineBorder(
                        new Color(210, 210, 210)));

        JLabel detail = new JLabel(message);

        detail.setForeground(Color.GRAY);

        detail.setFont(new Font(
                config.fontName,
                Font.PLAIN,
                15
        ));

        body.add(detail);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);

        return panel;
    }

    public void setStatus(String message) {
        lblStatus.setText(message);
    }

    public void setNavigationEnabled(boolean enabled) {

        navigationEnabled = enabled;

        cboCoSo.setEnabled(enabled);

        for (JButton button : buttons.values()) {
            button.setEnabled(enabled);
        }
    }

    // ==================== PHAT SU KIEN ====================

    public void onMenuClick(ActionListener listener) {
        menuListeners.add(listener);
    }

    public void onCoSoChanged(ActionListener listener) {
        cboCoSo.addActionListener(listener);
    }

    private static String escapeHtml(String text) {

        if (text == null) return "";

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}