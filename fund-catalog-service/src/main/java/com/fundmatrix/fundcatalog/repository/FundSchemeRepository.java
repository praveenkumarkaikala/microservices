package com.fundmatrix.fundcatalog.repository;

import com.fundmatrix.fundcatalog.domain.FundScheme;
import com.fundmatrix.fundcatalog.domain.enums.SchemeCategory;
import com.fundmatrix.fundcatalog.domain.enums.SchemeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FundSchemeRepository extends JpaRepository<FundScheme, Long> {

    Optional<FundScheme> findBySchemeCodeIgnoreCase(String schemeCode);

    boolean existsBySchemeCodeIgnoreCase(String schemeCode);

    List<FundScheme> findByStatus(SchemeStatus status);

    List<FundScheme> findByCategory(SchemeCategory category);
}
