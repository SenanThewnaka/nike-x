package com.zentora.nike_x.service;

import com.google.gson.JsonObject;
import com.zentora.nike_x.entity.Product;
import com.zentora.nike_x.entity.User;
import com.zentora.nike_x.entity.Wishlist;
import com.zentora.nike_x.util.AppUtil;
import com.zentora.nike_x.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class WishlistService {

    public String addToWishlist(int userId, int productId) {
        JsonObject response = new JsonObject();
        boolean status = false;
        String message;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Check if already exists
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(w) FROM Wishlist w WHERE w.user.id = :uid AND w.product.id = :pid", Long.class);
            query.setParameter("uid", userId);
            query.setParameter("pid", productId);
            Long count = query.uniqueResult();

            if (count > 0) {
                message = "Product already in wishlist";
            } else {
                Transaction transaction = session.beginTransaction();

                User user = session.get(User.class, userId);
                Product product = session.get(Product.class, productId);

                if (user != null && product != null) {
                    Wishlist wishlist = new Wishlist();
                    wishlist.setUser(user);
                    wishlist.setProduct(product);
                    session.save(wishlist);

                    transaction.commit();
                    status = true;
                    message = "Added to wishlist";
                } else {
                    message = "Invalid user or product";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            message = "Error adding to wishlist";
        }

        response.addProperty("status", status);
        response.addProperty("message", message);
        return AppUtil.GSON.toJson(response);
    }

    public String removeFromWishlist(int userId, int productId) {
        JsonObject response = new JsonObject();
        boolean status = false;
        String message;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("DELETE FROM Wishlist w WHERE w.user.id = :uid AND w.product.id = :pid");
            query.setParameter("uid", userId);
            query.setParameter("pid", productId);

            int result = query.executeUpdate();

            if (result > 0) {
                transaction.commit();
                status = true;
                message = "Removed from wishlist";
            } else {
                message = "Item not found in wishlist";
            }
        } catch (Exception e) {
            e.printStackTrace();
            message = "Error removing from wishlist";
        }

        response.addProperty("status", status);
        response.addProperty("message", message);
        return AppUtil.GSON.toJson(response);
    }

    public String checkStatus(int userId, int productId) {
        JsonObject response = new JsonObject();
        boolean status = false;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(w) FROM Wishlist w WHERE w.user.id = :uid AND w.product.id = :pid", Long.class);
            query.setParameter("uid", userId);
            query.setParameter("pid", productId);
            Long count = query.uniqueResult();

            if (count > 0)
                status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.addProperty("status", status);
        return AppUtil.GSON.toJson(response);
    }

    public String getWishlistItems(int userId) {
        JsonObject response = new JsonObject();
        boolean status = false;
        String message;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Wishlist> items = session.createQuery(
                            "SELECT w FROM Wishlist w JOIN FETCH w.product p JOIN FETCH p.brand WHERE w.user.id = :uid ORDER BY w.createdAt DESC",
                            Wishlist.class)
                    .setParameter("uid", userId)
                    .list();

            com.google.gson.JsonArray products = new com.google.gson.JsonArray();

            for (Wishlist w : items) {
                Product p = w.getProduct();
                JsonObject item = new JsonObject();
                item.addProperty("id", p.getId());
                item.addProperty("name", p.getName());
                item.addProperty("brand", p.getBrand().getName());
                item.addProperty("model", p.getModel().getName());

                // Price
                Double minPrice = session.createQuery(
                                "SELECT MIN(s.sellingPrice) FROM Stock s WHERE s.product = :p AND s.status.type = 'Active' AND s.qty > 0",
                                Double.class)
                        .setParameter("p", p)
                        .uniqueResult();
                item.addProperty("price", minPrice != null ? minPrice : 0.0);

                // Image
                List<String> images = session.createQuery(
                                "SELECT pi.path FROM ProductImage pi WHERE pi.product = :p", String.class)
                        .setParameter("p", p)
                        .setMaxResults(1)
                        .list();
                if (!images.isEmpty()) {
                    item.addProperty("imagePath", images.get(0));
                }

                products.add(item);
            }

            response.add("items", products);
            status = true;
            message = "Wishlist fetched";

        } catch (Exception e) {
            e.printStackTrace();
            message = "Error fetching wishlist";
        }

        response.addProperty("status", status);
        response.addProperty("message", message);
        return AppUtil.GSON.toJson(response);
    }

    public List<Integer> getWishlistProductIds(int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT w.product.id FROM Wishlist w WHERE w.user.id = :uid", Integer.class)
                    .setParameter("uid", userId)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
}
