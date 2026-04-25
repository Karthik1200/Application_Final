package com.example.Application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "msg91")
public class Msg91Properties {

    private String apiKey;
    private Sms sms = new Sms();
    private Email email = new Email();
    private Whatsapp whatsapp = new Whatsapp();

    public static class Sms {
        private String templateId;
        private String senderId = "VRGTSY";
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String v) { templateId = v; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String v) { senderId = v; }
    }

    public static class Email {
        private String templateId;
        private String from = "noreply@vrgt.in";
        private String fromName = "VRGT System";
        private String domain = "vrgt.in";
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String v) { templateId = v; }
        public String getFrom() { return from; }
        public void setFrom(String v) { from = v; }
        public String getFromName() { return fromName; }
        public void setFromName(String v) { fromName = v; }
        public String getDomain() { return domain; }
        public void setDomain(String v) { domain = v; }
    }

    public static class Whatsapp {
        private String integratedNumber;
        private String templateName = "vrgt_visitor_otp";
        public String getIntegratedNumber() { return integratedNumber; }
        public void setIntegratedNumber(String v) { integratedNumber = v; }
        public String getTemplateName() { return templateName; }
        public void setTemplateName(String v) { templateName = v; }
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String v) { apiKey = v; }
    public Sms getSms() { return sms; }
    public void setSms(Sms v) { sms = v; }
    public Email getEmail() { return email; }
    public void setEmail(Email v) { email = v; }
    public Whatsapp getWhatsapp() { return whatsapp; }
    public void setWhatsapp(Whatsapp v) { whatsapp = v; }
}
