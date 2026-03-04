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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CartService {
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);


    public String addToDbCart(int userId, int stockId, int qty) {
        JsonObject response = new JsonObject();
        boolean status = false;
        String message;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            Cart cart = getOrCreateCart(session, userId);

            Query<CartItem> itemQuery = session.createQuery(
                    "SELECT ci FROM CartItem ci WHERE ci.cart = :cart AND ci.stock.id = :sid", CartItem.class);
            itemQuery.setParameter("cart", cart);
            itemQuery.setParameter("sid", stockId);
            CartItem existingItem = itemQuery.uniqueResult();

            if (existingItem != null) {
                existingItem.setQty(existingItem.getQty() + qty);
                session.update(existingItem);
            } else {
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
            logger.error("Exception occurred: ", e);
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


    @SuppressWarnings("unchecked")
    public void mergeCarts(int userId, HttpSession httpSession) {
        List<CartItemDTO> sessionCart = (List<CartItemDTO>) httpSession.getAttribute("session_cart");

        if (sessionCart != null && !sessionCart.isEmpty()) {
            logger.info("Merging {} items from session to DB for user {}", sessionCart.size(), userId);

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Transaction transaction = session.beginTransaction();
                Cart cart = getOrCreateCart(session, userId);

                for (CartItemDTO sessionItem : sessionCart) {
                    Query<CartItem> itemQuery = session.createQuery(
                            "SELECT ci FROM CartItem ci WHERE ci.cart = :cart AND ci.stock.id = :sid", CartItem.class);
                    itemQuery.setParameter("cart", cart);
                    itemQuery.setParameter("sid", sessionItem.getStockId());
                    CartItem dbItem = itemQuery.uniqueResult();

                    if (dbItem != null) {


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
                logger.error("Exception occurred: ", e);
            }

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
            logger.error("Exception occurred: ", e);
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

                Stock s = dbItem.getStock();
                dto.setProductId(s.getProduct().getId());
                dto.setProductName(s.getProduct().getName());
                dto.setPrice(s.getSellingPrice()); // Use Stock specific price
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

                cartItems.add(dto);
            }
        } catch (Exception e) {
            logger.error("Exception occurred: ", e);
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
            logger.error("Exception occurred: ", e);
        }
        return fullDetails;
    }

    public String updateCartItemQty(int userId, HttpSession httpSession, int stockId, int newQty) {
        JsonObject response = new JsonObject();
        boolean success = false;

        if (userId > 0) {
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
                        session.delete(item);
                        success = true;
                    }
                }
                transaction.commit();
            } catch (Exception e) {
                logger.error("Exception occurred: ", e);
            }
        } else {
            List<CartItemDTO> sessionCart = (List<CartItemDTO>) httpSession.getAttribute("session_cart");
            if (sessionCart != null) {
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
