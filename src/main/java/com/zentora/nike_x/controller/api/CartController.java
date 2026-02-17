package com.zentora.nike_x.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.UserDTO;
import com.zentora.nike_x.service.CartService;
import com.zentora.nike_x.util.AppUtil;
import com.zentora.nike_x.util.HibernateUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

@Path("/cart")
public class CartController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCartItems(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserDTO user = (UserDTO) session.getAttribute("user");
        CartService cartService = new CartService();

        List<com.zentora.nike_x.dto.CartItemDTO> items;

        if (user != null) {
            items = cartService.getDbCartItems(user.getId());
        } else {
            items = cartService.getSessionCartItems(session);
        }

        return Response.ok(AppUtil.GSON.toJson(items)).build();
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCartCount(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserDTO user = (UserDTO) session.getAttribute("user");
        CartService cartService = new CartService();
        int count = 0;

        if (user != null) {
            count = cartService.getDbCartCount(user.getId());
        } else {
            count = cartService.getSessionCartCount(session);
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("status", true);
        resp.addProperty("count", count);

        return Response.ok(AppUtil.GSON.toJson(resp)).build();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addToCart(String jsonBody, @Context HttpServletRequest request) {
        try {
            JsonObject json = new Gson().fromJson(jsonBody, JsonObject.class);
            if (!json.has("qty")) {
                return Response.ok(AppUtil.GSON.toJson(getError("Missing info: qty"))).build();
            }

            int qty = json.get("qty").getAsInt();
            int stockId = -1;

            if (json.has("stockId")) {
                stockId = json.get("stockId").getAsInt();
            } else if (json.has("productId")) {
                // Resolve Stock from Product, Color, Size
                int pid = json.get("productId").getAsInt();

                int cid = json.has("colorId") ? json.get("colorId").getAsInt() : -1;
                int sid = json.has("sizeId") ? json.get("sizeId").getAsInt() : -1;

                try (Session hibSession = HibernateUtil.getSessionFactory().openSession()) {
                    // Find active stock (Status 1 = Active usually)
                    StringBuilder hql = new StringBuilder(
                            "SELECT s.id FROM Stock s WHERE s.product.id = :pid AND s.status.id = 1");

                    if (cid != -1)
                        hql.append(" AND s.color.id = :cid");
                    if (sid != -1)
                        hql.append(" AND s.size.id = :sid");

                    // If vague selection, order by ID to get deterministic first one
                    hql.append(" ORDER BY s.id ASC");

                    Query<Integer> q = hibSession.createQuery(hql.toString(), Integer.class);
                    q.setParameter("pid", pid);
                    if (cid != -1)
                        q.setParameter("cid", cid);
                    if (sid != -1)
                        q.setParameter("sid", sid);

                    // Limit 1
                    q.setMaxResults(1);

                    List<Integer> ids = q.list();
                    if (!ids.isEmpty()) {
                        stockId = ids.get(0);
                    }
                }
            }

            if (stockId == -1) {
                return Response.ok(AppUtil.GSON.toJson(getError("Stock not found for selection"))).build();
            }

            HttpSession session = request.getSession();
            UserDTO user = (UserDTO) session.getAttribute("user");

            CartService cartService = new CartService();
            String result;

            if (user != null) {
                // Logged In: Add to DB
                result = cartService.addToDbCart(user.getId(), stockId, qty);
            } else {
                // Guest: Add to Session
                result = cartService.addToSessionCart(session, stockId, qty);
            }

            return Response.ok(result).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(AppUtil.GSON.toJson(getError("Server error"))).build();
        }
    }

    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCartItem(String jsonBody, @Context HttpServletRequest request) {
        try {
            JsonObject json = new Gson().fromJson(jsonBody, JsonObject.class);
            int stockId = json.get("stockId").getAsInt();
            int qty = json.get("qty").getAsInt();

            HttpSession session = request.getSession();
            UserDTO user = (UserDTO) session.getAttribute("user");
            CartService cartService = new CartService();

            String result = cartService.updateCartItemQty(user != null ? user.getId() : 0, session, stockId, qty);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.ok(AppUtil.GSON.toJson(getError("Invalid request"))).build();
        }
    }

    @POST
    @Path("/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeCartItem(String jsonBody, @Context HttpServletRequest request) {
        try {
            JsonObject json = new Gson().fromJson(jsonBody, JsonObject.class);
            int stockId = json.get("stockId").getAsInt();

            HttpSession session = request.getSession();
            UserDTO user = (UserDTO) session.getAttribute("user");
            CartService cartService = new CartService();

            String result = cartService.removeCartItem(user != null ? user.getId() : 0, session, stockId);
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
