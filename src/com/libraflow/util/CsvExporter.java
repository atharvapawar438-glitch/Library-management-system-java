package com.libraflow.util;

import com.libraflow.model.AuditLog;
import com.libraflow.model.Book;
import com.libraflow.model.LoanTransaction;
import com.libraflow.model.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports library catalog, circulation transactions, members, and audit logs to standard CSV files.
 */
public final class CsvExporter {

    private CsvExporter() {}

    public static File exportBooksCatalog(List<Book> books, File targetFile) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(targetFile))) {
            pw.println("Book ID,ISBN,Title,Author,Publisher,Year,Category,Total Copies,Available Copies,Shelf,Borrows Count,Rating");
            for (Book b : books) {
                pw.printf("%s,%s,%s,%s,%s,%d,%s,%d,%d,%s,%d,%.1f%n",
                        escape(b.getId()),
                        escape(b.getIsbn()),
                        escape(b.getTitle()),
                        escape(b.getAuthor()),
                        escape(b.getPublisher()),
                        b.getPublicationYear(),
                        escape(b.getCategory().getDisplayName()),
                        b.getTotalCopies(),
                        b.getAvailableCopies(),
                        escape(b.getShelfLocation()),
                        b.getTotalBorrowsCount(),
                        b.getRating()
                );
            }
        }
        return targetFile;
    }

    public static File exportLoanTransactions(List<LoanTransaction> transactions, File targetFile) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(targetFile))) {
            pw.println("Transaction ID,Book ID,ISBN,Book Title,Member ID,Member Name,Issue Date,Due Date,Return Date,Status,Renewals,Fine (INR),Fine Paid,Issued By");
            for (LoanTransaction tx : transactions) {
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d,%.2f,%s,%s%n",
                        escape(tx.getTransactionId()),
                        escape(tx.getBookId()),
                        escape(tx.getBookIsbn()),
                        escape(tx.getBookTitle()),
                        escape(tx.getMemberId()),
                        escape(tx.getMemberName()),
                        tx.getIssueDate(),
                        tx.getDueDate(),
                        tx.getReturnDate() != null ? tx.getReturnDate().toString() : "ACTIVE",
                        tx.getStatus().name(),
                        tx.getRenewalCount(),
                        tx.calculateCurrentFine(),
                        tx.isFinePaid() ? "YES" : "NO",
                        escape(tx.getIssuedByStaff())
                );
            }
        }
        return targetFile;
    }

    public static File exportMembers(List<User> users, File targetFile) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(targetFile))) {
            pw.println("User ID,Username,Full Name,Email,Phone,Role,Membership Tier,Max Books Allowed,Joined Date,Status");
            for (User u : users) {
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%d,%s,%s%n",
                        escape(u.getId()),
                        escape(u.getUsername()),
                        escape(u.getFullName()),
                        escape(u.getEmail()),
                        escape(u.getPhone()),
                        u.getRole().name(),
                        escape(u.getMembershipTier()),
                        u.getMaxBooksAllowed(),
                        u.getJoinedDate(),
                        u.isActive() ? "ACTIVE" : "SUSPENDED"
                );
            }
        }
        return targetFile;
    }

    public static File exportExecutiveReport(List<Book> books, List<User> users,
                                             List<LoanTransaction> loans, List<AuditLog> logs,
                                             File targetFile) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(targetFile))) {
            pw.println("================================================================================");
            pw.println("                 LIBRAFLOW PRO - EXECUTIVE AUDIT & ANALYTICS REPORT             ");
            pw.println("================================================================================");
            pw.println("Generated At : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pw.println("Total Titles : " + books.size());
            int totalCopies = books.stream().mapToInt(Book::getTotalCopies).sum();
            int availCopies = books.stream().mapToInt(Book::getAvailableCopies).sum();
            pw.println("Total Copies : " + totalCopies + " (Available: " + availCopies + ", Issued: " + (totalCopies - availCopies) + ")");
            pw.println("Total Users  : " + users.size());
            pw.println("Total Loans  : " + loans.size());
            long overdueCount = loans.stream().filter(l -> l.getStatus() == LoanTransaction.Status.OVERDUE).count();
            double totalFines = loans.stream().mapToDouble(LoanTransaction::calculateCurrentFine).sum();
            pw.printf("Overdue Loans: %d | Total Accrued Fines: ₹%.2f%n", overdueCount, totalFines);
            pw.println("--------------------------------------------------------------------------------");
            pw.println("RECENT AUDIT TRAIL (LAST 25 EVENTS):");
            int limit = Math.min(25, logs.size());
            for (int i = 0; i < limit; i++) {
                AuditLog log = logs.get(i);
                pw.printf("[%s] (%s - %s) %s: %s%n",
                        log.getFormattedTimestamp(), log.getActorUsername(), log.getActorRole(),
                        log.getActionType(), log.getDetails());
            }
            pw.println("================================================================================");
        }
        return targetFile;
    }

    private static String escape(String value) {
        if (value == null) return "";
        String cleaned = value.replace("\"", "\"\"");
        if (cleaned.contains(",") || cleaned.contains("\"") || cleaned.contains("\n")) {
            return "\"" + cleaned + "\"";
        }
        return cleaned;
    }
}
