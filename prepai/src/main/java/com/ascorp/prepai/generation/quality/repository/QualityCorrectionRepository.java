package com.ascorp.prepai.generation.quality.repository;

import com.ascorp.prepai.generation.quality.model.entity.QualityCorrection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityCorrectionRepository extends JpaRepository<QualityCorrection, UUID> {
}
