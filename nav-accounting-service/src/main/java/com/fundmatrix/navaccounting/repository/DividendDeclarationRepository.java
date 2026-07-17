package com.fundmatrix.navaccounting.repository;

import com.fundmatrix.navaccounting.domain.DividendDeclaration;
import com.fundmatrix.navaccounting.domain.enums.DividendStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DividendDeclarationRepository extends JpaRepository<DividendDeclaration, Long> {

    List<DividendDeclaration> findByStatus(DividendStatus status);

    List<DividendDeclaration> findByOptionIdOrderByRecordDateDesc(Long optionId);

    List<DividendDeclaration> findBySchemeIdOrderByRecordDateDesc(Long schemeId);
}
