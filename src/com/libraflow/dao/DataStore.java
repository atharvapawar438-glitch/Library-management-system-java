package com.libraflow.dao;

import com.libraflow.model.AuditLog;
import com.libraflow.model.Book;
import com.libraflow.model.BookCategory;
import com.libraflow.model.LoanTransaction;
import com.libraflow.model.Reservation;
import com.libraflow.model.Role;
import com.libraflow.model.User;
import com.libraflow.util.PasswordHasher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe persistent repository backed by local serialized storage (data/libraflow.dat)
 * with automatic rich data seeding on first startup.
 */
public class DataStore {
    private static final String DATA_DIR = "data";
    private static final String DATA_FILE = "data/libraflow_db.ser";
    private static DataStore instance;

    public static class DatabaseSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<Book> books = new ArrayList<>();
        public List<User> users = new ArrayList<>();
        public List<LoanTransaction> transactions = new ArrayList<>();
        public List<Reservation> reservations = new ArrayList<>();
        public List<AuditLog> auditLogs = new ArrayList<>();
    }

    private DatabaseSnapshot db;

    private DataStore() {
        loadOrSeed();
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    private void loadOrSeed() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Object obj = ois.readObject();
                if (obj instanceof DatabaseSnapshot snapshot) {
                    this.db = snapshot;
                    return;
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not read existing datastore, re-seeding default dataset: " + e.getMessage());
            }
        }
        this.db = new DatabaseSnapshot();
        seedDefaultData();
        save();
    }

    public synchronized void save() {
        try {
            File dir = new File(DATA_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
                oos.writeObject(db);
            }
        } catch (IOException e) {
            System.err.println("Error saving database snapshot: " + e.getMessage());
        }
    }

    public synchronized void resetToFactoryDefaults() {
        this.db = new DatabaseSnapshot();
        seedDefaultData();
        save();
    }

    private void seedDefaultData() {
        // 1. Seed Users (Admin, Librarian, Members)
        User admin = new User("USR-1001", "admin", PasswordHasher.hash("admin123"),
                "Atharva Pawar (Chief Admin)", "admin@libraflow.edu", "+91 98765 43210",
                Role.ADMIN, "Staff Executive", 15);
        admin.setJoinedDate(LocalDate.now().minusMonths(18));

        User librarian = new User("USR-1002", "librarian", PasswordHasher.hash("lib123"),
                "Dr. Meera Deshmukh", "meera.lib@libraflow.edu", "+91 98234 56789",
                Role.LIBRARIAN, "Senior Librarian", 10);
        librarian.setJoinedDate(LocalDate.now().minusMonths(12));

        User member1 = new User("USR-2001", "member", PasswordHasher.hash("member123"),
                "Rohan Sharma", "rohan.sharma@student.edu", "+91 91234 56780",
                Role.MEMBER, "Premium Research", 6);
        member1.setJoinedDate(LocalDate.now().minusMonths(8));

        User member2 = new User("USR-2002", "ananya", PasswordHasher.hash("ananya123"),
                "Ananya Verma", "ananya.verma@student.edu", "+91 97654 32109",
                Role.MEMBER, "Faculty Scholar", 8);
        member2.setJoinedDate(LocalDate.now().minusMonths(6));

        User member3 = new User("USR-2003", "vikram", PasswordHasher.hash("vikram123"),
                "Vikramaditya Kulkarni", "vikram.k@student.edu", "+91 99887 76655",
                Role.MEMBER, "Standard Student", 4);
        member3.setJoinedDate(LocalDate.now().minusMonths(4));

        User member4 = new User("USR-2004", "priya", PasswordHasher.hash("priya123"),
                "Priya Nair", "priya.nair@student.edu", "+91 90112 23344",
                Role.MEMBER, "Premium Research", 6);
        member4.setJoinedDate(LocalDate.now().minusMonths(2));

        db.users.addAll(List.of(admin, librarian, member1, member2, member3, member4));

        // 2. Seed Books (12 rich titles across categories)
        Book b1 = new Book("BK-101", "978-0132350884", "Clean Code: A Handbook of Agile Software Craftsmanship",
                "Robert C. Martin", "Prentice Hall", 2008, BookCategory.SOFTWARE_ENGINEERING,
                5, 3, "SE-201-A", "Agile craftsmanship principles, naming, refactoring, and clean OOP patterns.", 4.9);
        b1.setTotalBorrowsCount(28);

        Book b2 = new Book("BK-102", "978-0134685991", "Effective Java (3rd Edition)",
                "Joshua Bloch", "Addison-Wesley", 2018, BookCategory.COMPUTER_SCIENCE,
                6, 5, "CS-104-B", "90 essential best practices for the Java platform, concurrency, generics, and streams.", 4.9);
        b2.setTotalBorrowsCount(34);

        Book b3 = new Book("BK-103", "978-0262046305", "Introduction to Algorithms (CLRS, 4th Ed.)",
                "Thomas H. Cormen et al.", "MIT Press", 2022, BookCategory.COMPUTER_SCIENCE,
                4, 3, "CS-101-A", "Comprehensive gold-standard reference on algorithms and data structures.", 4.8);
        b3.setTotalBorrowsCount(41);

        Book b4 = new Book("BK-104", "978-1449373320", "Designing Data-Intensive Applications",
                "Martin Kleppmann", "O'Reilly Media", 2017, BookCategory.DATA_SCIENCE,
                4, 2, "DS-301-C", "The big ideas behind reliable, scalable, and maintainable distributed systems.", 4.9);
        b4.setTotalBorrowsCount(25);

        Book b5 = new Book("BK-105", "978-0201633610", "Design Patterns: Elements of Reusable OO Software",
                "Erich Gamma et al. (GoF)", "Addison-Wesley", 1994, BookCategory.SOFTWARE_ENGINEERING,
                3, 3, "SE-205-B", "Foundational Gang of Four software design patterns catalog.", 4.7);
        b5.setTotalBorrowsCount(19);

        Book b6 = new Book("BK-106", "978-1098104030", "Hands-On Machine Learning with Scikit-Learn & TensorFlow",
                "Aurélien Géron", "O'Reilly Media", 2022, BookCategory.DATA_SCIENCE,
                5, 4, "DS-305-A", "Concepts, tools, and techniques to build intelligent ML systems.", 4.8);
        b6.setTotalBorrowsCount(31);

        Book b7 = new Book("BK-107", "978-0135957059", "The Pragmatic Programmer (20th Anniversary)",
                "David Thomas & Andrew Hunt", "Addison-Wesley", 2019, BookCategory.SOFTWARE_ENGINEERING,
                4, 4, "SE-202-A", "Your journey to mastery in modern software engineering.", 4.8);
        b7.setTotalBorrowsCount(22);

        Book b8 = new Book("BK-108", "978-0691170244", "How to Solve It: A New Aspect of Mathematical Method",
                "George Pólya", "Princeton University Press", 2014, BookCategory.MATHEMATICS,
                3, 3, "MT-402-A", "Classic mathematical heuristics and problem-solving methodology.", 4.6);
        b8.setTotalBorrowsCount(14);

        Book b9 = new Book("BK-109", "978-0735211292", "Atomic Habits",
                "James Clear", "Avery Publishing", 2018, BookCategory.BUSINESS,
                5, 4, "BZ-601-A", "Tiny changes, remarkable results: behavioural framework for productivity.", 4.8);
        b9.setTotalBorrowsCount(29);

        Book b10 = new Book("BK-110", "978-0441172719", "Dune (Deluxe Collector's Edition)",
                "Frank Herbert", "Ace Books", 1965, BookCategory.FICTION,
                3, 2, "FC-501-D", "Epic science fiction masterpiece set on the desert planet Arrakis.", 4.9);
        b10.setTotalBorrowsCount(26);

        Book b11 = new Book("BK-111", "978-0553380163", "A Brief History of Time",
                "Stephen Hawking", "Bantam Books", 1998, BookCategory.SCIENCE,
                3, 3, "SC-701-A", "From the Big Bang to Black Holes: accessible modern cosmology.", 4.7);
        b11.setTotalBorrowsCount(18);

        Book b12 = new Book("BK-112", "978-0062316097", "Sapiens: A Brief History of Humankind",
                "Yuval Noah Harari", "Harper", 2015, BookCategory.HISTORY,
                2, 0, "HS-801-B", "Exploration of cognitive, agricultural, and scientific revolutions.", 4.8);
        b12.setTotalBorrowsCount(36);

        db.books.addAll(List.of(b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12));

        // 3. Seed Realistic Circulation Transactions (Active, Overdue, and Returned)
        LocalDate today = LocalDate.now();

        // Overdue loan 1 (Rohan Sharma - Clean Code, 6 days overdue -> ₹90 fine)
        LoanTransaction tx1 = new LoanTransaction("TXN-5001", b1.getId(), b1.getTitle(), b1.getIsbn(),
                member1.getId(), member1.getFullName(), librarian.getFullName(),
                today.minusDays(20), today.minusDays(6));
        tx1.setFinePaid(false);

        // Overdue loan 2 (Vikramaditya - Designing Data-Intensive Apps, 4 days overdue -> ₹60 fine)
        LoanTransaction tx2 = new LoanTransaction("TXN-5002", b4.getId(), b4.getTitle(), b4.getIsbn(),
                member3.getId(), member3.getFullName(), librarian.getFullName(),
                today.minusDays(18), today.minusDays(4));
        tx2.setFinePaid(false);

        // Active loan 3 (Ananya Verma - Effective Java)
        LoanTransaction tx3 = new LoanTransaction("TXN-5003", b2.getId(), b2.getTitle(), b2.getIsbn(),
                member2.getId(), member2.getFullName(), admin.getFullName(),
                today.minusDays(5), today.plusDays(9));

        // Active loan 4 (Rohan Sharma - Intro to Algorithms)
        LoanTransaction tx4 = new LoanTransaction("TXN-5004", b3.getId(), b3.getTitle(), b3.getIsbn(),
                member1.getId(), member1.getFullName(), librarian.getFullName(),
                today.minusDays(3), today.plusDays(11));

        // Active loan 5 (Priya Nair - Hands-On Machine Learning)
        LoanTransaction tx5 = new LoanTransaction("TXN-5005", b6.getId(), b6.getTitle(), b6.getIsbn(),
                member4.getId(), member4.getFullName(), librarian.getFullName(),
                today.minusDays(7), today.plusDays(7));

        // Active loan 6 & 7 (Sapiens has 0 copies left so we can demonstrate Reservations)
        LoanTransaction tx6 = new LoanTransaction("TXN-5006", b12.getId(), b12.getTitle(), b12.getIsbn(),
                member2.getId(), member2.getFullName(), librarian.getFullName(),
                today.minusDays(10), today.plusDays(11));

        LoanTransaction tx7 = new LoanTransaction("TXN-5007", b12.getId(), b12.getTitle(), b12.getIsbn(),
                member4.getId(), member4.getFullName(), admin.getFullName(),
                today.minusDays(8), today.plusDays(13));

        // Active loan 8 (Clean Code copy 2)
        LoanTransaction tx8 = new LoanTransaction("TXN-5008", b1.getId(), b1.getTitle(), b1.getIsbn(),
                member4.getId(), member4.getFullName(), librarian.getFullName(),
                today.minusDays(2), today.plusDays(12));

        // Active loan 9 (DDIA copy 2)
        LoanTransaction tx9 = new LoanTransaction("TXN-5009", b4.getId(), b4.getTitle(), b4.getIsbn(),
                member2.getId(), member2.getFullName(), librarian.getFullName(),
                today.minusDays(4), today.plusDays(10));

        // Active loan 10 (Atomic Habits)
        LoanTransaction tx10 = new LoanTransaction("TXN-5010", b9.getId(), b9.getTitle(), b9.getIsbn(),
                member3.getId(), member3.getFullName(), librarian.getFullName(),
                today.minusDays(6), today.plusDays(8));

        // Active loan 11 (Dune)
        LoanTransaction tx11 = new LoanTransaction("TXN-5011", b10.getId(), b10.getTitle(), b10.getIsbn(),
                member1.getId(), member1.getFullName(), admin.getFullName(),
                today.minusDays(1), today.plusDays(13));

        // Historical Returned loan 12 (with paid fine)
        LoanTransaction tx12 = new LoanTransaction("TXN-5000", b5.getId(), b5.getTitle(), b5.getIsbn(),
                member1.getId(), member1.getFullName(), librarian.getFullName(),
                today.minusDays(30), today.minusDays(16));
        tx12.setReturnDate(today.minusDays(13));
        tx12.setFineAmount(45.0);
        tx12.setFinePaid(true);
        tx12.setNotes("Returned 3 days late; fine settled via UPI.");

        db.transactions.addAll(List.of(tx11, tx10, tx9, tx8, tx7, tx6, tx5, tx4, tx3, tx2, tx1, tx12));

        // 4. Seed Reservation Waitlist for Sapiens (BK-112, which has 0 available copies)
        Reservation res1 = new Reservation("RES-301", b12.getId(), b12.getTitle(),
                member1.getId(), member1.getFullName());
        db.reservations.add(res1);

        // 5. Seed Initial Audit Logs
        db.auditLogs.add(new AuditLog("LOG-104", "librarian", "LIBRARIAN", "BOOK_ISSUED",
                "Issued 'Dune (Deluxe Collector's Edition)' to Rohan Sharma (TXN-5011)"));
        db.auditLogs.add(new AuditLog("LOG-103", "member", "MEMBER", "RESERVATION_PLACED",
                "Rohan Sharma joined waitlist for 'Sapiens: A Brief History of Humankind' (RES-301)"));
        db.auditLogs.add(new AuditLog("LOG-102", "librarian", "LIBRARIAN", "FINE_SETTLED",
                "Collected ₹45.00 overdue fine for TXN-5000 from Rohan Sharma"));
        db.auditLogs.add(new AuditLog("LOG-101", "admin", "ADMIN", "SYSTEM_SEED",
                "Initialized LibraFlow Pro Enterprise Database with 12 catalog titles and 6 accounts"));
    }

    public List<Book> getBooks() { return db.books; }
    public List<User> getUsers() { return db.users; }
    public List<LoanTransaction> getTransactions() { return db.transactions; }
    public List<Reservation> getReservations() { return db.reservations; }
    public List<AuditLog> getAuditLogs() { return db.auditLogs; }
}
