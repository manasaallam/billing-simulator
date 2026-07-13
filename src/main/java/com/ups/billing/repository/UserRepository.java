package com.ups.billing.repository;

import com.ups.billing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User}. Replaces the previous in-memory
 * {@code ConcurrentHashMap}-based store. Backed by H2 today and Supabase/Postgres
 * once the datasource is configured — no code changes required to switch.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
