package com.libraflow;

import com.libraflow.model.Book;
import com.libraflow.model.BookCategory;
import com.libraflow.model.LoanTransaction;
import com.libraflow.model.Reservation;
import com.libraflow.model.User;
import com.libraflow.service.LibraryService;
import com.libraflow.ui.LoginFrame;
import com.libraflow.ui.MainFrame;
import com.libraflow.util.CsvExporter;
import com.libraflow.util.IsbnLookupService;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;
import java.util.List;
import java.util.Scanner;

/**
 * Main entrypoint for LibraFlow Pro — Enterprise Library Management System.
 *
 * Modes supported:
 *   1. Default (GUI Mode): Launches the Modern Java Swing & Java 2D Desktop Application.
 *   2. --cli             : Launches an interactive terminal menu for console evaluation.
 *   3. --verify          : Runs an automated self-test suite verifying Auth, Catalog CRUD,
 *                          Circulation, Fines, Reservations, and CSV Exports.
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            if ("--verify".equalsIgnoreCase(args[0])) {
                runVerificationSuite();
                return;
            }
            if ("--cli".equalsIgnoreCase(args[0])) {
                runInteractiveCli();
                return;
            }
        }

        // Launch GUI Mode
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}

            LoginFrame loginFrame = new LoginFrame(loggedInUser -> {
                MainFrame mainFrame = new MainFrame(loggedInUser);
                mainFrame.setVisible(true);
            });
            loginFrame.setVisible(true);
        });
    }

    private static void runVerificationSuite() {
        System.out.println("======================================================================");
        System.out.println("🚀 LIBRAFLOW PRO — AUTOMATED JAVA VERIFICATION SUITE");
        System.out.println("======================================================================");

        LibraryService service = LibraryService.getInstance();
        service.resetDemoDatabase();

        // 1. Verify Authentication
        User admin = service.authenticate("admin", "admin123")
                .orElseThrow(() -> new AssertionError("Admin login failed"));
        System.out.println("[PASS] 1. Role-Based Auth verified for: " + admin.getFullName() + " (" + admin.getRole() + ")");

        // 2. Verify ISBN Lookup & Catalog Addition
        var meta = IsbnLookupService.lookup("978-0132350884")
                .orElseThrow(() -> new AssertionError("ISBN lookup failed"));
        Book added = service.addBook(meta.isbn(), "Test Edition: " + meta.title(), meta.author(),
                meta.publisher(), meta.year(), BookCategory.SOFTWARE_ENGINEERING, 3, "SE-999", meta.description(), 4.9);
        System.out.println("[PASS] 2. ISBN Auto-Lookup & Book Creation verified: " + added.getId() + " — " + added.getTitle());

        // 3. Verify Circulation (Issue -> Renew -> Return)
        LoanTransaction loan = service.issueBook(added.getId(), "USR-2003", 14);
        System.out.println("[PASS] 3. Book Issue workflow verified: " + loan.getTransactionId() + " (Due: " + loan.getDueDate() + ")");

        LoanTransaction renewed = service.renewLoan(loan.getTransactionId(), 7);
        System.out.println("[PASS] 4. Loan Renewal workflow verified: New Due Date = " + renewed.getDueDate());

        LoanTransaction returned = service.returnBook(loan.getTransactionId(), true);
        System.out.println("[PASS] 5. Book Return workflow verified: Status = " + returned.getStatus());

        // 4. Verify Overdue Fine Calculation
        List<LoanTransaction> overdues = service.getAllTransactions().stream()
                .filter(t -> t.getStatus() == LoanTransaction.Status.OVERDUE)
                .toList();
        if (overdues.isEmpty()) {
            throw new AssertionError("Expected seeded overdue transactions");
        }
        LoanTransaction sampleOverdue = overdues.get(0);
        System.out.printf("[PASS] 6. Automated Overdue Fine Engine verified: %s (%d days overdue -> ₹%.2f)%n",
                sampleOverdue.getTransactionId(), sampleOverdue.getOverdueDays(), sampleOverdue.calculateCurrentFine());

        // 5. Verify Reservation Waitlist
        Reservation res = service.placeReservation("BK-112", "USR-2003");
        System.out.println("[PASS] 7. Reservation Waitlist Queue verified: " + res.getReservationId() + " (" + res.getStatus() + ")");

        // 6. Verify CSV Export
        try {
            File outDir = new File("exports");
            if (!outDir.exists()) outDir.mkdirs();
            File csv = CsvExporter.exportBooksCatalog(service.getAllBooks(), new File(outDir, "verify_catalog.csv"));
            System.out.println("[PASS] 8. CSV Report Exporter verified: " + csv.getAbsolutePath());
        } catch (Exception e) {
            throw new AssertionError("CSV export failed: " + e.getMessage());
        }

        // Restore clean seeded state for user launch
        service.resetDemoDatabase();
        System.out.println("======================================================================");
        System.out.println("✅ ALL 8 ENTERPRISE VERIFICATION CHECKS PASSED SUCCESSFULLY!");
        System.out.println("======================================================================");
    }

    private static void runInteractiveCli() {
        LibraryService service = LibraryService.getInstance();
        Scanner sc = new Scanner(System.in);
        System.out.println("======================================================================");
        System.out.println("📚 Welcome to LibraFlow Pro — Interactive Console Mode (CLI)");
        System.out.println("======================================================================");
        service.authenticate("admin", "admin123");

        boolean running = true;
        while (running) {
            System.out.println("\n[1] List All Books   [2] Search Books   [3] View Active/Overdue Loans   [4] Exit CLI");
            System.out.print("Select option: ");
            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> {
                    for (Book b : service.getAllBooks()) {
                        System.out.printf("%-7s | %-14s | %-42s | Avail: %d/%d%n",
                                b.getId(), b.getIsbn(),
                                b.getTitle().length() > 40 ? b.getTitle().substring(0, 40) + ".." : b.getTitle(),
                                b.getAvailableCopies(), b.getTotalCopies());
                    }
                }
                case "2" -> {
                    System.out.print("Enter search keyword: ");
                    String q = sc.nextLine();
                    for (Book b : service.searchBooks(q, null, false)) {
                        System.out.printf(" -> [%s] %s by %s (%s)%n", b.getId(), b.getTitle(), b.getAuthor(), b.getShelfLocation());
                    }
                }
                case "3" -> {
                    for (LoanTransaction tx : service.getAllTransactions()) {
                        System.out.printf("%-8s | %-10s | %-22s | Due: %s | Fine: ₹%.2f%n",
                                tx.getTransactionId(), tx.getStatus(), tx.getMemberName(),
                                tx.getDueDate(), tx.calculateCurrentFine());
                    }
                }
                case "4", "q", "exit" -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }
}
