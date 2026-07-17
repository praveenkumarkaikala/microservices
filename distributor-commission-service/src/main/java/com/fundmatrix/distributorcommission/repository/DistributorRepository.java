package com.fundmatrix.distributorcommission.repository;

import com.fundmatrix.distributorcommission.domain.Distributor;
import com.fundmatrix.distributorcommission.domain.enums.DistributorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistributorRepository extends JpaRepository<Distributor, Long> {

    Optional<Distributor> findByArnNumberIgnoreCase(String arnNumber);

    /** Renamed from findByUser_Id: Distributor.user (OneToOne) became a plain userId column. */
    Optional<Distributor> findByUserId(Long userId);

    boolean existsByArnNumberIgnoreCase(String arnNumber);

    List<Distributor> findByStatus(DistributorStatus status);
}
