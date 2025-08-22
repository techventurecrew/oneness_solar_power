package com.solar.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.solar.entity.Otp;
import com.solar.repo.OtpRepo;
import com.solar.utility.Data;
import com.solar.utility.Utilities;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class OtpService {
    private static final long OTP_EXPIRY_MINUTES = 10;
    @Autowired
    private OtpRepo otpRepo;

    @Autowired
    private JavaMailSender mailSender;

    public String generateOtp(String email) throws MessagingException {
        MimeMessage mm = mailSender.createMimeMessage();
        MimeMessageHelper msg = new MimeMessageHelper(mm, true);
        msg.setTo(email);
        msg.setSubject("Your OTP code from Solar Solutions");
        String genOtp = Utilities.generateOTP();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        Otp otp = new Otp(normalizedEmail, genOtp, LocalDateTime.now());
        otpRepo.save(otp);
        msg.setText(Data.getMessageBody(email, genOtp), true);
        mailSender.send(mm); 
        return genOtp;
    }

    public Boolean verifyOtp(String email, String otp) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String providedOtp = otp == null ? "" : otp.trim();

        Otp otpentity = otpRepo.findByEmail(normalizedEmail).orElse(null);
        if (otpentity == null) {
            return false;
        }

        if (otpentity.getOtpCode() == null || !otpentity.getOtpCode().equals(providedOtp)) {
            return false;
        }

        LocalDateTime expiryCutoff = LocalDateTime.now().minusMinutes(OTP_EXPIRY_MINUTES);
        if (otpentity.getCreationTime() == null || otpentity.getCreationTime().isBefore(expiryCutoff)) {
            return false;
        }

        // Invalidate OTP after successful verification to prevent reuse
        otpRepo.deleteById(normalizedEmail);
        return true;
    }

    @Scheduled(fixedRate = 60000)
    public void removeExpiredOtps() {
        LocalDateTime expiry = LocalDateTime.now().minusMinutes(OTP_EXPIRY_MINUTES);
        System.out.println("Checking for expired OTPs before: " + expiry);
        List<Otp> expiredOtps = otpRepo.findByCreationTimeBefore(expiry);
        System.out.println("Removing expired OTPs: " + expiredOtps.size());
        if (!expiredOtps.isEmpty()) {
            otpRepo.deleteAll(expiredOtps);
        }
    }
}
