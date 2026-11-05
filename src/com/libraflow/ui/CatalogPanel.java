package com.libraflow.ui;

import com.libraflow.model.Book;
import com.libraflow.model.BookCategory;
import com.libraflow.service.LibraryService;
import com.libraflow.util.IsbnLookupService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Optional;

/**
 * Interactive Book Catalog with instant search, category filter, dynamic Java 2D book cover preview,
 * ISBN-13 auto-lookup, and full CRUD management.
 */
public class CatalogPanel extends JPanel {
    private final LibraryService service;
    private final Runnable onDataChanged;

    private final JTextField txtSearch = Theme.createStyledTextField(22);
    private final JComboBox<String> cmbCategory = new JComboBox<>();
    private final JCheckBox chkAvailableOnly = new JCheckBox("Available Copies Only");

    private final DefaultTableModel tableModel;
    private final JTable table;

    private final BookCoverCanvas coverCanvas = new BookCoverCanvas();
    private final JLabel lblDetailTitle = new JLabel("Select a book");
    private final JLabel lblDetailAuthor = new JLabel("-");
    private final JLabel lblDetailMeta = new JLabel("-");
    private final JTextArea txtDetailDesc = new JTextArea(4, 20);

    public CatalogPanel(Runnable onDataChanged) {
        this.service = LibraryService.getInstance();
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(18, 18));
        setBackground(Theme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // 1. Top Filter & Action Toolbar
        Theme.CardPanel toolbar = new Theme.CardPanel(new BorderLayout(12, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFilters.setOpaque(false);
        leftFilters.add(new JLabel("Search:"));
        txtSearch.setToolTipText("Search by Title, Author, ISBN, Book ID, or Shelf Location");
        leftFilters.add(txtSearch);

        cmbCategory.addItem("All Categories");
        for (BookCategory cat : BookCategory.values()) {
            cmbCategory.addItem(cat.getDisplayName());
        }
        Theme.styleComboBox(cmbCategory);
        leftFilters.add(cmbCategory);

        chkAvailableOnly.setOpaque(false);
        chkAvailableOnly.setFont(Theme.FONT_BODY);
        leftFilters.add(chkAvailableOnly);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        JButton btnAdd = Theme.createPrimaryButton("+ Add Book / ISBN Lookup");
        JButton btnEdit = Theme.createSecondaryButton("Edit Selected");
        JButton btnDelete = Theme.createDangerButton("Delete");

        btnAdd.addActionListener(e -> openBookDialog(null));
        btnEdit.addActionListener(e -> {
            Book selected = getSelectedBook();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select a book from the table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            openBookDialog(selected);
        });
        btnDelete.addActionListener(e -> deleteSelectedBook());

        rightActions.add(btnAdd);
        rightActions.add(btnEdit);
        rightActions.add(btnDelete);

        toolbar.add(leftFilters, BorderLayout.WEST);
        toolbar.add(rightActions, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        // 2. Center Table
        String[] cols = {"ID", "ISBN-13", "Title", "Author", "Category", "Year", "Shelf", "Available / Total", "Rating"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(68);
        table.getColumnModel().getColumn(1).setPreferredWidth(115);
        table.getColumnModel().getColumn(2).setPreferredWidth(240);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        table.getColumnModel().getColumn(5).setPreferredWidth(55);
        table.getColumnModel().getColumn(6).setPreferredWidth(75);
        table.getColumnModel().getColumn(7).setPreferredWidth(105);
        table.getColumnModel().getColumn(8).setPreferredWidth(60);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Right Book Cover & Inspector Sidebar
        Theme.CardPanel inspector = new Theme.CardPanel(new BorderLayout(12, 12));
        inspector.setPreferredSize(new Dimension(285, 0));
        inspector.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        inspector.add(coverCanvas, BorderLayout.NORTH);

        JPanel metaPanel = new JPanel(new BorderLayout(6, 6));
        metaPanel.setOpaque(false);

        JPanel titlesBox = new JPanel(new GridBagLayout());
        titlesBox.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 2, 0);

        lblDetailTitle.setFont(Theme.FONT_SUBTITLE);
        lblDetailTitle.setForeground(Theme.TEXT_PRIMARY);
        titlesBox.add(lblDetailTitle, gbc);

        gbc.gridy++;
        lblDetailAuthor.setFont(Theme.FONT_BODY_BOLD);
        lblDetailAuthor.setForeground(Theme.PRIMARY);
        titlesBox.add(lblDetailAuthor, gbc);

        gbc.gridy++;
        lblDetailMeta.setFont(Theme.FONT_SMALL);
        lblDetailMeta.setForeground(Theme.TEXT_MUTED);
        titlesBox.add(lblDetailMeta, gbc);

        metaPanel.add(titlesBox, BorderLayout.NORTH);

        txtDetailDesc.setEditable(false);
        txtDetailDesc.setLineWrap(true);
        txtDetailDesc.setWrapStyleWord(true);
        txtDetailDesc.setFont(Theme.FONT_BODY);
        txtDetailDesc.setForeground(Theme.TEXT_SECONDARY);
        txtDetailDesc.setBackground(new Color(248, 250, 252));
        txtDetailDesc.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        metaPanel.add(new JScrollPane(txtDetailDesc), BorderLayout.CENTER);

        JButton btnReserveQuick = Theme.createStyledButton("Place Reservation / Hold", Theme.PURPLE, Color.WHITE);
        btnReserveQuick.addActionListener(e -> quickReserveSelectedBook());
        metaPanel.add(btnReserveQuick, BorderLayout.SOUTH);

        inspector.add(metaPanel, BorderLayout.CENTER);
        add(inspector, BorderLayout.EAST);

        // Listeners
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshData(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshData(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshData(); }
        });
        cmbCategory.addActionListener(e -> refreshData());
        chkAvailableOnly.addActionListener(e -> refreshData());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateInspector(getSelectedBook());
            }
        });

