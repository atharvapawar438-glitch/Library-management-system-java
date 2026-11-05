package com.libraflow.ui;

import com.libraflow.model.LoanTransaction;
import com.libraflow.model.Role;
import com.libraflow.model.User;
import com.libraflow.service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * User & Member Directory Panel supporting registration, role assignment,
 * borrowing limit governance, and account suspension/activation.
 */
public class MembersPanel extends JPanel {
    private final LibraryService service;
    private final Runnable onDataChanged;

    private final DefaultTableModel tableModel;
    private final JTable table;

    public MembersPanel(Runnable onDataChanged) {
        this.service = LibraryService.getInstance();
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Bar
        Theme.CardPanel topBar = new Theme.CardPanel(new BorderLayout(12, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Registered Members, Faculty & Staff Directory");
        title.setFont(Theme.FONT_SUBTITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnAddMember = Theme.createPrimaryButton("+ Register New Account");
        JButton btnToggleStatus = Theme.createSecondaryButton("Suspend / Activate Selected");

        btnAddMember.addActionListener(e -> openRegistrationDialog());
        btnToggleStatus.addActionListener(e -> toggleSelectedUser());

        actions.add(btnAddMember);
        actions.add(btnToggleStatus);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Table
        String[] cols = {
                "User ID", "Username", "Full Name", "Role", "Membership Tier",
                "Email", "Phone", "Active Loans / Max", "Joined Date", "Status"
        };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Theme.styleTable(table);

        add(new JScrollPane(table), BorderLayout.CENTER);
        refreshData();
    }

    public void refreshData() {
        tableModel.setRowCount(0);
        List<User> users = service.getAllUsers();
        for (User u : users) {
            List<LoanTransaction> activeLoans = service.getActiveLoansForMember(u.getId());
            tableModel.addRow(new Object[]{
                    u.getId(),
                    "@" + u.getUsername(),
                    u.getFullName(),
                    u.getRole().getDisplayName(),
                    u.getMembershipTier(),
                    u.getEmail(),
                    u.getPhone(),
                    activeLoans.size() + " / " + u.getMaxBooksAllowed(),
                    u.getJoinedDate(),
                    u.isActive() ? "ACTIVE" : "SUSPENDED"
            });
        }
    }

    private void toggleSelectedUser() {
        if (!service.hasStaffPrivileges()) {
            JOptionPane.showMessageDialog(this, "Only Staff (Admin/Librarian) can change account status.",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a user row first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String userId = (String) tableModel.getValueAt(row, 0);
        try {
            service.toggleUserStatus(userId);
            refreshData();
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Operation Blocked", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openRegistrationDialog() {
        if (!service.hasStaffPrivileges()) {
            JOptionPane.showMessageDialog(this, "Only Staff (Admin/Librarian) can register new accounts.",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Register New Library Account", true);
        dialog.setSize(460, 440);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField tfUsername = Theme.createStyledTextField(16);
        JPasswordField pfPassword = new JPasswordField("welcome123", 16);
        JTextField tfFullName = Theme.createStyledTextField(18);
        JTextField tfEmail = Theme.createStyledTextField(18);
        JTextField tfPhone = Theme.createStyledTextField(14);
        JComboBox<Role> cbRole = new JComboBox<>(Role.values());
        cbRole.setSelectedItem(Role.MEMBER);
        JComboBox<String> cbTier = new JComboBox<>(new String[]{
                "Standard Student", "Premium Research", "Faculty Scholar", "Senior Librarian", "Staff Executive"
        });
        JSpinner spMaxBooks = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));

        int r = 0;
        gbc.gridx = 0; gbc.gridy = r; gbc.weightx = 0.3;
        form.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(tfUsername, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r;
        form.add(new JLabel("Initial Password:"), gbc);
        gbc.gridx = 1;
        form.add(pfPassword, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r;
        form.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        form.add(tfFullName, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r;
        form.add(new JLabel("Email Address:"), gbc);
        gbc.gridx = 1;
        form.add(tfEmail, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r;
        form.add(new JLabel("Phone Number:"), gbc);
        gbc.gridx = 1;
        form.add(tfPhone, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r;
        form.add(new JLabel("System Role:"), gbc);
        gbc.gridx = 1;
        form.add(cbRole, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r;
        form.add(new JLabel("Membership Tier:"), gbc);
        gbc.gridx = 1;
        form.add(cbTier, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r;
        form.add(new JLabel("Max Books Allowed:"), gbc);
        gbc.gridx = 1;
        form.add(spMaxBooks, gbc);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = Theme.createSecondaryButton("Cancel");
        JButton btnSave = Theme.createPrimaryButton("Create Account");

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            if (tfUsername.getText().isBlank() || tfFullName.getText().isBlank()) {
                JOptionPane.showMessageDialog(dialog, "Username and Full Name are required.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                service.registerUser(
                        tfUsername.getText(),
                        new String(pfPassword.getPassword()),
                        tfFullName.getText(),
                        tfEmail.getText(),
                        tfPhone.getText(),
                        (Role) cbRole.getSelectedItem(),
                        (String) cbTier.getSelectedItem(),
                        (Integer) spMaxBooks.getValue()
                );
                dialog.dispose();
                refreshData();
                onDataChanged.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        footer.add(btnCancel);
        footer.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
