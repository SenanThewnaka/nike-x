package com.zentora.nike_x.mail;

import io.rocketbase.mail.EmailTemplateBuilder;
import io.rocketbase.mail.model.HtmlTextEmail;
import com.zentora.nike_x.util.Env;

public class ResetPasswordMail implements MailContent {

    private final String toEmail;
    private final String resetCode;

    public ResetPasswordMail(String toEmail, String resetCode) {
        this.toEmail = toEmail;
        this.resetCode = resetCode;
    }

    @Override
    public String getToEmail() {
        return toEmail;
    }

    @Override
    public String getSubject() {
        return "Password Reset Code - " + Env.get("app.name");
    }

    @Override
    public String getHtmlContent() {
        String appURL = Env.get("app.url");

        HtmlTextEmail htmlTextEmail = EmailTemplateBuilder.builder()
                .header()
                .logo("https://logos-world.net/wp-content/uploads/2020/06/Nike-Logo.png")
                .logoHeight(40)
                .and()

                .text("PASSWORD RESET REQUEST").h1().center().and()
                .text("Hello " + toEmail + ",").center().and()
                .text("You requested to reset your password.").center().and()
                .text("Use the code below to proceed:").center().and()
                .text(resetCode).bold().h1().color("#dc3545").center().and()

                .text("If you did not request this, please ignore this email.")
                .center().and()

                .copyright(Env.get("app.name"))
                .url(appURL)
                .suffix(". All Rights Reserved")
                .and()

                .build();

        return htmlTextEmail.getHtml();
    }
}