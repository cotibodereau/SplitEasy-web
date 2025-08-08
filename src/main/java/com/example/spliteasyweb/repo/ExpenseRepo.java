package com.example.spliteasyweb.repo;

import com.example.spliteasyweb.model.ExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ExpenseRepo extends JpaRepository<ExpenseEntity, Long> {
  List<ExpenseEntity> findByGroupIdOrderByDateDescIdDesc(Long groupId);

  @Modifying
  @Transactional
  void deleteByGroupId(Long groupId);
}
