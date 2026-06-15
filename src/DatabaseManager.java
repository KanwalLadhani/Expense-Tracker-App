import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

public class DatabaseManager {

    // This is the JDBC connection URL — jdbc:sqlite: tells Java to use SQLite
    private static final String URL = "jdbc:sqlite:expenses.db";
    private Connection conn;

    // ── Connect & create table ────────────────────────────────────────
    public void connect() {
        try {
            conn = DriverManager.getConnection(URL);
            createTable();
        } catch (SQLException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    public void disconnect() {
        try { if (conn != null) conn.close(); }
        catch (SQLException e) { System.out.println("Close error: " + e.getMessage()); }
    }

    // CREATE TABLE — runs only if table doesn't exist yet
    private void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS transactions (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                type        TEXT    NOT NULL,
                category    TEXT    NOT NULL,
                description TEXT,
                amount      REAL    NOT NULL,
                date        TEXT    NOT NULL
            )
            """;
        conn.createStatement().execute(sql);
    }

    // ── INSERT ────────────────────────────────────────────────────────
    public void addTransaction(Transaction.Type type, String category,
                               String description, double amount, LocalDate date) {
        // PreparedStatement prevents SQL injection — always use this, never string concat
        String sql = "INSERT INTO transactions (type, category, description, amount, date) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ps.setString(2, category);
            ps.setString(3, description);
            ps.setDouble(4, amount);
            ps.setString(5, date.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Insert error: " + e.getMessage());
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────
    public void deleteTransaction(int id) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Delete error: " + e.getMessage());
        }
    }

    // ── SELECT ALL ────────────────────────────────────────────────────
    public ArrayList<Transaction> getAllTransactions() {
        ArrayList<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY date DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"),
                        Transaction.Type.valueOf(rs.getString("type")),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getDouble("amount"),
                        LocalDate.parse(rs.getString("date"))
                ));
            }
        } catch (SQLException e) {
            System.out.println("Select error: " + e.getMessage());
        }
        return list;
    }

    // ── SELECT by type ────────────────────────────────────────────────
    public ArrayList<Transaction> getByType(Transaction.Type type) {
        ArrayList<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE type = ? ORDER BY date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"),
                        type,
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getDouble("amount"),
                        LocalDate.parse(rs.getString("date"))
                ));
            }
        } catch (SQLException e) {
            System.out.println("Select error: " + e.getMessage());
        }
        return list;
    }

    // ── Totals (SQL does the math) ────────────────────────────────────
    public double getTotal(Transaction.Type type) {
        String sql = "SELECT SUM(amount) FROM transactions WHERE type = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();
            return rs.getDouble(1);
        } catch (SQLException e) { return 0; }
    }

    // Category totals for chart — returns category -> total map
    public ArrayList<String[]> getCategoryTotals(Transaction.Type type) {
        ArrayList<String[]> list = new ArrayList<>();
        String sql = "SELECT category, SUM(amount) as total FROM transactions WHERE type = ? GROUP BY category ORDER BY total DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new String[]{rs.getString("category"), String.valueOf(rs.getDouble("total"))});
        } catch (SQLException e) {
            System.out.println("Category totals error: " + e.getMessage());
        }
        return list;
    }
}