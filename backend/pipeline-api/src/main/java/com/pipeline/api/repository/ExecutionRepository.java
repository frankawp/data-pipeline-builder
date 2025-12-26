package com.pipeline.api.repository;

import com.pipeline.api.entity.ExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionRepository extends JpaRepository<ExecutionEntity, String> {

    List<ExecutionEntity> findByPipelineIdOrderByStartTimeDesc(String pipelineId);

    List<ExecutionEntity> findByStatus(String status);
}
