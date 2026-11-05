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
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Modern split-screen Login Window with one-click demo role pills (Admin, Librarian, Member).
 */
public class LoginFrame extends JFrame {
    private final LibraryService service;
    private final Consumer<User> onLoginSuccess;

    private final JTextField txtUsername = Theme.createStyledTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JLabel lblStatus = new JLabel(" ");

    public LoginFrame(Consumer<User> onLoginSuccess) {
        super("LibraFlow Pro — Enterprise Library Management System");
        this.service = LibraryService.getInstance();
        this.onLoginSuccess = onLoginSuccess;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new GridLayout(1, 2));

        // Left Hero Branding Panel
        JPanel heroPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, Theme.SIDEBAR_BG, getWidth(), getHeight(), new Color(30, 58, 138)));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle decorative circles
                g2.setColor(new Color(255, 255, 255, 14));
                g2.fillOval(-60, -60, 260, 260);
                g2.fillOval(getWidth() - 180, getHeight() - 180, 280, 280);
                g2.dispose();
            }
        };
        heroPanel.setLayout(new BoxLayout(heroPanel, BoxLayout.Y_AXIS));
        heroPanel.setBorder(BorderFactory.createEmptyBorder(65, 45, 50, 45));

        JLabel badge = Theme.createBadge("JAVA SE • ENTERPRISE EDITION", Theme.PRIMARY, Color.WHITE);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heroTitle = new JLabel("<html><body style='width:320px'>LibraFlow Pro<br><span style='font-size:15px;font-weight:normal;color:#94A3B8'>Next-Gen Library Automation &amp; Circulation Analytics</span></body></html>");
        heroTitle.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 30));
        heroTitle.setForeground(Color.WHITE);
        heroTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel featuresList = new JLabel("<html><body style='width:330px;color:#CBD5E1;font-size:12px;line-height:1.7'>"
                + "✓ Role-Based Access (Admin, Librarian, Member)<br>"
                + "✓ Real-Time ISBN-13 Auto-Fill &amp; Dynamic Jacket Preview<br>"
                + "✓ Automated Overdue Fine Engine (₹15.00/day)<br>"
                + "✓ Waitlist Reservation Queue &amp; Instant Hold Alerts<br>"
                + "✓ Custom Java 2D Executive Analytics &amp; CSV Reports"
                + "</body></html>");
        featuresList.setAlignmentX(Component.LEFT_ALIGNMENT);

        heroPanel.add(badge);
        heroPanel.add(Box.createVerticalStrut(22));
        heroPanel.add(heroTitle);
        heroPanel.add(Box.createVerticalStrut(26));
        heroPanel.add(featuresList);
        heroPanel.add(Box.createVerticalGlue());

        JLabel footerCopy = new JLabel("Built with Pure Java Swing & Java 2D • Zero External Dependencies");
        footerCopy.setFont(Theme.FONT_SMALL);
        footerCopy.setForeground(Theme.SIDEBAR_MUTED);
        footerCopy.setAlignmentX(Component.LEFT_ALIGNMENT);
        heroPanel.add(footerCopy);

        // Right Login Form Panel
        JPanel formWrapper = new JPanel(new GridBagLayout());
        formWrapper.setBackground(Color.WHITE);

        JPanel formBox = new JPanel(new GridBagLayout());
        formBox.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);

        JLabel signInHeader = new JLabel("Sign In to Workspace");
        signInHeader.setFont(Theme.FONT_TITLE);
        signInHeader.setForeground(Theme.TEXT_PRIMARY);
        formBox.add(signInHeader, gbc);

        gbc.gridy++;
        JLabel signInSub = new JLabel("Enter your credentials or click a quick-login role below");
        signInSub.setFont(Theme.FONT_BODY);
        signInSub.setForeground(Theme.TEXT_MUTED);
        formBox.add(signInSub, gbc);

        gbc.gridy++;
        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(Theme.FONT_BODY_BOLD);
        formBox.add(lblUser, gbc);

        gbc.gridy++;
        txtUsername.setText("admin");
        formBox.add(txtUsername, gbc);

        gbc.gridy++;
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(Theme.FONT_BODY_BOLD);
        formBox.add(lblPass, gbc);

        gbc.gridy++;
        txtPassword.setText("admin123");
        txtPassword.setFont(Theme.FONT_BODY);
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
        formBox.add(txtPassword, gbc);

        gbc.gridy++;
        lblStatus.setFont(Theme.FONT_SMALL.deriveFont(java.awt.Font.BOLD));
        lblStatus.setForeground(Theme.DANGER);
        lblStatus.setHorizontalAlignment(SwingConstants.LEFT);
        formBox.add(lblStatus, gbc);

        gbc.gridy++;
        JButton btnLogin = Theme.createPrimaryButton("Sign In to LibraFlow Pro →");
        btnLogin.setPreferredSize(new Dimension(320, 42));
        btnLogin.addActionListener(e -> attemptLogin());
        formBox.add(btnLogin, gbc);

        // Quick-Login Demo Role Pills
        gbc.gridy++;
        JLabel lblQuick = new JLabel("One-Click Demo Credentials:");
        lblQuick.setFont(Theme.FONT_SMALL.deriveFont(java.awt.Font.BOLD));
        lblQuick.setForeground(Theme.TEXT_MUTED);
        formBox.add(lblQuick, gbc);

        gbc.gridy++;
        JPanel quickRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        quickRow.setOpaque(false);

        JButton btnAdminDemo = Theme.createSecondaryButton("Admin (admin)");
        JButton btnLibDemo = Theme.createSecondaryButton("Librarian (librarian)");
        JButton btnMemDemo = Theme.createSecondaryButton("Member (member)");

        btnAdminDemo.setFont(Theme.FONT_SMALL.deriveFont(java.awt.Font.BOLD));
        btnLibDemo.setFont(Theme.FONT_SMALL.deriveFont(java.awt.Font.BOLD));
        btnMemDemo.setFont(Theme.FONT_SMALL.deriveFont(java.awt.Font.BOLD));

        btnAdminDemo.addActionListener(e -> fillAndLogin("admin", "admin123"));
        btnLibDemo.addActionListener(e -> fillAndLogin("librarian", "lib123"));
        btnMemDemo.addActionListener(e -> fillAndLogin("member", "member123"));

        quickRow.add(btnAdminDemo);
        quickRow.add(btnLibDemo);
        quickRow.add(btnMemDemo);
        formBox.add(quickRow, gbc);

        formWrapper.add(formBox);

        add(heroPanel);
        add(formWrapper);
        getRootPane().setDefaultButton(btnLogin);
    }

    private void fillAndLogin(String user, String pass) {
        txtUsername.setText(user);
        txtPassword.setText(pass);
        attemptLogin();
    }

    private void attemptLogin() {
        String user = txtUsername.getText();
        String pass = new String(txtPassword.getPassword());
        Optional<User> auth = service.authenticate(user, pass);
        if (auth.isPresent()) {
            dispose();
            onLoginSuccess.accept(auth.get());
        } else {
            lblStatus.setText("Invalid username/password or suspended account.");
        }
    }
}
