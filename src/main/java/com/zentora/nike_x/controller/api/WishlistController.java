package com.zentora.nike_x.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.UserDTO;
import com.zentora.nike_x.service.WishlistService;
import com.zentora.nike_x.util.AppUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/wishlist")
public class WishlistController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWishlistItems(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null) {
            return Response.ok(AppUtil.GSON.toJson(getError("Please login"))).build();
        }

        WishlistService wishlistService = new WishlistService();
        String response = wishlistService.getWishlistItems(user.getId());
        return Response.ok(response).build();

    }

    @GET
    @Path("/ids")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWishlistIds(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserDTO user = (UserDTO) session.getAttribute("user");

        JsonObject r = new JsonObject();
        if (user == null) {
            r.addProperty("status", false);
            r.add("ids", new com.google.gson.JsonArray());
            return Response.ok(AppUtil.GSON.toJson(r)).build();
        }

        WishlistService wishlistService = new WishlistService();
        java.util.List<Integer> ids = wishlistService.getWishlistProductIds(user.getId());

        com.google.gson.JsonArray idArray = new com.google.gson.JsonArray();
        for (Integer id : ids)
            idArray.add(id);

        r.addProperty("status", true);
        r.add("ids", idArray);
        return Response.ok(AppUtil.GSON.toJson(r)).build();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addToWishlist(String jsonBody, @Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null) {
            return Response.ok(AppUtil.GSON.toJson(getError("Please login to add to wishlist"))).build();
        }

        try {
            JsonObject json = new Gson().fromJson(jsonBody, JsonObject.class);
            int productId = json.get("productId").getAsInt();

            WishlistService wishlistService = new WishlistService();
            String result = wishlistService.addToWishlist(user.getId(), productId);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.ok(AppUtil.GSON.toJson(getError("Invalid request"))).build();
        }
    }

    @GET
    @Path("/check/{productId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkWishlistStatus(@PathParam("productId") int productId, @Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null) {
            // Not logged in -> false
            JsonObject r = new JsonObject();
            r.addProperty("status", false);
            r.addProperty("inWishlist", false);
            return Response.ok(AppUtil.GSON.toJson(r)).build();
        }

        WishlistService wishlistService = new WishlistService();
        String response = wishlistService.checkStatus(user.getId(), productId);
        return Response.ok(response).build();
    }

    @POST
    @Path("/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeFromWishlist(String jsonBody, @Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserDTO user = (UserDTO) session.getAttribute("user");

        if (user == null) {
            return Response.ok(AppUtil.GSON.toJson(getError("Please login"))).build();
        }

        try {
            JsonObject json = new Gson().fromJson(jsonBody, JsonObject.class);
            int productId = json.get("productId").getAsInt();

            WishlistService wishlistService = new WishlistService();
            String result = wishlistService.removeFromWishlist(user.getId(), productId);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.ok(AppUtil.GSON.toJson(getError("Invalid request"))).build();
        }
    }

    private JsonObject getError(String msg) {
        JsonObject r = new JsonObject();
        r.addProperty("status", false);
        r.addProperty("message", msg);
        return r;
    }
}
