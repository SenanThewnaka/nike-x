package com.zentora.nike_x.controller.api;

import com.zentora.nike_x.dto.UserDTO;

import com.zentora.nike_x.service.UserService;
import com.zentora.nike_x.util.AppUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
public class UserController {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNewAccount(String jsonData) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().addNewUser(userDTO);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/update-email")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEmail(String jsonData) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().updateEmail(userDTO);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/resend-code")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resendCode(String jsonData) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().resendCode(userDTO);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/verify-code")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyCode(String jsonData) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().verifyCode(userDTO);
        return Response.ok().entity(responseJson).build();

    }

    @Path("/forgot-password")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response forgotPasswordCode(String jsonData) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().forgotPasswordCode(userDTO);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/reset-password")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetPassword(String jsonData) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().ResetPassword(userDTO);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/sign-in")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String jsonData, @Context HttpServletRequest request, @Context HttpServletResponse response) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().login(userDTO, request, response);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/check-remember-me")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkRememberMe(@Context HttpServletRequest request) {
        String responseJson = new UserService().checkRememberMe(request);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/auth-check")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkAuth(@Context HttpServletRequest request) {
        String responseJson = new UserService().checkAuth(request);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/profile")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProfile(@Context HttpServletRequest request) {
        String responseJson = new UserService().getUserProfile(request);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/save-address")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveAddress(String jsonData, @Context HttpServletRequest request) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().saveAddress(userDTO, request);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/set-primary-address")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response setPrimaryAddress(@jakarta.ws.rs.QueryParam("id") int id, @Context HttpServletRequest request) {
        String responseJson = new UserService().setPrimaryAddress(id, request);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/delete-address")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAddress(@jakarta.ws.rs.QueryParam("id") int id, @Context HttpServletRequest request) {
        String responseJson = new UserService().deleteAddress(id, request);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/delete-mobile")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMobile(@jakarta.ws.rs.QueryParam("id") int id, @Context HttpServletRequest request) {
        String responseJson = new UserService().deleteMobile(id, request);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/save-mobile")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveMobile(String jsonData, @Context HttpServletRequest request) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().saveMobile(userDTO, request);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/logout")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        String responseJson = new UserService().logout(request, response);
        return Response.ok().entity(responseJson).build();
    }
}
