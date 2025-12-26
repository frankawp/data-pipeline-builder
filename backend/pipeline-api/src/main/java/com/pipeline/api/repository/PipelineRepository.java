package com.pipeline.api.repository;

import com.pipeline.api.entity.PipelineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PipelineRepository extends JpaRepository<PipelineEntity, String> {

    List<PipelineEntity> findByStatus(String status);

    List<PipelineEntity> findByNameContaining(String name);
}
