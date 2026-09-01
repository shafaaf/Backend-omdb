package com.example.movielist.repository;

import com.example.movielist.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for User — registered accounts, looked up by email. */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}
