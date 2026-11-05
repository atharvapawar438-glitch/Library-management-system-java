package com.libraflow.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a member reservation / waitlist entry when a book has 0 available copies.
 */
public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ReservationStatus {
        PENDING,
        NOTIFIED_READY,
        FULFILLED,
        CANCELLED
    }

    private String reservationId;
    private String bookId;
    private String bookTitle;
    private String memberId;
    private String memberName;
    private LocalDateTime createdAt;
    private ReservationStatus status;

    public Reservation(String reservationId, String bookId, String bookTitle,
                       String memberId, String memberName) {
        this.reservationId = reservationId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.memberId = memberId;
        this.memberName = memberName;
        this.createdAt = LocalDateTime.now();
        this.status = ReservationStatus.PENDING;
    }

    public String getReservationId() { return reservationId; }
    public String getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
}
