package com.zentora.nike_x.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.UserDTO;
import com.zentora.nike_x.service.OrderService;
import com.zentora.nike_x.util.AppUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.FormParam;

@Path("/order")
public class OrderController {

    @POST
    @Path("/checkout")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkout(String requestBodyJson, @Context HttpServletRequest req) {

        // Auth Check
        HttpSession session = req.getSession();
        UserDTO userDTO = (UserDTO) session.getAttribute("user");

        if (userDTO == null) {
            JsonObject error = new JsonObject();
            error.addProperty("status", false);
            error.addProperty("message", "Please sign in first!");
            return Response.ok(AppUtil.GSON.toJson(error)).build();
        }

        try {
            // Parse Request Body
            Gson gson = new Gson();
            JsonObject requestBody = gson.fromJson(requestBodyJson, JsonObject.class);

            int addressId = 0;
            String paymentMethod = null;
            String mobile = null;
            int stockId = 0;

            if (requestBody.has("addressId")) {
                addressId = requestBody.get("addressId").getAsInt();
            }
            if (requestBody.has("paymentMethod")) {
                paymentMethod = requestBody.get("paymentMethod").getAsString();
            }
            if (requestBody.has("mobile")) {
                mobile = requestBody.get("mobile").getAsString();
            }
            if (requestBody.has("stockId")) {
                stockId = requestBody.get("stockId").getAsInt();
            }

            OrderService orderService = new OrderService();
            String result;

            if (stockId > 0) {
                // Separate Process: Single Item Checkout
                result = orderService.placeSingleOrder(userDTO, addressId, paymentMethod, mobile, stockId);
            } else {
                // Separate Process: Full Cart Checkout
                result = orderService.placeCartOrder(userDTO, addressId, paymentMethod, mobile);
            }

            return Response.ok(result).build();

        } catch (Exception e) {
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("status", false);
            error.addProperty("message", "Invalid Request: " + e.getMessage());
            return Response.ok(AppUtil.GSON.toJson(error)).build();
        }
    }

    @GET
    @Path("/details")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrderDetails(@jakarta.ws.rs.QueryParam("id") int orderId, @Context HttpServletRequest req) {
        com.zentora.nike_x.dto.UserDTO user = (com.zentora.nike_x.dto.UserDTO) req.getSession().getAttribute("user");
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"status\":false, \"message\":\"Please login\"}").build();
        }

        OrderService orderService = new OrderService();
        String response = orderService.getOrderDetails(orderId, user.getId());
        return Response.ok(response).build();
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrderList(@Context HttpServletRequest req) {
        com.zentora.nike_x.dto.UserDTO user = (com.zentora.nike_x.dto.UserDTO) req.getSession().getAttribute("user");
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"status\":false, \"message\":\"Please login\"}").build();
        }

        OrderService orderService = new OrderService();
        String response = orderService.getOrdersByUser(user.getId());
        return Response.ok(response).build();
    }

    @POST
    @Path("/verify-payment")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String verifyPayment(@FormParam("orderId") int orderId) {
        OrderService orderService = new OrderService();
        return orderService.verifyOrderAndCapture(orderId);
    }

    @Path("/payment-failed")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response handlePaymentFailure(@jakarta.ws.rs.FormParam("orderId") int orderId,
            @Context HttpServletRequest req) {
        com.zentora.nike_x.dto.UserDTO user = (com.zentora.nike_x.dto.UserDTO) req.getSession().getAttribute("user");
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        OrderService orderService = new OrderService();
        String result = orderService.handlePaymentFailure(orderId, user.getId());
        return Response.ok(result).build();
    }
}
