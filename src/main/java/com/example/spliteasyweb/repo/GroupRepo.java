package com.example.spliteasyweb.repo;

import com.example.spliteasyweb.model.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepo extends JpaRepository<GroupEntity, Long> {
    Optional<GroupEntity> findBySlug(String slug);
    List<GroupEntity> findByOwnerSessionOrderByIdDesc(String ownerSession);
}
