package com.mbotamapay.backend.service;

import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final UserRepository userRepository;

    public String generateReferralCode() {
        String code;
        do {
            code = RandomStringUtils.randomAlphanumeric(8).toUpperCase();
        } while (userRepository.existsByReferralCode(code));
        return code;
    }

    public Optional<User> findByReferralCode(String code) {
        return userRepository.findByReferralCode(code);
    }
}
