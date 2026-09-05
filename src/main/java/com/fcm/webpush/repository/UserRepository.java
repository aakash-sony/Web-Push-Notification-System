package com.fcm.webpush.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fcm.webpush.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	@Query("SELECT u.id, u.createdAt FROM User u WHERE u.id > :lastId ORDER BY u.id ASC")
	List<Object[]> findUserIdAndCreatedAtChunk(@Param("lastId") Long lastId, Pageable pageable);
}

