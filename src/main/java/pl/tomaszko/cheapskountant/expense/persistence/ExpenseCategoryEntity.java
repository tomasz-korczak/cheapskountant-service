package pl.tomaszko.cheapskountant.expense.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense_category")
public class ExpenseCategoryEntity extends ExpenseTimestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    String name;

    protected ExpenseCategoryEntity() {
    }

    public ExpenseCategoryEntity(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }
}
