package com.zentora.nike_x.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.ContactDTO;
import com.zentora.nike_x.mail.ContactMessageMail;
import com.zentora.nike_x.provider.MailServiceProvider;
import com.zentora.nike_x.util.AppUtil;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/contact")
public class ContactController {

    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendMessage(String requestBody) {
        JsonObject responseObject = new JsonObject();
        try {
            Gson gson = new Gson();
            ContactDTO contactDTO = gson.fromJson(requestBody, ContactDTO.class);

            if (contactDTO.getFirstName() == null || contactDTO.getFirstName().isEmpty()) {
                throw new Exception("First name is required");
            }
            if (contactDTO.getEmail() == null || contactDTO.getEmail().isEmpty()) {
                throw new Exception("Email is required");
            }
            if (contactDTO.getMessage() == null || contactDTO.getMessage().isEmpty()) {
                throw new Exception("Message is required");
            }

            // Send Email
            MailServiceProvider.getMailServiceProvider().sendMail(
                    new ContactMessageMail(
                            contactDTO.getFirstName(),
                            contactDTO.getLastName() != null ? contactDTO.getLastName() : "",
                            contactDTO.getEmail(),
                            contactDTO.getMessage()));

            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Message sent successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Failed to send message: " + e.getMessage());
        }
        return Response.ok(AppUtil.GSON.toJson(responseObject)).build();
    }
}
