package com.sales.controller;

import com.sales.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private static final ConcurrentHashMap<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();
    private static final Random random = new Random();
    private static final long EXPIRY_MS = 5 * 60 * 1000; // 5 minutes

    static {
        // Schedule cleanup of expired CAPTCHAs every minute
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            captchaStore.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        }, 1, 1, TimeUnit.MINUTES);
    }

    private static class CaptchaEntry {
        String answer;
        long expiresAt;

        CaptchaEntry(String answer, long expiresAt) {
            this.answer = answer;
            this.expiresAt = expiresAt;
        }
    }

    @GetMapping("/generate")
    public ApiResponse<Map<String, String>> generate() throws Exception {
        String code = generateRandomCode(4);
        String base64Image = generateCaptchaImage(code);

        String id = UUID.randomUUID().toString();
        captchaStore.put(id, new CaptchaEntry(code, System.currentTimeMillis() + EXPIRY_MS));

        Map<String, String> data = new HashMap<>();
        data.put("id", id);
        data.put("image", base64Image);
        return ApiResponse.success(data);
    }

    @PostMapping("/verify")
    public ApiResponse<Void> verify(@RequestBody Map<String, String> body) {
        String id = body.get("id");
        String answer = body.get("answer");

        if (id == null || id.isEmpty() || answer == null || answer.isEmpty()) {
            return ApiResponse.error("参数不能为空");
        }

        CaptchaEntry entry = captchaStore.get(id);
        if (entry == null) {
            return ApiResponse.error("验证码已过期");
        }

        if (System.currentTimeMillis() > entry.expiresAt) {
            captchaStore.remove(id);
            return ApiResponse.error("验证码已过期");
        }

        if (entry.answer.equalsIgnoreCase(answer.trim())) {
            captchaStore.remove(id);
            return ApiResponse.success();
        }

        return ApiResponse.error("验证码错误");
    }

    private static String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String generateCaptchaImage(String code) throws Exception {
        int width = 120, height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);

        // Random font
        String[] fonts = {"Arial", "Verdana", "Times New Roman"};
        Font font = new Font(fonts[random.nextInt(fonts.length)], Font.BOLD, 28);
        g.setFont(font);

        // Draw each character with random color and slight rotation
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
            double theta = Math.toRadians(random.nextInt(20) - 10);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.rotate(theta, 20 + i * 25, 30);
            g2.drawString(String.valueOf(code.charAt(i)), 15 + i * 25, 30);
            g2.dispose();
        }

        // Noise lines (5-8)
        int lineCount = 5 + random.nextInt(4);
        for (int i = 0; i < lineCount; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
        }

        // Noise dots (50-100)
        int dotCount = 50 + random.nextInt(51);
        for (int i = 0; i < dotCount; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.fillOval(random.nextInt(width), random.nextInt(height), 2, 2);
        }

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
