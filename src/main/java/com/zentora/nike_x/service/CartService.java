package com.zentora.nike_x.service;

import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.CartItemDTO;
import com.zentora.nike_x.entity.Cart;
import com.zentora.nike_x.entity.CartItem;
import com.zentora.nike_x.entity.Stock;
import com.zentora.nike_x.entity.User;
import com.zentora.nike_x.util.AppUtil;
import com.zentora.nike_x.util.HibernateUtil;
import jakarta.servlet.http.HttpSession;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;

public class CartService {

    // --- DB CART OPERATIONS ---

    public String addToDbCart(int userId, int stockId, int qty) {
        JsonObject response = new JsonObject();
        boolean status = false;
        String message;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            // 1. Get or Create Cart
            Cart cart = getOrCreateCart(session, userId);

            // 2. Check if item exists in cart
            Query<CartItem> itemQuery = session.createQuery(
                    "SELECT ci FROM CartItem ci WHERE ci.cart = :cart AND ci.stock.id = :sid", CartItem.class);
            itemQuery.setParameter("cart", cart);
            itemQuery.setParameter("sid", stockId);
            CartItem existingItem = itemQuery.uniqueResult();

            if (existingItem != null) {
                // Update Qty
                existingItem.setQty(existingItem.getQty() + qty);
                session.update(existingItem);
            } else {
                // Add New Item
                Stock stock = session.get(Stock.class, stockId);
                if (stock != null) {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setStock(stock);
                    newItem.setQty(qty);
                    session.save(newItem);
                } else {
                    transaction.rollback();
                    return errorResponse("Stock not found");
                }
            }

            transaction.commit();
            status = true;
            message = "Added to cart";

        } catch (Exception e) {
            e.printStackTrace();
            message = "Error adding to cart";
        }

