# 💰 Expense Tracker

A desktop Expense Tracker built with Java Swing and SQLite database.

## Features
- Add income and expense transactions
- Category-based organization
- Filter by All / Income / Expense
- Delete transactions
- Live bar chart showing spending by category
- Dashboard with total income, expenses and balance
- Data stored in SQLite database using JDBC

## Tech Stack
- Java 17+
- Java Swing (GUI)
- SQLite (Database)
- JDBC (Java Database Connectivity)
- Java2D (Custom bar chart)

## How to Run
1. Clone the repository
2. Download sqlite-jdbc jar from https://github.com/xerial/sqlite-jdbc/releases
3. Add it to your project as an external library in IntelliJ
4. Run `ExpenseApp.java`

## Project Structure
- `Transaction.java` — data model with Type enum
- `DatabaseManager.java` — all SQL operations (INSERT, SELECT, DELETE)
- `ExpenseApp.java` — GUI layer with Java Swing and Java2D chart
