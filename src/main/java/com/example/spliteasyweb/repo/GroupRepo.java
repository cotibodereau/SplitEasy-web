package com.example.spliteasyweb.repo;

import com.example.spliteasyweb.model.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepo extends JpaRepository<GroupEntity, Long> {
    Optional<GroupEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
