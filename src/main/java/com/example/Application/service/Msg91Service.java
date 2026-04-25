package com.example.Application.service;

import com.example.Application.config.Msg91Properties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class Msg91Service {

    private static final Logger log = LoggerFactory.getLogger(Msg91Service.class);

    private static final String OTP_API_URL        = "https://control.msg91.com/api/v5/otp";
    private static final String EMAIL_API_URL       = "https://control.msg91.com/api/v5/email/send";
    private static final String WHATSAPP_API_URL    = "https://api.msg91.com/api/v5/whatsapp/whatsapp-outbound-message/bulk/";

    @Autowired private Msg91Properties props;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper       = new ObjectMapper();

    // ── Public entry point ────────────────────────────────────────────────────

    /**
     * Sends OTP via every channel listed in otpChannels (comma-separated: sms,whatsapp,email).
     * Each channel is attempted independently; failures are logged but don't abort others.
     */
    public OtpResult sendOtp(String phone, String email, String visitorName, String otp, String otpChannels) {
        List<String> sent   = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        Set<String> channels = parseChannels(otpChannels);

        if (channels.contains("sms")) {
            if (sendSmsOtp(phone, otp, visitorName)) sent.add("SMS");
            else failed.add("SMS");
        }
        if (channels.contains("whatsapp")) {
            if (sendWhatsappOtp(phone, otp, visitorName)) sent.add("WhatsApp");
            else failed.add("WhatsApp");
        }
        if (channels.contains("email")) {
            if (sendEmailOtp(email, otp, visitorName)) sent.add("Email");
            else failed.add("Email");
        }

        return new OtpResult(sent, failed);
    }

    // ── SMS OTP ───────────────────────────────────────────────────────────────

    private boolean sendSmsOtp(String phone, String otp, String visitorName) {
        String mobile = sanitizeMobile(phone);
        if (mobile == null) {
            log.warn("SMS OTP skipped — invalid phone: {}", phone);
            return false;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("template_id", props.getSms().getTemplateId());
        body.put("mobile",      mobile);
        body.put("otp",         otp);
        body.put("otp_expiry",  10);
        body.put("user_name",   visitorName);

        try {
            ResponseEntity<String> res = post(OTP_API_URL, body);
            if (res.getStatusCode().is2xxSuccessful()) {
                log.info("MSG91 SMS OTP sent to {}", mobile);
                return true;
            }
            log.warn("MSG91 SMS OTP non-2xx {} for {}: {}", res.getStatusCode(), mobile, res.getBody());
            return false;
        } catch (Exception e) {
            log.error("MSG91 SMS OTP error for {}: {}", mobile, e.getMessage());
            return false;
        }
    }

    // ── WhatsApp OTP ──────────────────────────────────────────────────────────

    private boolean sendWhatsappOtp(String phone, String otp, String visitorName) {
        String mobile = sanitizeMobile(phone);
        if (mobile == null) {
            log.warn("WhatsApp OTP skipped — invalid phone: {}", phone);
            return false;
        }
        if (isBlank(props.getWhatsapp().getIntegratedNumber())) {
            log.warn("WhatsApp OTP skipped — msg91.whatsapp.integrated-number not configured");
            return false;
        }

        Map<String, Object> templateBody = new LinkedHashMap<>();
        templateBody.put("name",     props.getWhatsapp().getTemplateName());
        templateBody.put("language", Map.of("code", "en"));
        templateBody.put("components", List.of(Map.of(
                "type", "body",
                "parameters", List.of(
                        Map.of("type", "text", "text", visitorName),
                        Map.of("type", "text", "text", otp),
                        Map.of("type", "text", "text", "10 minutes")
                )
        )));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("to",       mobile);
        payload.put("type",     "template");
        payload.put("template", templateBody);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("integrated_number", props.getWhatsapp().getIntegratedNumber());
        body.put("content_type",      "template");
        body.put("payload",           payload);

        try {
            ResponseEntity<String> res = post(WHATSAPP_API_URL, body);
            if (res.getStatusCode().is2xxSuccessful()) {
                log.info("MSG91 WhatsApp OTP sent to {}", mobile);
                return true;
            }
            log.warn("MSG91 WhatsApp OTP non-2xx {} for {}: {}", res.getStatusCode(), mobile, res.getBody());
            return false;
        } catch (Exception e) {
            log.error("MSG91 WhatsApp OTP error for {}: {}", mobile, e.getMessage());
            return false;
        }
    }

    // ── Email OTP ─────────────────────────────────────────────────────────────

    private boolean sendEmailOtp(String toEmail, String otp, String visitorName) {
        if (isBlank(toEmail)) {
            log.warn("Email OTP skipped — no email provided");
            return false;
        }
        if (isBlank(props.getEmail().getTemplateId())) {
            log.warn("Email OTP skipped — msg91.email.template-id not configured");
            return false;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recipients", List.of(Map.of(
                "to",   List.of(Map.of("email", toEmail, "name", visitorName)),
                "variables", Map.of("NAME", visitorName, "OTP", otp, "EXPIRY", "10 minutes")
        )));
        body.put("from",         Map.of("email", props.getEmail().getFrom(), "name", props.getEmail().getFromName()));
        body.put("domain",       props.getEmail().getDomain());
        body.put("mail_type_id", "1");
        body.put("template_id",  props.getEmail().getTemplateId());

        try {
            ResponseEntity<String> res = post(EMAIL_API_URL, body);
            if (res.getStatusCode().is2xxSuccessful()) {
                log.info("MSG91 Email OTP sent to {}", toEmail);
                return true;
            }
            log.warn("MSG91 Email OTP non-2xx {} for {}: {}", res.getStatusCode(), toEmail, res.getBody());
            return false;
        } catch (Exception e) {
            log.error("MSG91 Email OTP error for {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<String> post(String url, Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("authkey", props.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = mapper.writeValueAsString(body);
        return restTemplate.postForEntity(url, new HttpEntity<>(json, headers), String.class);
    }

    /** Normalises to Indian mobile format 91XXXXXXXXXX; returns null if unparseable. */
    private String sanitizeMobile(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) return "91" + digits;
        if (digits.length() == 12 && digits.startsWith("91")) return digits;
        if (digits.length() == 11 && digits.startsWith("0")) return "91" + digits.substring(1);
        log.warn("Unrecognised phone format '{}', using as-is", phone);
        return digits.isEmpty() ? null : digits;
    }

    private Set<String> parseChannels(String raw) {
        Set<String> result = new HashSet<>();
        if (raw == null || raw.isBlank()) { result.add("sms"); return result; }
        for (String c : raw.split("[,\\s]+")) {
            if (!c.isBlank()) result.add(c.trim().toLowerCase());
        }
        return result;
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    // ── Result DTO ────────────────────────────────────────────────────────────

    public static class OtpResult {
        public final List<String> sent;
        public final List<String> failed;
        OtpResult(List<String> sent, List<String> failed) { this.sent = sent; this.failed = failed; }
        public boolean anySucceeded() { return !sent.isEmpty(); }
        public String summary() {
            if (sent.isEmpty()) return "OTP delivery failed on all channels";
            String s = "OTP sent via " + String.join(" + ", sent);
            if (!failed.isEmpty()) s += " (failed: " + String.join(", ", failed) + ")";
            return s;
        }
    }
}
