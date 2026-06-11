package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.entity.SystemConfig;
import com.sales.mapper.SystemConfigMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SystemConfigMapper systemConfigMapper;
    private JavaMailSender mailSender;

    private String getConfig(String key) {
        SystemConfig c = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        return c != null ? c.getConfigValue() : null;
    }

    private JavaMailSender getMailSender() {
        if (mailSender != null) return mailSender;
        String host = getConfig("mail_host");
        String user = getConfig("mail_username");
        String pass = getConfig("mail_password");
        if (host == null || host.isEmpty() || user == null || user.isEmpty()) {
            log.warn("Email not configured, mock mode");
            return null;
        }
        JavaMailSenderImpl s = new JavaMailSenderImpl();
        s.setHost(host);
        String port = getConfig("mail_port");
        s.setPort(port != null ? Integer.parseInt(port) : 587);
        s.setUsername(user);
        s.setPassword(pass);
        Properties p = s.getJavaMailProperties();
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "10000");
        p.put("mail.smtp.writetimeout", "10000");
        // 根据端口选择 SSL 或 STARTTLS
        int portNum = port != null ? Integer.parseInt(port) : 587;
        if (portNum == 465) {
            p.put("mail.smtp.ssl.enable", "true");
            p.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            p.put("mail.smtp.starttls.enable", "true");
        }
        mailSender = s;
        return s;
    }

    public boolean sendEmail(String to, String subject, String htmlContent) {
        // Convert plain text newlines to HTML breaks for text templates
        htmlContent = htmlContent.replace("\r\n", "<br/>").replace("\n", "<br/>");
        htmlContent = htmlContent.replaceAll(
            "(https?://[^\s<>]+)",
            "<a href=\"$1\" style=\"color:#667eea;text-decoration:underline;\">$1</a>"
        );
        JavaMailSender s = getMailSender();
        if (s == null) {
            log.info("[MOCK EMAIL] To: {}, Subject: {}", to, subject);
            return true;
        }
        try {
            MimeMessage msg = s.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            String fn = getConfig("mail_from_name");
            h.setFrom(getConfig("mail_username"), fn != null ? fn : "System");
            h.setTo(to);
            h.setSubject(subject);
            h.setText(htmlContent, true);
            s.send(msg);
            log.info("Email sent to: {}", to);
            return true;
        } catch (Exception e) {
            log.error("Email failed: {}", to, e);
            log.info("[FALLBACK] To: {}, Body: {}", to, htmlContent);
            return false;
        }
    }
}
