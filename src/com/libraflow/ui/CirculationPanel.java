package com.libraflow.ui;

import com.libraflow.model.Book;
import com.libraflow.model.LoanTransaction;
import com.libraflow.model.Reservation;
import com.libraflow.model.User;
import com.libraflow.service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manages Circulation Desk operations: Issuing books, Returning books, Renewing loans,
 * Automated Overdue Fine Collection (INR), and Reservation Queue fulfillment.
 */
public class CirculationPanel extends JPanel {
    private final LibraryService service;
    private final Runnable onDataChanged;

    private final JComboBox<String> cmbStatusFilter = new JComboBox<>(new String[]{
            "All Loans", "Active & Overdue Only", "Overdue Only", "Returned History"
    });
    private final DefaultTableModel loansModel;
    private final JTable loansTable;

    private final DefaultTableModel resModel;
    private final JTable resTable;

    public CirculationPanel(Runnable onDataChanged) {
        this.service = LibraryService.getInstance();
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Action Bar
        Theme.CardPanel topBar = new Theme.CardPanel(new BorderLayout(12, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel leftFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFilter.setOpaque(false);
        leftFilter.add(new JLabel("Filter Circulation:"));
        Theme.styleComboBox(cmbStatusFilter);
        leftFilter.add(cmbStatusFilter);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setOpaque(false);

        JButton btnIssue = Theme.createPrimaryButton("+ Issue Book to Member");
        JButton btnReturn = Theme.createSuccessButton("Return Selected Book");
        JButton btnRenew = Theme.createSecondaryButton("Renew (+7 Days)");
        JButton btnSettleFine = Theme.createDangerButton("Settle Fine (₹)");

        btnIssue.addActionListener(e -> openIssueDialog());
        btnReturn.addActionListener(e -> processReturn());
        btnRenew.addActionListener(e -> processRenewal());
        btnSettleFine.addActionListener(e -> processFineSettlement());

        rightButtons.add(btnIssue);
        rightButtons.add(btnReturn);
        rightButtons.add(btnRenew);
        rightButtons.add(btnSettleFine);

        topBar.add(leftFilter, BorderLayout.WEST);
        topBar.add(rightButtons, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Main Loans Table
        String[] loanCols = {
                "Txn ID", "Book ID", "Book Title", "Member Name",
                "Issue Date", "Due Date", "Return Date", "Status", "Renewals", "Fine (₹)", "Fine Status"
        };
        loansModel = new DefaultTableModel(loanCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        loansTable = new JTable(loansModel);
        loansTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Theme.styleTable(loansTable);

        JPanel loansContainer = new JPanel(new BorderLayout(0, 6));
        loansContainer.setOpaque(false);
        JLabel lblLoansHeader = new JLabel("Active & Historical Circulation Ledger (Overdue Rate: ₹15.00 / day)");
        lblLoansHeader.setFont(Theme.FONT_SUBTITLE);
        lblLoansHeader.setForeground(Theme.TEXT_PRIMARY);
        loansContainer.add(lblLoansHeader, BorderLayout.NORTH);
        loansContainer.add(new JScrollPane(loansTable), BorderLayout.CENTER);

        // Bottom Reservations Queue Table
        String[] resCols = {"Reservation ID", "Book ID", "Book Title", "Member ID", "Member Name", "Requested At", "Queue Status"};
        resModel = new DefaultTableModel(resCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resTable = new JTable(resModel);
        resTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Theme.styleTable(resTable);

        JPanel resContainer = new JPanel(new BorderLayout(0, 6));
        resContainer.setOpaque(false);

        JPanel resHeaderBar = new JPanel(new BorderLayout());
        resHeaderBar.setOpaque(false);
        JLabel lblResHeader = new JLabel("Hold & Waitlist Reservation Queue (Auto-Notifies Next Member Upon Book Return)");
        lblResHeader.setFont(Theme.FONT_SUBTITLE);
        lblResHeader.setForeground(Theme.TEXT_PRIMARY);

        JButton btnCancelRes = Theme.createSecondaryButton("Cancel Selected Reservation");
        btnCancelRes.addActionListener(e -> cancelSelectedReservation());

        resHeaderBar.add(lblResHeader, BorderLayout.WEST);
        resHeaderBar.add(btnCancelRes, BorderLayout.EAST);

        resContainer.add(resHeaderBar, BorderLayout.NORTH);
        resContainer.add(new JScrollPane(resTable), BorderLayout.CENTER);
        resContainer.setPreferredSize(new Dimension(0, 195));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, loansContainer, resContainer);
        splitPane.setResizeWeight(0.68);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        add(splitPane, BorderLayout.CENTER);

        cmbStatusFilter.addActionListener(e -> refreshData());
        refreshData();
    }

    public void refreshData() {
        int filterIdx = cmbStatusFilter.getSelectedIndex();
        List<LoanTransaction> allTx = service.getAllTransactions();

        loansModel.setRowCount(0);
        for (LoanTransaction tx : allTx) {
            LoanTransaction.Status st = tx.getStatus();
            if (filterIdx == 1 && st == LoanTransaction.Status.RETURNED) continue;
            if (filterIdx == 2 && st != LoanTransaction.Status.OVERDUE) continue;
            if (filterIdx == 3 && st != LoanTransaction.Status.RETURNED) continue;

            double fine = tx.calculateCurrentFine();
            String fineStatus = (fine == 0.0) ? "No Fine" : (tx.isFinePaid() ? "PAID" : "UNPAID (Due)");

            loansModel.addRow(new Object[]{
                    tx.getTransactionId(),
                    tx.getBookId(),
                    tx.getBookTitle(),
                    tx.getMemberName() + " (" + tx.getMemberId() + ")",
                    tx.getIssueDate(),
                    tx.getDueDate(),
                    tx.getReturnDate() != null ? tx.getReturnDate().toString() : "Active Loan",
                    st.name() + (tx.getOverdueDays() > 0 && st == LoanTransaction.Status.OVERDUE ? " (" + tx.getOverdueDays() + "d)" : ""),
                    tx.getRenewalCount() + " / 2",
                    String.format("₹%.2f", fine),
                    fineStatus
            });
        }

        resModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Reservation r : service.getAllReservations()) {
            resModel.addRow(new Object[]{
                    r.getReservationId(),
                    r.getBookId(),
                    r.getBookTitle(),
                    r.getMemberId(),
                    r.getMemberName(),
                    r.getCreatedAt().format(fmt),
                    r.getStatus().name()
            });
        }
    }

    private String getSelectedTransactionId() {
        int row = loansTable.getSelectedRow();
        if (row < 0) return null;
        return (String) loansModel.getValueAt(row, 0);
    }

    private void openIssueDialog() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Issue Book to Library Member", true);
        dialog.setSize(480, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> cbBook = new JComboBox<>();
        List<Book> books = service.getAllBooks();
        for (Book b : books) {
            cbBook.addItem(b.getId() + " — " + b.getTitle() + " (Avail: " + b.getAvailableCopies() + ")");
        }

        JComboBox<String> cbMember = new JComboBox<>();
        List<User> users = service.getAllUsers();
        for (User u : users) {
            if (u.isActive()) {
                cbMember.addItem(u.getId() + " — " + u.getFullName() + " [" + u.getMembershipTier() + "]");
            }
        }

        JSpinner spDays = new JSpinner(new SpinnerNumberModel(14, 1, 60, 1));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        form.add(new JLabel("Select Book:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(cbBook, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Select Member:"), gbc);
        gbc.gridx = 1;
        form.add(cbMember, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Loan Duration (Days):"), gbc);
        gbc.gridx = 1;
        form.add(spDays, gbc);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = Theme.createSecondaryButton("Cancel");
        JButton btnConfirm = Theme.createPrimaryButton("Confirm Issue");

        btnCancel.addActionListener(e -> dialog.dispose());
        btnConfirm.addActionListener(e -> {
            String bookItem = (String) cbBook.getSelectedItem();
            String memberItem = (String) cbMember.getSelectedItem();
            if (bookItem == null || memberItem == null) return;

            String bookId = bookItem.split(" — ")[0];
            String memberId = memberItem.split(" — ")[0];
            try {
                LoanTransaction tx = service.issueBook(bookId, memberId, (Integer) spDays.getValue());
                dialog.dispose();
                refreshData();
                onDataChanged.run();
                JOptionPane.showMessageDialog(this,
                        "Book issued successfully!\nTransaction ID: " + tx.getTransactionId() + "\nDue Date: " + tx.getDueDate(),
                        "Book Issued", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Cannot Issue Book", JOptionPane.ERROR_MESSAGE);
            }
        });

        footer.add(btnCancel);
        footer.add(btnConfirm);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void processReturn() {
        String txId = getSelectedTransactionId();
        if (txId == null) {
            JOptionPane.showMessageDialog(this, "Please select an active loan row to return.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            LoanTransaction tx = service.returnBook(txId, true);
            refreshData();
            onDataChanged.run();
            String msg = "Book '" + tx.getBookTitle() + "' returned successfully.";
            if (tx.getFineAmount() > 0) {
                msg += String.format("\nOverdue Fine Collected: ₹%.2f", tx.getFineAmount());
            }
            JOptionPane.showMessageDialog(this, msg, "Return Completed", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Return Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void processRenewal() {
        String txId = getSelectedTransactionId();
        if (txId == null) {
            JOptionPane.showMessageDialog(this, "Please select an active loan row to renew.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            LoanTransaction tx = service.renewLoan(txId, 7);
            refreshData();
            onDataChanged.run();
            JOptionPane.showMessageDialog(this,
                    "Loan renewed! New Due Date: " + tx.getDueDate() + " (Renewals used: " + tx.getRenewalCount() + "/2)",
                    "Loan Renewed", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Renewal Blocked", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void processFineSettlement() {
        String txId = getSelectedTransactionId();
        if (txId == null) {
            JOptionPane.showMessageDialog(this, "Please select a transaction with an unpaid fine.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            service.settleFine(txId);
            refreshData();
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Overdue fine settled and recorded in audit trail.",
                    "Fine Paid", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSelectedReservation() {
        int row = resTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a reservation from the bottom waitlist table.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String resId = (String) resModel.getValueAt(row, 0);
        try {
            service.cancelReservation(resId);
            refreshData();
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
