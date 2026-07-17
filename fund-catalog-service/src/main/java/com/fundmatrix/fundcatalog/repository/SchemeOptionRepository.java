package com.fundmatrix.fundcatalog.repository;

import com.fundmatrix.fundcatalog.domain.SchemeOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchemeOptionRepository extends JpaRepository<SchemeOption, Long> {

    List<SchemeOption> findByScheme_Id(Long schemeId);

    Optional<SchemeOption> findByIsinIgnoreCase(String isin);

    boolean existsByIsinIgnoreCase(String isin);
}