        refreshData();
    }

    public void refreshData() {
        BookCategory selectedCat = null;
        int catIdx = cmbCategory.getSelectedIndex();
        if (catIdx > 0) {
            selectedCat = BookCategory.values()[catIdx - 1];
        }

        List<Book> filtered = service.searchBooks(txtSearch.getText(), selectedCat, chkAvailableOnly.isSelected());
        tableModel.setRowCount(0);
        for (Book b : filtered) {
            tableModel.addRow(new Object[]{
                    b.getId(),
                    b.getIsbn(),
                    b.getTitle(),
                    b.getAuthor(),
                    b.getCategory().getDisplayName(),
                    b.getPublicationYear(),
                    b.getShelfLocation(),
                    b.getAvailableCopies() + " / " + b.getTotalCopies() + (b.isAvailable() ? " (In Stock)" : " (Out)"),
                    "★ " + b.getRating()
            });
        }
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        } else {
            updateInspector(null);
        }
    }

    private Book getSelectedBook() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        String id = (String) tableModel.getValueAt(row, 0);
        return service.findBookById(id).orElse(null);
    }

    private void updateInspector(Book book) {
        coverCanvas.setBook(book);
        if (book == null) {
            lblDetailTitle.setText("No Book Selected");
            lblDetailAuthor.setText("-");
            lblDetailMeta.setText("-");
            txtDetailDesc.setText("Select any title from the catalog table to inspect its cover, synopsis, and shelf metadata.");
            return;
        }
        lblDetailTitle.setText("<html><body style='width:195px'>" + book.getTitle() + "</body></html>");
        lblDetailAuthor.setText(book.getAuthor());
        lblDetailMeta.setText(String.format("%s • %d • Shelf %s • %d Borrows",
                book.getPublisher(), book.getPublicationYear(), book.getShelfLocation(), book.getTotalBorrowsCount()));
        txtDetailDesc.setText(book.getDescription());
    }

    private void quickReserveSelectedBook() {
        Book selected = getSelectedBook();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a book first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (service.getCurrentUser() == null) return;
        try {
            var res = service.placeReservation(selected.getId(), service.getCurrentUser().getId());
            JOptionPane.showMessageDialog(this,
                    "Reservation placed! Reference ID: " + res.getReservationId()
                            + "\nStatus: " + res.getStatus(),
                    "Reservation Confirmed", JOptionPane.INFORMATION_MESSAGE);
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Reservation Notice", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteSelectedBook() {
        if (!service.hasStaffPrivileges()) {
            JOptionPane.showMessageDialog(this, "Only Administrators and Librarians can delete catalog entries.",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Book selected = getSelectedBook();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a book to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to permanently remove '" + selected.getTitle() + "' from the catalog?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                service.deleteBook(selected.getId());
                refreshData();
                onDataChanged.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Delete Book", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openBookDialog(Book existing) {
        if (!service.hasStaffPrivileges()) {
            JOptionPane.showMessageDialog(this, "Only Administrators and Librarians can add or edit catalog books.",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, existing == null ? "Add New Book (with ISBN Auto-Fill)" : "Edit Book Metadata", true);
        dialog.setSize(540, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField tfIsbn = Theme.createStyledTextField(16);
        JButton btnLookup = Theme.createSecondaryButton("Auto-Fill ISBN");
        JTextField tfTitle = Theme.createStyledTextField(22);
        JTextField tfAuthor = Theme.createStyledTextField(22);
        JTextField tfPublisher = Theme.createStyledTextField(18);
        JSpinner spYear = new JSpinner(new SpinnerNumberModel(2024, 1900, 2030, 1));
        JComboBox<BookCategory> cbCat = new JComboBox<>(BookCategory.values());
        JSpinner spCopies = new JSpinner(new SpinnerNumberModel(4, 1, 100, 1));
        JTextField tfShelf = Theme.createStyledTextField(10);
        JSpinner spRating = new JSpinner(new SpinnerNumberModel(4.8, 1.0, 5.0, 0.1));
        JTextArea taDesc = new JTextArea(3, 22);
        taDesc.setLineWrap(true);
        taDesc.setWrapStyleWord(true);
        taDesc.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR));

        if (existing != null) {
            tfIsbn.setText(existing.getIsbn());
            tfTitle.setText(existing.getTitle());
            tfAuthor.setText(existing.getAuthor());
            tfPublisher.setText(existing.getPublisher());
            spYear.setValue(existing.getPublicationYear());
            cbCat.setSelectedItem(existing.getCategory());
            spCopies.setValue(existing.getTotalCopies());
            tfShelf.setText(existing.getShelfLocation());
            spRating.setValue(existing.getRating());
            taDesc.setText(existing.getDescription());
        } else {
            tfIsbn.setText("978-0134685991");
            tfShelf.setText("CS-108-A");
        }

        btnLookup.addActionListener(e -> {
            Optional<IsbnLookupService.BookMetadata> metaOpt = IsbnLookupService.lookup(tfIsbn.getText());
            if (metaOpt.isPresent()) {
                var m = metaOpt.get();
                tfTitle.setText(m.title());
                tfAuthor.setText(m.author());
                tfPublisher.setText(m.publisher());
                spYear.setValue(m.year());
                cbCat.setSelectedItem(m.category());
                tfShelf.setText(m.category().getShelfPrefix() + "-A");
                spRating.setValue(m.rating());
                taDesc.setText(m.description());
                JOptionPane.showMessageDialog(dialog, "Bibliographic metadata loaded for ISBN " + m.isbn(),
                        "ISBN Lookup Hit", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(dialog,
                        "ISBN not in offline preset index. Try 978-0132350884, 978-0134685991, or 978-1449373320, or fill fields manually.",
                        "Manual Entry", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        form.add(new JLabel("ISBN-13:"), gbc);
        JPanel isbnRow = new JPanel(new BorderLayout(6, 0));
        isbnRow.setOpaque(false);
        isbnRow.add(tfIsbn, BorderLayout.CENTER);
        isbnRow.add(btnLookup, BorderLayout.EAST);
        gbc.gridx = 1; gbc.weightx = 0.75;
        form.add(isbnRow, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        form.add(new JLabel("Book Title:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        form.add(tfTitle, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Author(s):"), gbc);
        gbc.gridx = 1;
        form.add(tfAuthor, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Publisher:"), gbc);
        gbc.gridx = 1;
        form.add(tfPublisher, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        form.add(cbCat, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Publication Year:"), gbc);
        gbc.gridx = 1;
        form.add(spYear, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Total Copies:"), gbc);
        gbc.gridx = 1;
        form.add(spCopies, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Shelf Location:"), gbc);
        gbc.gridx = 1;
        form.add(tfShelf, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Editorial Rating:"), gbc);
        gbc.gridx = 1;
        form.add(spRating, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Synopsis:"), gbc);
        gbc.gridx = 1;
        form.add(new JScrollPane(taDesc), gbc);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = Theme.createSecondaryButton("Cancel");
        JButton btnSave = Theme.createPrimaryButton("Save Book");
        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            if (tfTitle.getText().isBlank() || tfAuthor.getText().isBlank()) {
                JOptionPane.showMessageDialog(dialog, "Title and Author are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (existing == null) {
                service.addBook(
                        tfIsbn.getText().trim(),
                        tfTitle.getText().trim(),
                        tfAuthor.getText().trim(),
                        tfPublisher.getText().trim(),
                        (Integer) spYear.getValue(),
                        (BookCategory) cbCat.getSelectedItem(),
                        (Integer) spCopies.getValue(),
                        tfShelf.getText().trim(),
                        taDesc.getText().trim(),
                        ((Number) spRating.getValue()).doubleValue()
                );
            } else {
                int diff = (Integer) spCopies.getValue() - existing.getTotalCopies();
                existing.setIsbn(tfIsbn.getText().trim());
                existing.setTitle(tfTitle.getText().trim());
                existing.setAuthor(tfAuthor.getText().trim());
                existing.setPublisher(tfPublisher.getText().trim());
                existing.setPublicationYear((Integer) spYear.getValue());
                existing.setCategory((BookCategory) cbCat.getSelectedItem());
                existing.setTotalCopies((Integer) spCopies.getValue());
                existing.setAvailableCopies(Math.max(0, existing.getAvailableCopies() + diff));
                existing.setShelfLocation(tfShelf.getText().trim());
                existing.setRating(((Number) spRating.getValue()).doubleValue());
                existing.setDescription(taDesc.getText().trim());
                service.updateBook(existing);
            }
            dialog.dispose();
            refreshData();
            onDataChanged.run();
        });

        footer.add(btnCancel);
        footer.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    /**
     * Dynamically renders an aesthetic hardcover book jacket preview using Java 2D gradients.
     */
    private static class BookCoverCanvas extends JPanel {
        private Book book;

        BookCoverCanvas() {
            setPreferredSize(new Dimension(240, 215));
            setOpaque(false);
        }

        void setBook(Book book) {
            this.book = book;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = 152;
            int h = 198;
            int x = (getWidth() - w) / 2;
            int y = 6;

            // Drop shadow
            g2.setColor(new Color(15, 23, 42, 35));
            g2.fillRoundRect(x + 6, y + 6, w, h, 12, 12);

            Color c1 = Theme.PRIMARY;
            Color c2 = Theme.SIDEBAR_BG;
            if (book != null) {
                switch (book.getCategory()) {
                    case COMPUTER_SCIENCE -> { c1 = new Color(37, 99, 235); c2 = new Color(30, 58, 138); }
                    case SOFTWARE_ENGINEERING -> { c1 = new Color(13, 148, 136); c2 = new Color(17, 94, 89); }
                    case DATA_SCIENCE -> { c1 = new Color(124, 58, 237); c2 = new Color(76, 29, 149); }
                    case MATHEMATICS -> { c1 = new Color(217, 119, 6); c2 = new Color(146, 64, 14); }
                    case FICTION -> { c1 = new Color(225, 29, 72); c2 = new Color(136, 19, 55); }
                    default -> { c1 = new Color(71, 85, 105); c2 = new Color(15, 23, 42); }
                }
            }

            g2.setPaint(new GradientPaint(x, y, c1, x + w, y + h, c2));
            g2.fillRoundRect(x, y, w, h, 12, 12);

            // Book spine crease highlight
            g2.setColor(new Color(255, 255, 255, 45));
            g2.fillRect(x + 12, y, 3, h);

            if (book != null) {
                g2.setColor(new Color(255, 255, 255, 190));
                g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                g2.drawString(book.getCategory().getShelfPrefix(), x + 22, y + 24);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                drawWrappedText(g2, book.getTitle(), x + 22, y + 48, w - 32, 4);

                g2.setColor(new Color(226, 232, 240));
                g2.setFont(new Font("SansSerif", Font.ITALIC, 10));
                drawWrappedText(g2, book.getAuthor(), x + 22, y + h - 36, w - 32, 2);

                // Availability pill on cover
                Color badgeBg = book.isAvailable() ? Theme.SUCCESS : Theme.DANGER;
                g2.setColor(badgeBg);
                g2.fillRoundRect(x + 22, y + h - 22, 86, 15, 6, 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                g2.drawString(book.isAvailable() ? "AVAILABLE (" + book.getAvailableCopies() + ")" : "WAITLIST ONLY", x + 28, y + h - 11);
            }

            g2.dispose();
        }

        private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth, int maxLines) {
            FontMetrics fm = g2.getFontMetrics();
            String[] words = text.split("\\s+");
            StringBuilder line = new StringBuilder();
            int linesDrawn = 0;
            for (String word : words) {
                if (fm.stringWidth(line + word) < maxWidth) {
                    line.append(word).append(" ");
                } else {
                    g2.drawString(line.toString().trim(), x, y);
                    y += fm.getHeight();
                    linesDrawn++;
                    if (linesDrawn >= maxLines - 1) {
                        line = new StringBuilder(word + "...");
                        break;
                    }
                    line = new StringBuilder(word + " ");
                }
            }
            if (!line.isEmpty() && linesDrawn < maxLines) {
                g2.drawString(line.toString().trim(), x, y);
            }
        }
    }
}
