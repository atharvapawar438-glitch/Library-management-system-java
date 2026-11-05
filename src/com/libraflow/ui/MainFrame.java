package com.libraflow.ui;

import com.libraflow.model.User;
import com.libraflow.service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Enterprise Application Window with Dark Navy Sidebar navigation, Top Header Bar,
 * and CardLayout workspace views.
 */
public class MainFrame extends JFrame {
    private final LibraryService service;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentArea = new JPanel(cardLayout);

    private final DashboardPanel dashboardPanel;
    private final CatalogPanel catalogPanel;
    private final CirculationPanel circulationPanel;
    private final MembersPanel membersPanel;
    private final ReportsPanel reportsPanel;

    private final JLabel lblPageTitle = new JLabel("Executive Analytics Dashboard");
    private final List<JButton> navButtons = new ArrayList<>();

    public MainFrame(User loggedInUser) {
        super("LibraFlow Pro — Advanced Library Management System (" + loggedInUser.getRole().getDisplayName() + ")");
        this.service = LibraryService.getInstance();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1340, 820);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Instantiate sub-panels with global refresh callback
        Runnable refreshAll = this::refreshAllPanels;
        this.dashboardPanel = new DashboardPanel();
        this.catalogPanel = new CatalogPanel(refreshAll);
        this.circulationPanel = new CirculationPanel(refreshAll);
        this.membersPanel = new MembersPanel(refreshAll);
        this.reportsPanel = new ReportsPanel(refreshAll);

        contentArea.add(dashboardPanel, "DASHBOARD");
        contentArea.add(catalogPanel, "CATALOG");
        contentArea.add(circulationPanel, "CIRCULATION");
        contentArea.add(membersPanel, "MEMBERS");
        contentArea.add(reportsPanel, "REPORTS");

        // 1. Left Dark Navy Sidebar
        JPanel sidebar = buildSidebar(loggedInUser);
        add(sidebar, BorderLayout.WEST);

        // 2. Right Main Area (Header + Content)
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setBackground(Theme.APP_BG);
        rightWrapper.add(buildTopHeader(loggedInUser), BorderLayout.NORTH);
        rightWrapper.add(contentArea, BorderLayout.CENTER);

        add(rightWrapper, BorderLayout.CENTER);
    }

    private JPanel buildSidebar(User user) {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 20, 16));

        JLabel brandTitle = new JLabel("📚 LibraFlow Pro");
        brandTitle.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 20));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandTitle.setBorder(BorderFactory.createEmptyBorder(0, 10, 4, 0));

        JLabel brandSub = new JLabel("Enterprise LMS v2.5");
        brandSub.setFont(Theme.FONT_SMALL);
        brandSub.setForeground(Theme.SIDEBAR_MUTED);
        brandSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandSub.setBorder(BorderFactory.createEmptyBorder(0, 10, 24, 0));

        sidebar.add(brandTitle);
        sidebar.add(brandSub);

        addNavButton(sidebar, "📊  Analytics Dashboard", "DASHBOARD", "Executive Analytics Dashboard", true);
        addNavButton(sidebar, "📖  Book Catalog & ISBN", "CATALOG", "Book Catalog, Search & ISBN Lookup", false);
        addNavButton(sidebar, "🔄  Circulation & Fines", "CIRCULATION", "Circulation Desk, Renewals & Waitlist", false);
        addNavButton(sidebar, "👥  Members & Staff", "MEMBERS", "Member & Staff Account Directory", false);
        addNavButton(sidebar, "📑  Reports & Audit Logs", "REPORTS", "CSV Exports & Immutable Audit Logs", false);

        sidebar.add(Box.createVerticalGlue());

        // User Profile Card at bottom of sidebar
        JPanel userBox = new JPanel(new BorderLayout(4, 4));
        userBox.setBackground(Theme.SIDEBAR_HOVER);
        userBox.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        userBox.setMaximumSize(new Dimension(220, 82));
        userBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblName = new JLabel(user.getFullName());
        lblName.setFont(Theme.FONT_BODY_BOLD);
        lblName.setForeground(Color.WHITE);

        JLabel lblRole = new JLabel(user.getRole().getDisplayName() + " • @" + user.getUsername());
        lblRole.setFont(Theme.FONT_SMALL);
        lblRole.setForeground(Theme.SIDEBAR_MUTED);

        JButton btnLogout = Theme.createDangerButton("Sign Out");
        btnLogout.setFont(Theme.FONT_SMALL.deriveFont(java.awt.Font.BOLD));
        btnLogout.addActionListener(e -> {
            service.logout();
            dispose();
            new LoginFrame(u -> new MainFrame(u).setVisible(true)).setVisible(true);
        });

        userBox.add(lblName, BorderLayout.NORTH);
        userBox.add(lblRole, BorderLayout.CENTER);
        userBox.add(btnLogout, BorderLayout.SOUTH);

        sidebar.add(userBox);
        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String label, String cardKey, String headerTitle, boolean activeInitially) {
        JButton btn = new JButton(label);
        btn.setFont(Theme.FONT_BODY_BOLD);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(220, 44));
        btn.setPreferredSize(new Dimension(220, 44));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        styleNavButton(btn, activeInitially);
        navButtons.add(btn);

        btn.addActionListener(e -> {
            for (JButton b : navButtons) {
                styleNavButton(b, b == btn);
            }
            lblPageTitle.setText(headerTitle);
            refreshAllPanels();
            cardLayout.show(contentArea, cardKey);
        });

        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(6));
    }

    private void styleNavButton(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(Theme.SIDEBAR_ACTIVE);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(Theme.SIDEBAR_BG);
            btn.setForeground(Theme.SIDEBAR_TEXT);
        }
    }

    private JPanel buildTopHeader(User user) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(16, 28, 16, 28)
        ));

        lblPageTitle.setFont(Theme.FONT_TITLE);
        lblPageTitle.setForeground(Theme.TEXT_PRIMARY);

        JPanel rightMeta = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightMeta.setOpaque(false);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"));
        JLabel lblDate = new JLabel("📅 " + dateStr);
        lblDate.setFont(Theme.FONT_BODY);
        lblDate.setForeground(Theme.TEXT_SECONDARY);

        JLabel roleBadge = Theme.createBadge(user.getRole().getDisplayName().toUpperCase(),
                new Color(219, 234, 254), Theme.PRIMARY_DARK);

        rightMeta.add(lblDate);
        rightMeta.add(roleBadge);

        header.add(lblPageTitle, BorderLayout.WEST);
        header.add(rightMeta, BorderLayout.EAST);
        return header;
    }

    private void refreshAllPanels() {
        dashboardPanel.refreshData();
        catalogPanel.refreshData();
        circulationPanel.refreshData();
        membersPanel.refreshData();
        reportsPanel.refreshData();
    }
}
