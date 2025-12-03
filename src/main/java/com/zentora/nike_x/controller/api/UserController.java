package com.zentora.nike_x.controller.api;

import com.zentora.nike_x.dto.UserDTO;
import com.zentora.nike_x.entity.User;
import com.zentora.nike_x.service.UserService;
import com.zentora.nike_x.util.AppUtil;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path( "/users")
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
    public Response updateEmail(String jsonData){
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().updateEmail(userDTO);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/resend-code")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resendCode(String jsonData){
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().resendCode(userDTO);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/verify-code")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyCode(String jsonData){
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().verifyCode(userDTO);
        return Response.ok().entity(responseJson).build();

    }

    @Path("/forgot-password")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response forgotPasswordCode(String jsonData){
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().forgotPasswordCode(userDTO);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/reset-password")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetPassword(String jsonData){
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().ResetPassword(userDTO);
        return Response.ok().entity(responseJson).build();
    }
}

