package com.example.WaffleBear.feater;

import com.example.WaffleBear.feater.model.Feater;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeaterRepository extends JpaRepository<Feater, Long> {
    @EntityGraph(attributePaths = {"user"})
    Optional<Feater> findByUser_Idx(Long userIdx);

    @EntityGraph(attributePaths = {"user"})
    List<Feater> findAllByUser_IdxIn(Collection<Long> userIdxs);
}
