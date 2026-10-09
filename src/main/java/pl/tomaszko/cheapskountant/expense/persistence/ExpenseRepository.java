package pl.tomaszko.cheapskountant.expense.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pl.tomaszko.cheapskountant.expense.api.CategoryTotal;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {

    long countByPaymentDate(LocalDate paymentDate);

    @Query("""
            select new pl.tomaszko.cheapskountant.expense.api.CategoryTotal(c.name, e.currency, sum(e.amount))
            from ExpenseEntity e
            join e.category c
            where e.paymentDate >= :from and e.paymentDate <= :to
            group by c.id, c.name, e.currency
            """)
    List<CategoryTotal> summarize(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
