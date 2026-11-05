package com.libraflow.ui;

import com.libraflow.model.Book;
import com.libraflow.model.BookCategory;
import com.libraflow.model.LoanTransaction;
import com.libraflow.service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.util.List;
import java.util.Map;

/**
 * Executive Dashboard featuring real-time KPI summary cards and custom Java 2D vector charts
 * (Category Distribution Bar Chart & Loan Health Donut Chart).
 */
public class DashboardPanel extends JPanel {
    private final LibraryService service;

    private final JLabel lblTotalBooksVal = new JLabel("0");
    private final JLabel lblTotalBooksSub = new JLabel("");
    private final JLabel lblActiveLoansVal = new JLabel("0");
    private final JLabel lblActiveLoansSub = new JLabel("");
    private final JLabel lblOverdueVal = new JLabel("0");
    private final JLabel lblOverdueSub = new JLabel("");
    private final JLabel lblMembersVal = new JLabel("0");
    private final JLabel lblMembersSub = new JLabel("");

    private final CategoryBarChartPanel barChartPanel = new CategoryBarChartPanel();
    private final CirculationDonutPanel donutChartPanel = new CirculationDonutPanel();

    public DashboardPanel() {
        this.service = LibraryService.getInstance();
        setLayout(new BorderLayout(20, 20));
        setBackground(Theme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Top KPI Row (4 cards)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 18, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.add(createKpiCard("TOTAL CATALOG COPIES", lblTotalBooksVal, lblTotalBooksSub, Theme.PRIMARY));
        kpiGrid.add(createKpiCard("ACTIVE CIRCULATION", lblActiveLoansVal, lblActiveLoansSub, Theme.SUCCESS));
        kpiGrid.add(createKpiCard("OVERDUE & FINES", lblOverdueVal, lblOverdueSub, Theme.DANGER));
        kpiGrid.add(createKpiCard("REGISTERED ACCOUNTS", lblMembersVal, lblMembersSub, Theme.PURPLE));

        add(kpiGrid, BorderLayout.NORTH);

        // Center Charts Row
        JPanel chartsGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        chartsGrid.setOpaque(false);

        Theme.CardPanel leftCard = new Theme.CardPanel(new BorderLayout(10, 10));
        leftCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel leftTitle = new JLabel("Inventory Distribution by Category & Top Borrowed Titles");
        leftTitle.setFont(Theme.FONT_SUBTITLE);
        leftTitle.setForeground(Theme.TEXT_PRIMARY);
        leftCard.add(leftTitle, BorderLayout.NORTH);
        leftCard.add(barChartPanel, BorderLayout.CENTER);

        Theme.CardPanel rightCard = new Theme.CardPanel(new BorderLayout(10, 10));
        rightCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel rightTitle = new JLabel("Circulation Health & Availability Breakdown");
        rightTitle.setFont(Theme.FONT_SUBTITLE);
        rightTitle.setForeground(Theme.TEXT_PRIMARY);
        rightCard.add(rightTitle, BorderLayout.NORTH);
        rightCard.add(donutChartPanel, BorderLayout.CENTER);

        chartsGrid.add(leftCard);
        chartsGrid.add(rightCard);

        add(chartsGrid, BorderLayout.CENTER);
        refreshData();
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, JLabel subtitleLabel, Color accentColor) {
        Theme.CardPanel card = new Theme.CardPanel(new BorderLayout(6, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, accentColor),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
        lblTitle.setForeground(Theme.TEXT_MUTED);

        valueLabel.setFont(Theme.FONT_KPI);
        valueLabel.setForeground(Theme.TEXT_PRIMARY);

        subtitleLabel.setFont(Theme.FONT_SMALL);
        subtitleLabel.setForeground(Theme.TEXT_SECONDARY);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(subtitleLabel, BorderLayout.SOUTH);
        return card;
    }

    public void refreshData() {
        List<Book> books = service.getAllBooks();
        List<LoanTransaction> txList = service.getAllTransactions();

        int totalCopies = books.stream().mapToInt(Book::getTotalCopies).sum();
        int availCopies = books.stream().mapToInt(Book::getAvailableCopies).sum();
        lblTotalBooksVal.setText(totalCopies + " Copies");
        lblTotalBooksSub.setText(books.size() + " Unique Titles (" + availCopies + " on shelf)");

        long activeCount = txList.stream().filter(t -> t.getReturnDate() == null).count();
        long returnedCount = txList.stream().filter(t -> t.getReturnDate() != null).count();
        lblActiveLoansVal.setText(String.valueOf(activeCount));
        lblActiveLoansSub.setText(returnedCount + " historical returns completed");

        long overdueCount = txList.stream().filter(t -> t.getStatus() == LoanTransaction.Status.OVERDUE).count();
        double totalFines = txList.stream().mapToDouble(LoanTransaction::calculateCurrentFine).sum();
        lblOverdueVal.setText(overdueCount + " Overdue");
        lblOverdueSub.setText(String.format("Accrued Fines: ₹%.2f (₹15/day)", totalFines));

        int totalUsers = service.getAllUsers().size();
        long pendingRes = service.getAllReservations().stream()
                .filter(r -> r.getStatus().name().startsWith("PENDING") || r.getStatus().name().startsWith("NOTIFIED"))
                .count();
        lblMembersVal.setText(totalUsers + " Users");
        lblMembersSub.setText(pendingRes + " active waitlist reservations");

        barChartPanel.repaint();
        donutChartPanel.repaint();
    }

    /**
     * Custom Java 2D Horizontal Bar Chart displaying Category Volume and Top Borrowed Books.
     */
    private class CategoryBarChartPanel extends JPanel {
        CategoryBarChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(420, 340));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Map<BookCategory, Integer> dist = service.getCategoryDistribution();
            int maxVal = dist.values().stream().mapToInt(Integer::intValue).max().orElse(10);
            if (maxVal == 0) maxVal = 1;

            int y = 22;
            int barMaxWidth = Math.max(120, getWidth() - 220);
            Color[] palette = {
                    Theme.PRIMARY, new Color(14, 165, 233), Theme.PURPLE,
                    Theme.SUCCESS, Theme.WARNING, new Color(236, 72, 153),
                    new Color(99, 102, 241), new Color(20, 184, 166)
            };

            int idx = 0;
            for (Map.Entry<BookCategory, Integer> entry : dist.entrySet()) {
                String label = entry.getKey().getDisplayName();
                int count = entry.getValue();

                g2.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
                g2.setColor(Theme.TEXT_SECONDARY);
                g2.drawString(label, 8, y + 14);

                int barX = 185;
                int barW = (int) ((count * 1.0 / maxVal) * barMaxWidth);
                g2.setColor(new Color(241, 245, 249));
                g2.fillRoundRect(barX, y, barMaxWidth, 18, 8, 8);

                g2.setColor(palette[idx % palette.length]);
                g2.fillRoundRect(barX, y, Math.max(6, barW), 18, 8, 8);

                g2.setColor(Theme.TEXT_PRIMARY);
                g2.drawString(count + " copies", barX + barMaxWidth + 10, y + 14);

                y += 30;
                idx++;
            }

            // Top Borrowed Section at the bottom
            y += 14;
            g2.setColor(Theme.BORDER_COLOR);
            g2.drawLine(8, y - 8, getWidth() - 16, y - 8);

            g2.setFont(Theme.FONT_BODY_BOLD);
            g2.setColor(Theme.TEXT_PRIMARY);
            g2.drawString("Most Popular Titles (All-Time Borrows):", 8, y + 10);
            y += 26;

            List<Book> topBooks = service.getTopBorrowedBooks(3);
            g2.setFont(Theme.FONT_SMALL);
            for (int i = 0; i < topBooks.size(); i++) {
                Book b = topBooks.get(i);
                g2.setColor(Theme.PRIMARY);
                g2.drawString("#" + (i + 1), 8, y);
                g2.setColor(Theme.TEXT_PRIMARY);
                String t = b.getTitle().length() > 42 ? b.getTitle().substring(0, 42) + "..." : b.getTitle();
                g2.drawString(t, 32, y);
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(b.getTotalBorrowsCount() + " loans  ★ " + b.getRating(), getWidth() - 115, y);
                y += 20;
            }

            g2.dispose();
        }
    }

