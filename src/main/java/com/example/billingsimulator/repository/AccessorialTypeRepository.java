package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.AccessorialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessorialTypeRepository extends JpaRepository<AccessorialType, String> {

    List<AccessorialType> findByCodeIn(List<String> codes);
}
