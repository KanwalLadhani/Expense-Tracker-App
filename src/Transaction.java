import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Transaction {

    // enum for transaction type — only INCOME or EXPENSE allowed
    public enum Type { INCOME, EXPENSE }

    private int       id;          // database primary key
    private Type      type;
    private String    category;
    private String    description;
    private double    amount;
    private LocalDate date;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Transaction(int id, Type type, String category, String description, double amount, LocalDate date) {
        this.id          = id;
        this.type        = type;
        this.category    = category;
        this.description = description;
        this.amount      = amount;
        this.date        = date;
    }

    // ── Getters ──────────────────────────────────────────────────────
    public int       getId()          { return id; }
    public Type      getType()        { return type; }
    public String    getCategory()    { return category; }
    public String    getDescription() { return description; }
    public double    getAmount()      { return amount; }
    public LocalDate getDate()        { return date; }
    public String    getDateStr()     { return date.format(FMT); }

    public boolean isIncome()  { return type == Type.INCOME; }
    public boolean isExpense() { return type == Type.EXPENSE; }
}