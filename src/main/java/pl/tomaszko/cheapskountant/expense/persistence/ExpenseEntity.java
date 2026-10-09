package pl.tomaszko.cheapskountant.expense.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense")
public class ExpenseEntity extends ExpenseTimestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "amount", nullable = false, precision = 8, scale = 2)
    BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    LocalDate paymentDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    ExpenseCategoryEntity category;

    @Column(name = "currency", nullable = false, length = 3)
    String currency;

    @Column(name = "description", length = 100)
    String description;

    protected ExpenseEntity() {
    }

    public ExpenseEntity(
            BigDecimal amount,
            LocalDate paymentDate,
            ExpenseCategoryEntity category,
            String currency,
            String description) {
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.category = category;
        this.currency = currency;
        this.description = description;
    }

    public BigDecimal amount() {
        return amount;
    }

    public LocalDate paymentDate() {
        return paymentDate;
    }

    public ExpenseCategoryEntity category() {
        return category;
    }

    public String currency() {
        return currency;
    }

    public String description() {
        return description;
    }
}
