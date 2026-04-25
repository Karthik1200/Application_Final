package com.example.Application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VisitorRegistrationDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    private String company;

    private String idType;

    private String idNumber;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    @NotBlank(message = "Host name is required")
    private String hostName;

    private String hostEmail;

    private String hostPhone;

    private String scheduledDate;

    private String scheduledTime;

    /** Comma-separated channels: sms, whatsapp, email  (default: sms) */
    @lombok.Builder.Default
    private String otpChannel = "sms";
}
