package com.libraflow.ui;

import com.libraflow.model.AuditLog;
import com.libraflow.service.LibraryService;
import com.libraflow.util.CsvExporter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;

/**
 * Provides one-click CSV / Executive Report exports and displays the immutable system Audit Trail.
 */
public class ReportsPanel extends JPanel {
    private final LibraryService service;
    private final Runnable onDataChanged;
    private final DefaultTableModel logModel;

    public ReportsPanel(Runnable onDataChanged) {
        this.service = LibraryService.getInstance();
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Export Bar
        Theme.CardPanel exportCard = new Theme.CardPanel(new BorderLayout(12, 12));
        exportCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel lblTitle = new JLabel("Data Export Suite & Security Audit Trail (Exports saved to ./exports/)");
        lblTitle.setFont(Theme.FONT_SUBTITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        exportCard.add(lblTitle, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);

        JButton btnExportBooks = Theme.createPrimaryButton("Export Books Catalog (.CSV)");
        JButton btnExportLoans = Theme.createSuccessButton("Export Loans & Fines Ledger (.CSV)");
        JButton btnExportMembers = Theme.createSecondaryButton("Export Members Directory (.CSV)");
        JButton btnExportExec = Theme.createStyledButton("Generate Executive Summary (.TXT)", Theme.PURPLE, java.awt.Color.WHITE);
        JButton btnResetDb = Theme.createDangerButton("Reset Demo Database");

        btnExportBooks.addActionListener(e -> runExport("catalog_export.csv", file ->
                CsvExporter.exportBooksCatalog(service.getAllBooks(), file)));
        btnExportLoans.addActionListener(e -> runExport("circulation_ledger.csv", file ->
                CsvExporter.exportLoanTransactions(service.getAllTransactions(), file)));
        btnExportMembers.addActionListener(e -> runExport("members_directory.csv", file ->
                CsvExporter.exportMembers(service.getAllUsers(), file)));
        btnExportExec.addActionListener(e -> runExport("executive_audit_report.txt", file ->
                CsvExporter.exportExecutiveReport(
                        service.getAllBooks(),
                        service.getAllUsers(),
                        service.getAllTransactions(),
                        service.getAuditLogs(),
                        file)));

        btnResetDb.addActionListener(e -> {
            if (!service.isAdmin()) {
                JOptionPane.showMessageDialog(this, "Only Administrators can reset the database snapshot.",
                        "Admin Required", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Reset all books, loans, and reservations back to factory demo defaults?",
                    "Confirm Factory Reset", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                service.resetDemoDatabase();
                refreshData();
                onDataChanged.run();
                JOptionPane.showMessageDialog(this, "Database restored to factory defaults.", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnRow.add(btnExportBooks);
        btnRow.add(btnExportLoans);
        btnRow.add(btnExportMembers);
        btnRow.add(btnExportExec);
        btnRow.add(btnResetDb);

        exportCard.add(btnRow, BorderLayout.CENTER);
        add(exportCard, BorderLayout.NORTH);

        // Audit Logs Table
        String[] cols = {"Log ID", "Timestamp", "Actor Username", "Role", "Event Type", "Audit Details"};
        logModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable logTable = new JTable(logModel);
        Theme.styleTable(logTable);
        logTable.getColumnModel().getColumn(0).setPreferredWidth(75);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(95);
        logTable.getColumnModel().getColumn(4).setPreferredWidth(145);
        logTable.getColumnModel().getColumn(5).setPreferredWidth(440);

        add(new JScrollPane(logTable), BorderLayout.CENTER);
        refreshData();
    }

    @FunctionalInterface
    private interface ExportAction {
        void execute(File target) throws Exception;
    }

    private void runExport(String fileName, ExportAction action) {
        try {
            File dir = new File("exports");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File target = new File(dir, fileName);
            action.execute(target);
            service.recordAudit("REPORT_EXPORTED", "Generated report file: " + target.getPath());
            refreshData();
            JOptionPane.showMessageDialog(this,
                    "Successfully exported file to:\n" + target.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshData() {
        logModel.setRowCount(0);
        List<AuditLog> logs = service.getAuditLogs();
        for (AuditLog log : logs) {
            logModel.addRow(new Object[]{
                    log.getId(),
                    log.getFormattedTimestamp(),
                    "@" + log.getActorUsername(),
                    log.getActorRole(),
                    log.getActionType(),
                    log.getDetails()
            });
        }
    }
}
