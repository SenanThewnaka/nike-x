package com.zentora.nike_x.controller.api;

import com.zentora.nike_x.service.OrderService;
import com.zentora.nike_x.util.PaymentConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.FormParam;

@Path("/order")
public class PaymentNotificationController {

    @POST
    @Path("/notify")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response handleNotification(
            @FormParam("merchant_id") String merchantId,
            @FormParam("order_id") String orderId,
            @FormParam("payhere_amount") String amount,
            @FormParam("payhere_currency") String currency,
            @FormParam("status_code") String statusCode,
            @FormParam("md5sig") String md5sig,
            @Context HttpServletRequest req) {

        System.out.println("PayHere Notification Received for Order: " + orderId + " Status: " + statusCode);

        // 1. Verify Hash
        String merchantSecret = PaymentConfig.getMd5(PaymentConfig.MERCHANT_SECRET);
        String localHash = PaymentConfig.getMd5(merchantId + orderId + amount + currency + statusCode + merchantSecret);

        if (!localHash.equalsIgnoreCase(md5sig)) {
            System.err.println("PayHere Hash Validation Failed! Local: " + localHash + " Received: " + md5sig);
            return Response.status(Response.Status.BAD_REQUEST).entity("Hash Mismatch").build();
        }

        // 2. Update Order Status
        OrderService orderService = new OrderService();
        boolean success = orderService.updatePaymentStatus(Integer.parseInt(orderId), Integer.parseInt(statusCode));

        if (success) {
            return Response.ok("OK").build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Update Failed").build();
        }
    }
}
