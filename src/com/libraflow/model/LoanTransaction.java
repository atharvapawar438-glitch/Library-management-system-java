package com.libraflow.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Tracks a book issue/return circulation transaction, due dates, renewals, and automated late fines.
 */
public class LoanTransaction implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final double DAILY_FINE_RATE_INR = 15.0; // ₹15 per overdue day

    public enum Status {
        ISSUED,
        RETURNED,
        OVERDUE
    }

    private String transactionId;
    private String bookId;
    private String bookTitle;
    private String bookIsbn;
    private String memberId;
    private String memberName;
    private String issuedByStaff;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private int renewalCount;
    private double fineAmount;
    private boolean finePaid;
    private String notes;

    public LoanTransaction(String transactionId, String bookId, String bookTitle, String bookIsbn,
                           String memberId, String memberName, String issuedByStaff,
                           LocalDate issueDate, LocalDate dueDate) {
        this.transactionId = transactionId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookIsbn = bookIsbn;
        this.memberId = memberId;
        this.memberName = memberName;
        this.issuedByStaff = issuedByStaff;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = null;
        this.renewalCount = 0;
        this.fineAmount = 0.0;
        this.finePaid = true;
        this.notes = "";
    }

    /**
     * Dynamically computes the current status and accrued fine based on today's date or return date.
     */
    public Status getStatus() {
        if (returnDate != null) {
            return Status.RETURNED;
        }
        if (LocalDate.now().isAfter(dueDate)) {
            return Status.OVERDUE;
        }
        return Status.ISSUED;
    }

    /**
     * Calculates overdue days as of returnDate (if returned) or LocalDate.now() (if still active).
     */
    public long getOverdueDays() {
        LocalDate referenceDate = (returnDate != null) ? returnDate : LocalDate.now();
        if (referenceDate.isAfter(dueDate)) {
            return ChronoUnit.DAYS.between(dueDate, referenceDate);
        }
        return 0;
    }

    /**
     * Computes live fine amount in INR (₹).
     */
    public double calculateCurrentFine() {
        if (returnDate != null) {
            return fineAmount;
        }
        long overdueDays = getOverdueDays();
        return overdueDays > 0 ? overdueDays * DAILY_FINE_RATE_INR : 0.0;
    }

    public String getTransactionId() { return transactionId; }
    public String getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getBookIsbn() { return bookIsbn; }
    public String getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public String getIssuedByStaff() { return issuedByStaff; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public int getRenewalCount() { return renewalCount; }
    public void incrementRenewalCount() { this.renewalCount++; }
    public double getFineAmount() { return fineAmount; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }
    public boolean isFinePaid() { return finePaid; }
    public void setFinePaid(boolean finePaid) { this.finePaid = finePaid; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
