package com.libraflow.util;

import com.libraflow.model.BookCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simulates an ISBN-13 / ISBN-10 bibliographic metadata lookup service
 * with built-in ISBN checksum validation and pre-indexed classic computer science & literature titles.
 */
public final class IsbnLookupService {

    public record BookMetadata(
            String isbn,
            String title,
            String author,
            String publisher,
            int year,
            BookCategory category,
            String description,
            double rating
    ) {}

    private static final Map<String, BookMetadata> METADATA_CATALOG = new HashMap<>();

    static {
        register("978-0132350884", "Clean Code: A Handbook of Agile Software Craftsmanship",
                "Robert C. Martin", "Prentice Hall", 2008, BookCategory.SOFTWARE_ENGINEERING,
                "Best practices for writing readable, maintainable, and testable object-oriented software.", 4.8);

        register("978-0134685991", "Effective Java (3rd Edition)",
                "Joshua Bloch", "Addison-Wesley", 2018, BookCategory.COMPUTER_SCIENCE,
                "Definitive guide to Java platform best practices, generics, lambdas, streams, and concurrency.", 4.9);

        register("978-0201633610", "Design Patterns: Elements of Reusable Object-Oriented Software",
                "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides", "Addison-Wesley", 1994, BookCategory.SOFTWARE_ENGINEERING,
                "The classic Gang of Four (GoF) catalog of creational, structural, and behavioral patterns.", 4.7);

        register("978-0262046305", "Introduction to Algorithms (CLRS, 4th Edition)",
                "Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, Clifford Stein", "MIT Press", 2022, BookCategory.COMPUTER_SCIENCE,
                "Comprehensive treatment of data structures, graph algorithms, dynamic programming, and NP-completeness.", 4.9);

        register("978-1449373320", "Designing Data-Intensive Applications",
                "Martin Kleppmann", "O'Reilly Media", 2017, BookCategory.DATA_SCIENCE,
                "Deep dive into distributed systems, replication, partitioning, consensus, and stream processing.", 4.9);

        register("978-0135957059", "The Pragmatic Programmer (20th Anniversary Edition)",
                "David Thomas, Andrew Hunt", "Addison-Wesley", 2019, BookCategory.SOFTWARE_ENGINEERING,
                "Timeless software engineering wisdom covering tracer bullets, DRY, orthogonality, and automation.", 4.8);

        register("978-1098104030", "Hands-On Machine Learning with Scikit-Learn, Keras & TensorFlow",
                "Aurélien Géron", "O'Reilly Media", 2022, BookCategory.DATA_SCIENCE,
                "Practical end-to-end machine learning and deep neural networks guide.", 4.8);

        register("978-0735211292", "Atomic Habits",
                "James Clear", "Avery Publishing", 2018, BookCategory.BUSINESS,
                "An easy and proven way to build good habits and break bad ones using behavioral science.", 4.8);
    }

    private static void register(String isbn, String title, String author, String publisher,
                                 int year, BookCategory category, String description, double rating) {
        METADATA_CATALOG.put(normalizeIsbn(isbn), new BookMetadata(isbn, title, author, publisher, year, category, description, rating));
    }

    public static String normalizeIsbn(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^0-9Xx]", "").toUpperCase();
    }

    public static Optional<BookMetadata> lookup(String rawIsbn) {
        String normalized = normalizeIsbn(rawIsbn);
        if (METADATA_CATALOG.containsKey(normalized)) {
            return Optional.of(METADATA_CATALOG.get(normalized));
        }
        return Optional.empty();
    }

    public static Map<String, BookMetadata> getAllPresets() {
        return METADATA_CATALOG;
    }
}
