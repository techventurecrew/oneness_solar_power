package com.solar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.solar.controller.QueryController;
import com.solar.entity.Otp;
import com.solar.repo.OtpRepo;

@SpringBootTest
class OtpVerificationTests {

    @Autowired
    private OtpRepo otpRepo;

    @Autowired
    private QueryController queryController;

    private String lastEmailUsed;

    @AfterEach
    void cleanup() {
        if (lastEmailUsed != null) {
            otpRepo.deleteById(lastEmailUsed.toLowerCase().trim());
        }
    }

    @Test
    void verifyOtp_valid_success_and_invalidate() {
        String email = "otp.test.valid@example.com";
        String otpCode = "123456";
        lastEmailUsed = email;

        otpRepo.deleteById(email);
        otpRepo.save(new Otp(email.toLowerCase().trim(), otpCode, LocalDateTime.now()));

        ResponseEntity<String> response = queryController.verifyOtp(email, otpCode);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // OTP should be invalidated (deleted) after successful verification
        assertFalse(otpRepo.findById(email.toLowerCase().trim()).isPresent());
    }

    @Test
    void verifyOtp_invalid_code_returns_bad_request() {
        String email = "otp.test.invalid@example.com";
        String storedOtp = "111111";
        String providedOtp = "000000";
        lastEmailUsed = email;

        otpRepo.deleteById(email);
        otpRepo.save(new Otp(email.toLowerCase().trim(), storedOtp, LocalDateTime.now()));

        ResponseEntity<String> response = queryController.verifyOtp(email, providedOtp);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void verifyOtp_expired_returns_bad_request() {
        String email = "otp.test.expired@example.com";
        String otpCode = "222222";
        lastEmailUsed = email;

        otpRepo.deleteById(email);
        otpRepo.save(new Otp(email.toLowerCase().trim(), otpCode, LocalDateTime.now().minusMinutes(11)));

        ResponseEntity<String> response = queryController.verifyOtp(email, otpCode);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}


