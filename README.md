# 📚 LibraFlow Pro — Enterprise Library Management System (Java)

![Java](https://img.shields.io/badge/Java-17%2B%20%7C%2021%20%7C%2025%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![UI](https://img.shields.io/badge/Desktop%20UI-Java%20Swing%20%26%20Java%202D-2563EB?style=for-the-badge)
![Architecture](https://img.shields.io/badge/Architecture-MVC%20%2B%20DAO%20%2B%20Service-10B981?style=for-the-badge)
![Dependencies](https://img.shields.io/badge/External%20Dependencies-Zero%20(100%25%20Standard%20JDK)-7C3AED?style=for-the-badge)

**LibraFlow Pro** is a full-featured, production-grade **Library Management System** engineered in **Java** using clean **Object-Oriented Programming (OOP)** principles, a layered **Model–DAO–Service–View** architecture, custom **Java 2D vector data visualizations**, and a modern **Dark Navy & Slate Enterprise GUI**.

It runs out-of-the-box on any machine with a standard Java Development Kit (JDK 17, 21, or 25+) with **zero external Maven/Gradle dependencies or database server setup required**.

---

## ✨ Key Advanced Features

### 1. 🔐 Role-Based Access Control (RBAC) & Cryptographic Security
- **SHA-256 Salted Password Hashing** (`PasswordHasher.java`) for all user credentials.
- **3 Distinct Access Roles**:
  - **Administrator (`ADMIN`)**: Full system control, catalog CRUD, staff/member registration, and factory database reset.
  - **Librarian (`LIBRARIAN`)**: Catalog management, book issue/return/renewal desk, overdue fine collection, and waitlist administration.
  - **Member (`MEMBER`)**: Catalog browsing, instant multi-criteria search, book cover inspection, and hold/reservation placement.
- **One-Click Demo Role Switcher** on the split-screen Login Window for effortless evaluation and project demonstrations.

### 2. 📖 Interactive Book Catalog & ISBN-13 Auto-Lookup
- **Instant Multi-Criteria Search**: Real-time filtering by Title, Author, ISBN-13, Book ID, or Shelf Location, plus Category dropdowns and "Available Copies Only" toggle.
- **ISBN-13 Metadata Auto-Fill (`IsbnLookupService.java`)**: Enter an ISBN (e.g., `978-0134685991` or `978-1449373320`) and click **Auto-Fill ISBN** to automatically populate title, author, publisher, year, category, shelf code, rating, and synopsis.
- **Dynamic Java 2D Hardcover Jacket Preview**: Custom-rendered gradient book cover with spine highlights, shelf badge, and live stock status pill.

### 3. 🔄 Circulation Desk, Due Dates, Renewals & Automated Fine Engine
- **Issue Books**: Enforces member borrowing limits (e.g., 4 for Standard Students, 6 for Premium Research, 8 for Faculty) and blocks duplicate active checkouts of the same title.
- **Automated Overdue Fine Engine**: Dynamically computes overdue days and accrues fines at **₹15.00 / day** (`LoanTransaction.DAILY_FINE_RATE_INR`).
- **Loan Renewals**: Supports extending due dates by +7 days (up to 2 renewals per loan), automatically blocking renewal if a loan is overdue or if another member is waiting in the reservation queue.
- **Waitlist & Reservation Queue**: When a book has `0` available copies (e.g., *Sapiens*), members can join the waitlist. Returning a copy automatically transitions the next queued member to `NOTIFIED_READY` and logs an alert.

### 4. 📊 Executive Analytics Dashboard (Custom Java 2D Charts)
- **4 Live KPI Summary Cards**: Total Catalog Copies, Active Circulation, Overdue & Accrued Fines (`₹`), and Registered Accounts.
- **Horizontal Category Distribution Chart**: Custom anti-aliased Java 2D bar chart + Top Borrowed Titles leaderboard.
- **Circulation Health Donut Chart**: Real-time breakdown of *Available on Shelf* vs. *Issued (On-Time)* vs. *Overdue*.

### 5. 📑 CSV / Executive Report Exporter & Immutable Audit Trail
- One-click export to `./exports/`:
  - `catalog_export.csv` (Full Book Inventory)
  - `circulation_ledger.csv` (All Loans, Due Dates & Fines)
  - `members_directory.csv` (User & Member Roster)
  - `executive_audit_report.txt` (Formatted Executive Summary & Recent Security Audit Logs)
- **Immutable Audit Trail**: Every login, book creation, loan checkout, renewal, return, fine payment, and report export is timestamped and recorded with the actor's username and role.

---

## 🏗️ System Architecture & Package Structure

```text
library-management-system/
├── run.sh                                  # One-command compiler & launcher script (macOS / Linux)
├── README.md                               # Project documentation
├── .gitignore                              # Git ignore rules for compiled binaries & runtime files
└── src/
    └── com/
        └── libraflow/
            ├── Main.java                   # Entrypoint (Supports GUI, --cli, and --verify modes)
            ├── model/
            │   ├── Role.java               # Enum: ADMIN, LIBRARIAN, MEMBER
            │   ├── BookCategory.java       # Enum: Categories, shelf prefixes & default loan days
            │   ├── Book.java               # Entity: Catalog item & inventory counts
            │   ├── User.java               # Entity: Account credentials, role & borrowing tier
            │   ├── LoanTransaction.java    # Entity: Issue/Due/Return dates & ₹15/day fine engine
            │   ├── Reservation.java        # Entity: Hold/Waitlist queue item
            │   └── AuditLog.java           # Entity: Immutable timestamped security event
            ├── dao/
            │   └── DataStore.java          # Thread-safe serialized persistence & rich demo seeder
            ├── service/
            │   └── LibraryService.java     # Core business rules, RBAC, circulation & analytics
            ├── util/
            │   ├── PasswordHasher.java     # SHA-256 salted password hashing
            │   ├── IsbnLookupService.java  # ISBN-13 normalization & bibliographic auto-fill
            │   └── CsvExporter.java        # CSV & Executive TXT report generator
            └── ui/
                ├── Theme.java              # Design system: Slate/Navy palette, rounded cards, tables
                ├── LoginFrame.java         # Split-screen login UI with 1-click demo role buttons
                ├── MainFrame.java          # Sidebar navigation shell & header bar
                ├── DashboardPanel.java     # KPI cards & custom Java 2D Bar/Donut charts
                ├── CatalogPanel.java       # Search, filter, ISBN dialog & 2D book jacket canvas
                ├── CirculationPanel.java   # Issue, Return, Renew, Fine settlement & Waitlist tables
                ├── MembersPanel.java       # Member registration & account status governance
                └── ReportsPanel.java       # One-click CSV exports & live Audit Log viewer
```

---

## 🚀 Quick Start (Compile & Run)

### Option 1: Using the One-Command Launcher (`run.sh`)
Open your terminal in the `library-management-system` directory and run:

```bash
chmod +x run.sh
./run.sh
```

### Option 2: Manual `javac` & `java` Commands
```bash
mkdir -p bin
javac -d bin $(find src -name "*.java")
java -cp bin com.libraflow.Main
```

### Option 3: Run Automated Self-Verification Suite (`--verify`)
To verify all 8 enterprise subsystems (Authentication, ISBN Auto-Lookup, Issue, Renew, Return, Overdue Fine Engine, Waitlist Queue, and CSV Export) in headless console mode:

```bash
./run.sh --verify
```

### Option 4: Interactive Console / CLI Mode (`--cli`)
```bash
./run.sh --cli
```

---

## 🔑 Default Demo Credentials

The login screen includes **One-Click Demo Buttons** for all three roles, or you can type them manually:

| Role | Username | Password | Capabilities |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Full access: Catalog CRUD, Circulation, User Registration, DB Reset |
| **Librarian** | `librarian` | `lib123` | Staff access: Catalog CRUD, Issue/Return/Renew, Fine Collection |
| **Member** | `member` | `member123` | Member access: Browse Catalog, ISBN Preview, Place Reservations |

---

## 🌐 Pushing This Project to GitHub

1. Create a new empty repository on [GitHub](https://github.com/new) (e.g., `libraflow-pro-java` or `library-management-system`).
2. Run the following commands inside `/Users/atharvapawar/Ai/library-management-system`:

```bash
cd /Users/atharvapawar/Ai/library-management-system
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo-name>.git
git push -u origin main
```

---

## 📜 License

Designed and developed for academic excellence, software engineering demonstrations, and portfolio showcase.
