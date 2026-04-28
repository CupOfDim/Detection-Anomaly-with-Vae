package com.natyrov.spring.detection_with_vae.repository;

import com.natyrov.spring.detection_with_vae.entity.Dataset;
import com.natyrov.spring.detection_with_vae.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    List<Dataset> findByOwnerOrderByUploadTimeDesc(User owner);
    Optional<Dataset> findByOwnerAndId(User owner, Long id);
}