    /**
     * Custom Java 2D Donut Chart visualizing On-Shelf vs Issued On-Time vs Overdue copies.
     */
    private class CirculationDonutPanel extends JPanel {
        CirculationDonutPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(420, 340));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            List<Book> books = service.getAllBooks();
            List<LoanTransaction> loans = service.getAllTransactions();

            int available = books.stream().mapToInt(Book::getAvailableCopies).sum();
            int overdue = (int) loans.stream().filter(t -> t.getStatus() == LoanTransaction.Status.OVERDUE).count();
            int issuedOnTime = (int) loans.stream().filter(t -> t.getStatus() == LoanTransaction.Status.ISSUED).count();
            int total = Math.max(1, available + overdue + issuedOnTime);

            int size = Math.min(getWidth() - 80, getHeight() - 120);
            size = Math.max(160, Math.min(size, 220));
            int cx = (getWidth() - size) / 2;
            int cy = 18;

            double angleAvail = (available * 360.0) / total;
            double angleIssued = (issuedOnTime * 360.0) / total;
            double angleOverdue = 360.0 - angleAvail - angleIssued;

            double start = 90;
            g2.setColor(Theme.SUCCESS);
            g2.fill(new Arc2D.Double(cx, cy, size, size, start, -angleAvail, Arc2D.PIE));
            start -= angleAvail;

