package com.nexio.nexio.email.repository;

import com.nexio.nexio.email.model.GoogleToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoogleTokenRepository extends JpaRepository<GoogleToken,Long> {

    Optional<GoogleToken> findByUserId(Long userId);
}
