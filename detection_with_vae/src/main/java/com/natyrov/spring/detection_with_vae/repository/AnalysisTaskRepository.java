package com.natyrov.spring.detection_with_vae.repository;

import com.natyrov.spring.detection_with_vae.entity.AnalysisTask;
import com.natyrov.spring.detection_with_vae.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, Long> {
    List<AnalysisTask> findByOwnerOrderByCreatedAtDesc(User owner);
}