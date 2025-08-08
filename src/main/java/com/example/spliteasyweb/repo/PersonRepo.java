package com.example.spliteasyweb.repo;

import com.example.spliteasyweb.model.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PersonRepo extends JpaRepository<PersonEntity, Long> {
  List<PersonEntity> findByGroupIdOrderByNameAsc(Long groupId);
  boolean existsByGroupIdAndName(Long groupId, String name);
  Optional<PersonEntity> findByGroupIdAndName(Long groupId, String name);

  @Modifying
  @Transactional
  void deleteByGroupId(Long groupId);
}