        response.addProperty("status", status);
        response.addProperty("message", message);
        response.addProperty("stockId", stockId);
        return AppUtil.GSON.toJson(response);
    }

    private Cart getOrCreateCart(Session session, int userId) {
        Query<Cart> query = session.createQuery("SELECT c FROM Cart c WHERE c.user.id = :uid", Cart.class);
        query.setParameter("uid", userId);
        Cart cart = query.uniqueResult();

        if (cart == null) {
            User user = session.get(User.class, userId);
            cart = new Cart();
            cart.setUser(user);
            session.save(cart);
        }
        return cart;
    }

    // --- SESSION CART OPERATIONS ---

    @SuppressWarnings("unchecked")
    public String addToSessionCart(HttpSession httpSession, int stockId, int qty) {
        List<CartItemDTO> sessionCart = (List<CartItemDTO>) httpSession.getAttribute("session_cart");
        if (sessionCart == null) {
            sessionCart = new ArrayList<>();
        }

        boolean found = false;
        for (CartItemDTO item : sessionCart) {
            if (item.getStockId() == stockId) {
                item.setQty(item.getQty() + qty);
                found = true;
                break;
            }
        }

        if (!found) {
            CartItemDTO newItem = new CartItemDTO();
            newItem.setStockId(stockId);
            newItem.setQty(qty);
            sessionCart.add(newItem);
        }

        httpSession.setAttribute("session_cart", sessionCart);

        JsonObject response = new JsonObject();
        response.addProperty("status", true);
        response.addProperty("message", "Added to session cart");
        return AppUtil.GSON.toJson(response);
    }

    // --- MERGE LOGIC ---

    @SuppressWarnings("unchecked")
    public void mergeCarts(int userId, HttpSession httpSession) {
        List<CartItemDTO> sessionCart = (List<CartItemDTO>) httpSession.getAttribute("session_cart");

        if (sessionCart != null && !sessionCart.isEmpty()) {
            System.out.println("Merging " + sessionCart.size() + " items from session to DB for user " + userId);

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Transaction transaction = session.beginTransaction();
                Cart cart = getOrCreateCart(session, userId);

                for (CartItemDTO sessionItem : sessionCart) {
                    // Check DB collision
                    Query<CartItem> itemQuery = session.createQuery(
                            "SELECT ci FROM CartItem ci WHERE ci.cart = :cart AND ci.stock.id = :sid", CartItem.class);
                    itemQuery.setParameter("cart", cart);
                    itemQuery.setParameter("sid", sessionItem.getStockId());
                    CartItem dbItem = itemQuery.uniqueResult();

                    if (dbItem != null) {
                        // Decide: Add active qty or overwrite? Usually add.
                        // But ensure we don't exceed stock limit (not checking here for simplicity)
                        // Actually, let's just make sure we don't double add if logic is complex.
                        // Simple requirement: "if product... isn't available in their db cart i wanna
                        // add".
                        // User said: "if the products on their session cart isn't available in their db
                        // cart i wanna add those"

                        // So if it IS available, do we skip?
                        // "if someone added products to the session cart when they log in if the
                        // products on thier sisson cart isn't available in their db cart i wanna add
                        // those products their db cart"
                        // This implies: If DB has it, DO NOTHING. If DB missing, ADD it.

                        // Logic: Skip existing.
                    } else {
                        Stock stock = session.get(Stock.class, sessionItem.getStockId());
                        if (stock != null) {
                            CartItem newItem = new CartItem();
                            newItem.setCart(cart);
                            newItem.setStock(stock);
                            newItem.setQty(sessionItem.getQty());
                            session.save(newItem);
                        }
                    }
                }

                transaction.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Clear session cart after merge
            httpSession.removeAttribute("session_cart");
        }
    }

    public int getDbCartCount(int userId) {
        int count = 0;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.user.id = :uid", Long.class);
            query.setParameter("uid", userId);
            Long result = query.uniqueResult();
            if (result != null) {
                count = result.intValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public int getSessionCartCount(HttpSession httpSession) {
        List<CartItemDTO> sessionCart = (List<CartItemDTO>) httpSession.getAttribute("session_cart");
        return (sessionCart != null) ? sessionCart.size() : 0;
    }

    private String errorResponse(String msg) {
        JsonObject r = new JsonObject();
        r.addProperty("status", false);
        r.addProperty("message", msg);
        return AppUtil.GSON.toJson(r);
    }

    // --- FULL CART DETAILS ---

    public List<CartItemDTO> getDbCartItems(int userId) {
        List<CartItemDTO> cartItems = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<CartItem> query = session.createQuery(
                    "SELECT ci FROM CartItem ci JOIN FETCH ci.stock s JOIN FETCH s.product p " +
                            "JOIN FETCH s.color c JOIN FETCH s.size sz " +
                            "WHERE ci.cart.user.id = :uid",
                    CartItem.class);
            query.setParameter("uid", userId);
            List<CartItem> dbItems = query.list();

            for (CartItem dbItem : dbItems) {
                CartItemDTO dto = new CartItemDTO();
                dto.setStockId(dbItem.getStock().getId());
                dto.setQty(dbItem.getQty());

                // Populate Product Details for Frontend
                Stock s = dbItem.getStock();
                dto.setProductId(s.getProduct().getId());
                dto.setProductName(s.getProduct().getName());
                dto.setPrice(s.getSellingPrice()); // Use Stock specific price
                dto.setSizeId(s.getSize().getId());
                dto.setSizeName(s.getSize().getName());
                dto.setColorId(s.getColor().getId());
                dto.setColorName(s.getColor().getName());
                dto.setColorHex(s.getColor().getName());

                // Get image
                // Basic logic: Get first image of product or stock specific if available
                // Assuming simple product image for now due to complexity of fetching active
                // stock image
                // Or better: Use the product's first image in list

                // Warning: Lazy loading might be an issue if we don't fetch images
                // Let's rely on frontend fetching or just send one image path if possible
                // For now, let's keep it simple. Frontend can fetch product details if needed,
                // OR better, exposed image path here.
                // Assuming Product entity has activeImages list.
                // We'll manually fetch one image.

                // Efficient way:
                Query<String> imgQuery = session.createQuery(
                        "SELECT pi.path FROM ProductImage pi WHERE pi.product.id = :pid ORDER BY pi.id ASC",
                        String.class);
                imgQuery.setParameter("pid", s.getProduct().getId());
                imgQuery.setMaxResults(1);
                List<String> images = imgQuery.list();
                if (!images.isEmpty()) {
                    dto.setImagePath(images.get(0));
                }

                cartItems.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cartItems;
    }

    public List<CartItemDTO> getSessionCartItems(HttpSession httpSession) {
        List<CartItemDTO> sessionCart = (List<CartItemDTO>) httpSession.getAttribute("session_cart");
        if (sessionCart == null || sessionCart.isEmpty()) {
            return new ArrayList<>();
        }

        List<CartItemDTO> fullDetails = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            for (CartItemDTO item : sessionCart) {
                Stock s = session.get(Stock.class, item.getStockId());
                if (s != null) {
                    CartItemDTO dto = new CartItemDTO();
                    dto.setStockId(s.getId());
                    dto.setQty(item.getQty());

                    dto.setProductId(s.getProduct().getId());
                    dto.setProductName(s.getProduct().getName());
                    dto.setPrice(s.getSellingPrice());
                    dto.setSizeId(s.getSize().getId());
                    dto.setSizeName(s.getSize().getName());
                    dto.setColorId(s.getColor().getId());
                    dto.setColorName(s.getColor().getName());
                    dto.setColorHex(s.getColor().getName());

                    Query<String> imgQuery = session.createQuery(
                            "SELECT pi.path FROM ProductImage pi WHERE pi.product.id = :pid ORDER BY pi.id ASC",
                            String.class);
                    imgQuery.setParameter("pid", s.getProduct().getId());
                    imgQuery.setMaxResults(1);
                    List<String> images = imgQuery.list();
                    if (!images.isEmpty()) {
                        dto.setImagePath(images.get(0));
                    }

                    fullDetails.add(dto);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fullDetails;
    }

    public String updateCartItemQty(int userId, HttpSession httpSession, int stockId, int newQty) {
        JsonObject response = new JsonObject();
        boolean success = false;

        if (userId > 0) {
            // DB Update
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Transaction transaction = session.beginTransaction();
                Query<CartItem> query = session.createQuery(
                        "SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :uid AND ci.stock.id = :sid",
                        CartItem.class);
                query.setParameter("uid", userId);
                query.setParameter("sid", stockId);
                CartItem item = query.uniqueResult();

                if (item != null) {
                    if (newQty > 0) {
                        item.setQty(newQty);
                        session.update(item);
                        success = true;
                    } else {
                        // Remove if 0
                        session.delete(item);
                        success = true;
                    }
                }
                transaction.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Session Update
            List<CartItemDTO> sessionCart = (List<CartItemDTO>) httpSession.getAttribute("session_cart");
            if (sessionCart != null) {
                // If qty <= 0, remove
                if (newQty <= 0) {
                    sessionCart.removeIf(i -> i.getStockId() == stockId);
                    success = true;
                } else {
                    for (CartItemDTO item : sessionCart) {
                        if (item.getStockId() == stockId) {
                            item.setQty(newQty);
                            success = true;
                            break;
                        }
                    }
                }
                httpSession.setAttribute("session_cart", sessionCart);
            }
        }

        response.addProperty("status", success);
        return AppUtil.GSON.toJson(response);
    }

    public String removeCartItem(int userId, HttpSession httpSession, int stockId) {
        return updateCartItemQty(userId, httpSession, stockId, 0); // Reuse logic with 0 qty
    }
}
