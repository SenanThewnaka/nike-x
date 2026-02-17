package com.zentora.nike_x.mail;

import com.zentora.nike_x.util.Env;
import io.rocketbase.mail.EmailTemplateBuilder;
import io.rocketbase.mail.model.HtmlTextEmail;

public class AdminLoginCodeMail implements MailContent{

    private final String toEmail;
    private final String verificationCode;

    public AdminLoginCodeMail(String toEmail, String verificationCode) {
        this.toEmail = toEmail;
        this.verificationCode = verificationCode;
    }

    @Override
    public String getToEmail() {
        return toEmail;
    }

    @Override
    public String getSubject() {
        return "Email Verification Code - " + Env.get("app.name");
    }

    @Override
    public String getHtmlContent() {
        String appURL = Env.get("app.url");
        String verifyURL = appURL + "/verify-email.html?email=" + toEmail;

        HtmlTextEmail htmlTextEmail = EmailTemplateBuilder.builder()
                .header()
                .logo("https://logos-world.net/wp-content/uploads/2020/06/Nike-Logo.png").logoHeight(40).and()
                .text("WELCOME BACK" + toEmail).h1().center().and()
                .text("Do not share this code with anyone else").center().bold().h2().color("red").and()
                .text("Your verification code is:").center().and()
                .text(verificationCode).bold().h1().color("#28a745").center().and()
                .copyright(Env.get("app.name")).url(appURL).suffix(". All Rights Reserved").and()
                .build();

        return htmlTextEmail.getHtml();
    }
}