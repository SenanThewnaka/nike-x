package com.zentora.nike_x.mail;

public interface MailContent {
    String getToEmail();
    String getSubject();
    String getHtmlContent();
}