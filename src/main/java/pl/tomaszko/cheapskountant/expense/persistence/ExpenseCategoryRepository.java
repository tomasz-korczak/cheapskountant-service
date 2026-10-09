package pl.tomaszko.cheapskountant.expense.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategoryEntity, Long> {

    Optional<ExpenseCategoryEntity> findByName(String name);

    List<ExpenseCategoryEntity> findByNameNotOrderByIdAsc(String name);
}
