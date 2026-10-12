package pl.tomaszko.cheapskountant.expense.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pl.tomaszko.cheapskountant.expense.api.CategoryTotal;
import pl.tomaszko.cheapskountant.expense.api.DailyExpense;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {

    long countByPaymentDate(LocalDate paymentDate);

    boolean existsByReceiptItem_Id(Long receiptItemId);

    List<ExpenseEntity> findByReceiptItem_Receipt_IdOrderByReceiptItem_LineNoAsc(Long receiptId);

    @Query("""
            select new pl.tomaszko.cheapskountant.expense.api.CategoryTotal(c.name, e.currency, sum(e.amount))
            from ExpenseEntity e
            join e.category c
            where e.paymentDate >= :from and e.paymentDate <= :to
            group by c.id, c.name, e.currency
            """)
    List<CategoryTotal> summarize(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select new pl.tomaszko.cheapskountant.expense.api.DailyExpense(
                'expense', e.paymentDate, e.id, c.name, e.amount)
            from ExpenseEntity e
            join e.category c
            where e.paymentDate = :paymentDate and e.receiptItem is null
            order by e.id
            """)
    List<DailyExpense> findStandaloneByPaymentDate(@Param("paymentDate") LocalDate paymentDate);

    @Query("""
            select new pl.tomaszko.cheapskountant.expense.api.DailyExpense(
                'receipt', e.paymentDate, r.id, s.tradeName, sum(e.amount))
            from ExpenseEntity e
            join e.receiptItem item
            join item.receipt r
            join r.seller s
            where e.paymentDate = :paymentDate
            group by r.id, s.tradeName, e.paymentDate
            order by r.id
            """)
    List<DailyExpense> summarizeReceiptsByPaymentDate(@Param("paymentDate") LocalDate paymentDate);
}
