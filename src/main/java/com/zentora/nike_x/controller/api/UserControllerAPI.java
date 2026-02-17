package com.zentora.nike_x.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.UserDTO;

import com.zentora.nike_x.util.AppUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class UserControllerAPI {

    @GET
    @Path("/auth/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAuthStatus(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserDTO user = (UserDTO) session.getAttribute("user");
        JsonObject resp = new JsonObject();

        if (user != null) {
            resp.addProperty("isLoggedIn", true);
            resp.addProperty("userId", user.getId());
            resp.addProperty("userName", user.getFirstName());
        } else {
            resp.addProperty("isLoggedIn", false);
        }
        return Response.ok(AppUtil.GSON.toJson(resp)).build();
    }

    private JsonObject getError(String msg) {
        JsonObject r = new JsonObject();
        r.addProperty("status", false);
        r.addProperty("message", msg);
        return r;
    }
}
