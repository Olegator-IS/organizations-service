package com.placeandplay.organizationservice.repository;

import com.placeandplay.organizationservice.model.Organizations;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organizations, Long> {
//    boolean existsByEmail(String email);
//    boolean existsByPhone(String phone);
}