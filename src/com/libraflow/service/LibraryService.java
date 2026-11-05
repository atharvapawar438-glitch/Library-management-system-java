package com.libraflow.service;

import com.libraflow.dao.DataStore;
import com.libraflow.model.AuditLog;
import com.libraflow.model.Book;
import com.libraflow.model.BookCategory;
import com.libraflow.model.LoanTransaction;
import com.libraflow.model.Reservation;
import com.libraflow.model.Role;
import com.libraflow.model.User;
import com.libraflow.util.PasswordHasher;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Central domain service coordinating authentication, catalog CRUD, loan workflows,
 * overdue fine computations, reservation queues, and audit logging.
 */
public class LibraryService {
    private static LibraryService instance;
    private final DataStore store;
    private User currentUser;

    private LibraryService() {
        this.store = DataStore.getInstance();
    }

    public static synchronized LibraryService getInstance() {
        if (instance == null) {
            instance = new LibraryService();
        }
        return instance;
    }

    // =========================================================================
    // 1. AUTHENTICATION & SESSION MANAGEMENT
    // =========================================================================

    public Optional<User> authenticate(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        String trimmedUser = username.trim().toLowerCase();
        for (User u : store.getUsers()) {
            if (u.getUsername().equalsIgnoreCase(trimmedUser) && u.isActive()) {
                if (PasswordHasher.verify(password, u.getPasswordHash())) {
                    this.currentUser = u;
                    recordAudit("USER_LOGIN", u.getFullName() + " signed in as " + u.getRole().getDisplayName());
                    return Optional.of(u);
                }
            }
        }
        return Optional.empty();
    }

