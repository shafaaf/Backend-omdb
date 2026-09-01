package com.example.movielist.repository;

import com.example.movielist.entity.Movie;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for Movie — the locally cached copy of OMDb movie data. */
public interface MovieRepository extends JpaRepository<Movie, Long> {

	Optional<Movie> findByExternalId(String externalId);
}
