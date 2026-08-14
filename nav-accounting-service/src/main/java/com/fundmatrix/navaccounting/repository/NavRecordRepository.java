package com.fundmatrix.navaccounting.repository;

import com.fundmatrix.navaccounting.domain.NavRecord;
import com.fundmatrix.navaccounting.domain.enums.NavStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NavRecordRepository extends JpaRepository<NavRecord, Long> {

    List<NavRecord> findBySchemeIdOrderByNavDateDesc(Long schemeId);

    List<NavRecord> findByOptionIdOrderByNavDateDesc(Long optionId);

    Optional<NavRecord> findByOptionIdAndNavDate(Long optionId, LocalDate navDate);

    /** Latest published NAV for a scheme option - the applicable NAV for allotment. */
    Optional<NavRecord> findTopByOptionIdAndStatusOrderByNavDateDesc(Long optionId, NavStatus status);

    Optional<NavRecord> findTopByOptionIdOrderByNavDateDesc(Long optionId);

    Optional<NavRecord> findTopBySchemeIdOrderByNavDateDesc(Long schemeId);

    /**
     * Distinct scheme ids that have at least one NAV record captured here. Used by
     * aumSummary() as the pragmatic substitute for the monolith's
     * {@code schemeRepository.findAll()} - see NavService for the full rationale.
     */
    @Query("select distinct n.schemeId from NavRecord n")
    List<Long> findDistinctSchemeIds();

    /**
     * Distinct option ids captured under a scheme. A scheme has multiple options (Growth,
     * Dividend Payout, Dividend Reinvestment), each carrying its own NAV/AUM - used by
     * aumSummary() to aggregate AUM across every option instead of a single scheme-wide record.
     */
    @Query("select distinct n.optionId from NavRecord n where n.schemeId = :schemeId")
    List<Long> findDistinctOptionIdsBySchemeId(@Param("schemeId") Long schemeId);
}