    public void logout() {
        if (currentUser != null) {
            recordAudit("USER_LOGOUT", currentUser.getFullName() + " signed out");
            currentUser = null;
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean hasStaffPrivileges() {
        return currentUser != null && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.LIBRARIAN);
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    // =========================================================================
    // 2. BOOK CATALOG & ADVANCED SEARCH
    // =========================================================================

    public List<Book> getAllBooks() {
        return store.getBooks();
    }

    public List<Book> searchBooks(String query, BookCategory categoryFilter, boolean availableOnly) {
        String q = (query == null) ? "" : query.trim().toLowerCase();
        return store.getBooks().stream()
                .filter(b -> {
                    boolean matchesQuery = q.isEmpty()
                            || b.getTitle().toLowerCase().contains(q)
                            || b.getAuthor().toLowerCase().contains(q)
                            || b.getIsbn().toLowerCase().contains(q)
                            || b.getId().toLowerCase().contains(q)
                            || b.getShelfLocation().toLowerCase().contains(q);
                    boolean matchesCategory = (categoryFilter == null) || b.getCategory() == categoryFilter;
                    boolean matchesAvail = !availableOnly || b.isAvailable();
                    return matchesQuery && matchesCategory && matchesAvail;
                })
                .collect(Collectors.toList());
    }

    public Optional<Book> findBookById(String bookId) {
        return store.getBooks().stream()
                .filter(b -> b.getId().equalsIgnoreCase(bookId))
                .findFirst();
    }

    public Book addBook(String isbn, String title, String author, String publisher,
                        int year, BookCategory category, int totalCopies,
                        String shelfLocation, String description, double rating) {
        String nextId = String.format("BK-%03d", store.getBooks().size() + 101);
        Book newBook = new Book(nextId, isbn, title, author, publisher, year,
                category, totalCopies, totalCopies, shelfLocation, description, rating);
        store.getBooks().add(0, newBook);
        recordAudit("BOOK_CREATED", "Added '" + title + "' (" + nextId + ") with " + totalCopies + " copies");
        store.save();
        return newBook;
    }

    public void updateBook(Book updated) {
        recordAudit("BOOK_UPDATED", "Updated catalog metadata for '" + updated.getTitle() + "' (" + updated.getId() + ")");
        store.save();
    }

    public void deleteBook(String bookId) {
        Book book = findBookById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
        long activeLoans = store.getTransactions().stream()
                .filter(tx -> tx.getBookId().equals(bookId) && tx.getReturnDate() == null)
                .count();
        if (activeLoans > 0) {
            throw new IllegalStateException("Cannot delete '" + book.getTitle() + "' while " + activeLoans + " copies are currently issued.");
        }
        store.getBooks().remove(book);
        recordAudit("BOOK_DELETED", "Removed '" + book.getTitle() + "' (" + bookId + ") from catalog");
        store.save();
    }

    // =========================================================================
    // 3. CIRCULATION: ISSUE, RETURN, RENEW & FINES
    // =========================================================================

    public List<LoanTransaction> getAllTransactions() {
        return store.getTransactions();
    }

    public List<LoanTransaction> getActiveLoansForMember(String memberId) {
        return store.getTransactions().stream()
                .filter(tx -> tx.getMemberId().equals(memberId) && tx.getReturnDate() == null)
                .collect(Collectors.toList());
    }

    public LoanTransaction issueBook(String bookId, String memberId, int loanDays) {
        Book book = findBookById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
        User member = findUserById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        if (!member.isActive()) {
            throw new IllegalStateException("Member account '" + member.getFullName() + "' is currently suspended.");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("No copies of '" + book.getTitle() + "' are currently available. Please place a reservation instead.");
        }

        List<LoanTransaction> activeMemberLoans = getActiveLoansForMember(memberId);
        if (activeMemberLoans.size() >= member.getMaxBooksAllowed()) {
            throw new IllegalStateException(member.getFullName() + " has reached their maximum borrowing limit (" + member.getMaxBooksAllowed() + " books).");
        }

        boolean alreadyHasSameBook = activeMemberLoans.stream()
                .anyMatch(tx -> tx.getBookId().equals(bookId));
        if (alreadyHasSameBook) {
            throw new IllegalStateException(member.getFullName() + " already has an active copy of '" + book.getTitle() + "' checked out.");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        book.incrementBorrowsCount();

        String txId = String.format("TXN-%04d", 5001 + store.getTransactions().size());
        String staffName = (currentUser != null) ? currentUser.getFullName() : "Self-Checkout Terminal";
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(loanDays > 0 ? loanDays : book.getCategory().getDefaultLoanDays());

        LoanTransaction tx = new LoanTransaction(txId, book.getId(), book.getTitle(), book.getIsbn(),
                member.getId(), member.getFullName(), staffName, today, dueDate);

        // If member had a pending reservation for this book, mark it fulfilled
        store.getReservations().stream()
                .filter(r -> r.getBookId().equals(bookId) && r.getMemberId().equals(memberId)
                        && (r.getStatus() == Reservation.ReservationStatus.PENDING || r.getStatus() == Reservation.ReservationStatus.NOTIFIED_READY))
                .forEach(r -> r.setStatus(Reservation.ReservationStatus.FULFILLED));

        store.getTransactions().add(0, tx);
        recordAudit("BOOK_ISSUED", String.format("Issued '%s' to %s (Due: %s, Ref: %s)",
                book.getTitle(), member.getFullName(), dueDate, txId));
        store.save();
        return tx;
    }

    public LoanTransaction returnBook(String transactionId, boolean collectFineImmediately) {
        LoanTransaction tx = store.getTransactions().stream()
                .filter(t -> t.getTransactionId().equalsIgnoreCase(transactionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (tx.getReturnDate() != null) {
            throw new IllegalStateException("Transaction " + transactionId + " has already been returned on " + tx.getReturnDate());
        }

        double accruedFine = tx.calculateCurrentFine();
        tx.setReturnDate(LocalDate.now());
        tx.setFineAmount(accruedFine);
        tx.setFinePaid(accruedFine == 0.0 || collectFineImmediately);

        findBookById(tx.getBookId()).ifPresent(book -> {
            book.setAvailableCopies(Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1));

            // Check if any member is waiting in the reservation queue for this book
            store.getReservations().stream()
                    .filter(r -> r.getBookId().equals(book.getId()) && r.getStatus() == Reservation.ReservationStatus.PENDING)
                    .findFirst()
                    .ifPresent(nextInLine -> {
                        nextInLine.setStatus(Reservation.ReservationStatus.NOTIFIED_READY);
                        recordAudit("RESERVATION_READY", "Notified " + nextInLine.getMemberName()
                                + " that reserved book '" + book.getTitle() + "' is now available!");
                    });
        });

        String fineInfo = accruedFine > 0
                ? String.format(" (Overdue Fine: ₹%.2f - %s)", accruedFine, tx.isFinePaid() ? "PAID" : "UNPAID")
                : " (On Time - No Fine)";
        recordAudit("BOOK_RETURNED", "Returned '" + tx.getBookTitle() + "' from " + tx.getMemberName() + fineInfo);
        store.save();
        return tx;
    }

    public LoanTransaction renewLoan(String transactionId, int extraDays) {
        LoanTransaction tx = store.getTransactions().stream()
                .filter(t -> t.getTransactionId().equalsIgnoreCase(transactionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (tx.getReturnDate() != null) {
            throw new IllegalStateException("Cannot renew a loan that has already been returned.");
        }
        if (tx.getRenewalCount() >= 2) {
            throw new IllegalStateException("Maximum renewal limit (2 renewals) reached for transaction " + transactionId + ".");
        }
        if (tx.getStatus() == LoanTransaction.Status.OVERDUE) {
            throw new IllegalStateException("Cannot renew an overdue loan. Please return the book or settle the ₹"
                    + String.format("%.2f", tx.calculateCurrentFine()) + " fine first.");
        }

        boolean hasWaitlist = store.getReservations().stream()
                .anyMatch(r -> r.getBookId().equals(tx.getBookId()) && r.getStatus() == Reservation.ReservationStatus.PENDING);
        if (hasWaitlist) {
            throw new IllegalStateException("Cannot renew '" + tx.getBookTitle() + "' because another member is waiting in the reservation queue.");
        }

        tx.setDueDate(tx.getDueDate().plusDays(extraDays));
        tx.incrementRenewalCount();
        recordAudit("LOAN_RENEWED", "Extended due date for '" + tx.getBookTitle() + "' (" + tx.getMemberName() + ") to " + tx.getDueDate());
        store.save();
        return tx;
    }

    public void settleFine(String transactionId) {
        LoanTransaction tx = store.getTransactions().stream()
                .filter(t -> t.getTransactionId().equalsIgnoreCase(transactionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        double fine = tx.calculateCurrentFine();
        tx.setFineAmount(fine);
        tx.setFinePaid(true);
        recordAudit("FINE_SETTLED", String.format("Collected ₹%.2f fine for %s from %s", fine, transactionId, tx.getMemberName()));
        store.save();
    }

    // =========================================================================
    // 4. RESERVATION WAITLIST
    // =========================================================================

    public List<Reservation> getAllReservations() {
        return store.getReservations();
    }

    public Reservation placeReservation(String bookId, String memberId) {
        Book book = findBookById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
        User member = findUserById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        boolean alreadyReserved = store.getReservations().stream()
                .anyMatch(r -> r.getBookId().equals(bookId) && r.getMemberId().equals(memberId)
                        && (r.getStatus() == Reservation.ReservationStatus.PENDING || r.getStatus() == Reservation.ReservationStatus.NOTIFIED_READY));
        if (alreadyReserved) {
            throw new IllegalStateException(member.getFullName() + " already has an active reservation for '" + book.getTitle() + "'.");
        }

        String resId = String.format("RES-%03d", 301 + store.getReservations().size());
        Reservation res = new Reservation(resId, book.getId(), book.getTitle(), member.getId(), member.getFullName());
        if (book.getAvailableCopies() > 0) {
            res.setStatus(Reservation.ReservationStatus.NOTIFIED_READY);
        }
        store.getReservations().add(0, res);
        recordAudit("RESERVATION_PLACED", member.getFullName() + " reserved '" + book.getTitle() + "' (" + resId + ")");
        store.save();
        return res;
    }

    public void cancelReservation(String reservationId) {
        Reservation res = store.getReservations().stream()
                .filter(r -> r.getReservationId().equalsIgnoreCase(reservationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));
        res.setStatus(Reservation.ReservationStatus.CANCELLED);
        recordAudit("RESERVATION_CANCELLED", "Cancelled reservation " + reservationId + " for '" + res.getBookTitle() + "'");
        store.save();
    }

    // =========================================================================
    // 5. USER & MEMBER MANAGEMENT
    // =========================================================================

    public List<User> getAllUsers() {
        return store.getUsers();
    }

    public Optional<User> findUserById(String userId) {
        return store.getUsers().stream()
                .filter(u -> u.getId().equalsIgnoreCase(userId))
                .findFirst();
    }

    public User registerUser(String username, String rawPassword, String fullName,
                             String email, String phone, Role role, String tier, int maxBooks) {
        boolean exists = store.getUsers().stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username.trim()));
        if (exists) {
            throw new IllegalStateException("Username '" + username + "' is already taken.");
        }
        String prefix = (role == Role.MEMBER) ? "USR-20" : "USR-10";
        String id = prefix + String.format("%02d", store.getUsers().size() + 1);
        User newUser = new User(id, username.trim(), PasswordHasher.hash(rawPassword),
                fullName.trim(), email.trim(), phone.trim(), role, tier, maxBooks);
        store.getUsers().add(newUser);
        recordAudit("USER_REGISTERED", "Created " + role.getDisplayName() + " account for " + fullName + " (@" + username + ")");
        store.save();
        return newUser;
    }

    public void toggleUserStatus(String userId) {
        User u = findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (currentUser != null && u.getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You cannot suspend your own currently logged-in account.");
        }
        u.setActive(!u.isActive());
        recordAudit("USER_STATUS_CHANGED", (u.isActive() ? "Activated" : "Suspended") + " account for " + u.getFullName());
        store.save();
    }

    // =========================================================================
    // 6. ANALYTICS & AUDIT LOGGING
    // =========================================================================

    public Map<BookCategory, Integer> getCategoryDistribution() {
        Map<BookCategory, Integer> map = new EnumMap<>(BookCategory.class);
        for (BookCategory cat : BookCategory.values()) {
            map.put(cat, 0);
        }
        for (Book b : store.getBooks()) {
            map.put(b.getCategory(), map.getOrDefault(b.getCategory(), 0) + b.getTotalCopies());
        }
        return map;
    }

    public List<Book> getTopBorrowedBooks(int limit) {
        return store.getBooks().stream()
                .sorted(Comparator.comparingInt(Book::getTotalBorrowsCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<AuditLog> getAuditLogs() {
        return store.getAuditLogs();
    }

    public void recordAudit(String actionType, String details) {
        String actor = (currentUser != null) ? currentUser.getUsername() : "system";
        String role = (currentUser != null) ? currentUser.getRole().name() : "SYSTEM";
        String id = String.format("LOG-%03d", 101 + store.getAuditLogs().size());
        store.getAuditLogs().add(0, new AuditLog(id, actor, role, actionType, details));
        store.save();
    }

    public void resetDemoDatabase() {
        store.resetToFactoryDefaults();
        recordAudit("DATABASE_RESET", "Restored factory demo database snapshot");
    }
}
