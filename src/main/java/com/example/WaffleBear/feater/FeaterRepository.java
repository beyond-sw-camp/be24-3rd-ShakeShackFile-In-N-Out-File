package com.example.WaffleBear.feater;

import com.example.WaffleBear.feater.model.Feater;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeaterRepository extends JpaRepository<Feater, Long> {
    Optional<Feater> findByUser_Idx(Long userIdx);
}