            g2.setColor(Theme.PRIMARY);
            g2.fill(new Arc2D.Double(cx, cy, size, size, start, -angleIssued, Arc2D.PIE));
            start -= angleIssued;

            g2.setColor(Theme.DANGER);
            g2.fill(new Arc2D.Double(cx, cy, size, size, start, -angleOverdue, Arc2D.PIE));

            // Inner cutout for Donut look
            int innerSize = (int) (size * 0.62);
            int icx = cx + (size - innerSize) / 2;
            int icy = cy + (size - innerSize) / 2;
            g2.setColor(Color.WHITE);
            g2.fillOval(icx, icy, innerSize, innerSize);

            // Center metric text
            g2.setColor(Theme.TEXT_PRIMARY);
            g2.setFont(Theme.FONT_KPI);
            String centerNum = String.valueOf(total);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(centerNum, cx + (size - fm.stringWidth(centerNum)) / 2, cy + size / 2 + 4);

            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_MUTED);
            String sub = "Total Copies";
            FontMetrics fmSub = g2.getFontMetrics();
            g2.drawString(sub, cx + (size - fmSub.stringWidth(sub)) / 2, cy + size / 2 + 22);

            // Legend at the bottom
            int ly = cy + size + 30;
            int lx = 24;
            drawLegendItem(g2, lx, ly, Theme.SUCCESS, "Available on Shelf", available, total);
            drawLegendItem(g2, lx, ly + 26, Theme.PRIMARY, "Issued (On-Time)", issuedOnTime, total);
            drawLegendItem(g2, lx, ly + 52, Theme.DANGER, "Overdue (Accruing ₹15/day)", overdue, total);

            g2.dispose();
        }

        private void drawLegendItem(Graphics2D g2, int x, int y, Color color, String label, int count, int total) {
            g2.setColor(color);
            g2.fillRoundRect(x, y - 11, 14, 14, 4, 4);

            g2.setFont(Theme.FONT_BODY_BOLD);
            g2.setColor(Theme.TEXT_PRIMARY);
            g2.drawString(label, x + 24, y);

            int pct = (int) Math.round((count * 100.0) / total);
            g2.setFont(Theme.FONT_BODY);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString(count + " (" + pct + "%)", getWidth() - 105, y);
        }
    }
}
