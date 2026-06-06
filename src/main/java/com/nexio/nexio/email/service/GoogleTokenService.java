package com.nexio.nexio.email.service;

import com.nexio.nexio.email.model.GoogleToken;
import com.nexio.nexio.email.repository.GoogleTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleTokenService {
    private final GoogleTokenRepository googleTokenRepository;

    public GoogleToken save(GoogleToken token){
        return googleTokenRepository.save(token);
    }
    public Optional<GoogleToken> findByUserId(Long userId){
        return googleTokenRepository.findByUserId(userId);
    }
}
