package com.libraflow.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a book title and its inventory state in the library catalog.
 */
public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private int publicationYear;
    private BookCategory category;
    private int totalCopies;
    private int availableCopies;
    private String shelfLocation;
    private String description;
    private double rating;
    private int totalBorrowsCount;
    private LocalDate addedDate;

    public Book(String id, String isbn, String title, String author, String publisher,
                int publicationYear, BookCategory category, int totalCopies,
                int availableCopies, String shelfLocation, String description, double rating) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.category = category;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.shelfLocation = shelfLocation;
        this.description = description;
        this.rating = rating;
        this.totalBorrowsCount = 0;
        this.addedDate = LocalDate.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public int getPublicationYear() { return publicationYear; }
    public void setPublicationYear(int publicationYear) { this.publicationYear = publicationYear; }

    public BookCategory getCategory() { return category; }
    public void setCategory(BookCategory category) { this.category = category; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getTotalBorrowsCount() { return totalBorrowsCount; }
    public void setTotalBorrowsCount(int totalBorrowsCount) { this.totalBorrowsCount = totalBorrowsCount; }
    public void incrementBorrowsCount() { this.totalBorrowsCount++; }

    public LocalDate getAddedDate() { return addedDate; }
    public void setAddedDate(LocalDate addedDate) { this.addedDate = addedDate; }

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book book)) return false;
        return Objects.equals(id, book.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
