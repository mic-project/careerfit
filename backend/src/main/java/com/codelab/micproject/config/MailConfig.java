package com.codelab.micproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean(name = "gmailSender")
    public JavaMailSender gmailSender(
            @Value("${app.mail.gmail.host}") String host,
            @Value("${app.mail.gmail.port}") int port,
            @Value("${app.mail.gmail.username}") String username,
            @Value("${app.mail.gmail.password}") String password,
            @Value("${app.mail.gmail.starttls}") boolean starttls,
            @Value("${app.mail.gmail.auth}") boolean auth
    ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        // 안정성 옵션
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        // 지메일에서 가끔 필요
        props.put("mail.smtp.ssl.trust", host);
        return sender;
    }

    @Bean(name = "naverSender")
    public JavaMailSender naverSender(
            @Value("${app.mail.naver.host}") String host,
            @Value("${app.mail.naver.port}") int port,
            @Value("${app.mail.naver.username}") String username,
            @Value("${app.mail.naver.password}") String password,
            @Value("${app.mail.naver.starttls}") boolean starttls,
            @Value("${app.mail.naver.auth}") boolean auth,
            @Value("${app.mail.naver.ssl:false}") boolean ssl
    ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        if (ssl) props.put("mail.smtp.ssl.enable", "true"); // 465 쓸 때
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.smtp.ssl.trust", host);
        return sender;
    }

}
