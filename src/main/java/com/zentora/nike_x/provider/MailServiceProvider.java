package com.zentora.nike_x.provider;

import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.emails.Email;
import com.mailersend.sdk.exceptions.MailerSendException;
import com.zentora.nike_x.mail.MailContent;
import com.zentora.nike_x.util.Env;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MailServiceProvider {

    private static MailServiceProvider mailServiceProvider;
    private final String API_TOKEN = Env.get("MAILERSEND_API_TOKEN") != null ? Env.get("MAILERSEND_API_TOKEN")
            : Env.get("mailersend.api_token");

    private final String FROM_EMAIL = Env.get("APP_MAIL") != null ? Env.get("APP_MAIL") : Env.get("app.mail");
    private final String FROM_NAME = Env.get("APP_NAME") != null ? Env.get("APP_NAME") : Env.get("app.name");

    private ThreadPoolExecutor executor;
    private final BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();

    private MailServiceProvider() {

    }

    public static synchronized MailServiceProvider getMailServiceProvider() {
        if (mailServiceProvider == null) {
            mailServiceProvider = new MailServiceProvider();
        }
        return mailServiceProvider;
    }

    public void start() {

        executor = new ThreadPoolExecutor(
                2,
                5,
                5,
                TimeUnit.SECONDS,
                blockingQueue,
                new ThreadPoolExecutor.AbortPolicy());
        executor.prestartCoreThread();
        System.out.println("MailServiceProvider Started (Background Threads Ready)");
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void sendMail(MailContent mailContent) {

        Runnable emailTask = () -> {
            try {
                Email email = new Email();

                email.setFrom(FROM_NAME, FROM_EMAIL);
                email.addRecipient("User", mailContent.getToEmail());
                email.setSubject(mailContent.getSubject());

                email.setHtml(mailContent.getHtmlContent());

                email.setPlain("Please enable HTML to view this email.");

                MailerSend ms = new MailerSend();
                ms.setToken(API_TOKEN);
                ms.emails().send(email);

                System.out.println("Email sent successfully to: " + mailContent.getToEmail());

            } catch (MailerSendException e) {
                System.err.println("Error sending email: " + e.getMessage());
                e.printStackTrace();
            }
        };

        boolean offered = blockingQueue.offer(emailTask);
        if (!offered) {
            System.err.println("Email Queue Full!");
        }
    }
}