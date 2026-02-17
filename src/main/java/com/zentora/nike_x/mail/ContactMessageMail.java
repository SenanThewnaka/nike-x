package com.zentora.nike_x.mail;

public class ContactMessageMail implements MailContent {

    private final String firstName;
    private final String lastName;
    private final String senderEmail;
    private final String message;

    public ContactMessageMail(String firstName, String lastName, String senderEmail, String message) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.senderEmail = senderEmail;
        this.message = message;
    }

    @Override
    public String getToEmail() {
        return "thewnakasenan@gmail.com";
    }

    @Override
    public String getSubject() {
        return "New Contact Message - Nike-X";
    }

    @Override
    public String getHtmlContent() {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;'>"
                +
                "<h2 style='color: #22c55e;'>New Contact Message</h2>" +
                "<p>You have received a new message from the Nike-X website contact form.</p>" +
                "<hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'>" +
                "<p><strong>Name:</strong> " + firstName + " " + lastName + "</p>" +
                "<p><strong>Email:</strong> " + senderEmail + "</p>" +
                "<p><strong>Message:</strong></p>" +
                "<div style='background-color: #f9f9f9; padding: 15px; border-left: 4px solid #22c55e; border-radius: 4px;'>"
                +
                message.replace("\n", "<br>") +
                "</div>" +
                "<hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'>" +
                "<p style='font-size: 12px; color: #999;'>This email was sent automatically from the Nike-X system.</p>"
                +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
