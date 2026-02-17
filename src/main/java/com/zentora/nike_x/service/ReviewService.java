package com.zentora.nike_x.service;

import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.ReviewDTO;
import com.zentora.nike_x.entity.*;
import com.zentora.nike_x.util.AppUtil;
import com.zentora.nike_x.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class ReviewService {

    public String checkEligibility(int userId, int productId) {
        JsonObject responseObject = new JsonObject();
        boolean eligible = false;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.user.id = :uid AND oi.stock.product.id = :pid AND UPPER(oi.order.status.type) IN ('COMPLETED', 'DELIVERED')",
                    Long.class)
                    .setParameter("uid", userId)
                    .setParameter("pid", productId)
                    .uniqueResult();

            if (count > 0) {
                eligible = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        responseObject.addProperty("status", eligible);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String addReview(ReviewDTO reviewDTO, int userId, String appPath) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            // 1. Validate User
            User user = session.get(User.class, userId);
            if (user == null) {
                message = "User not found";
            } else {
                // 2. Validate Product
                Product product = session.get(Product.class, reviewDTO.getProductId());
                if (product == null) {
                    message = "Product not found";
                } else {
                    // 3. Verify Purchase (Get latest stock bought)
                    // We need a specific stock to link the review to.
                    // Let's get the most recent OrderItem for this product bought by user which is
                    // PAID.
                    String hql = "SELECT oi.stock FROM OrderItem oi " +
                            "WHERE oi.order.user = :user AND oi.stock.product = :product AND UPPER(oi.order.status.type) IN ('PAID', 'COMPLETED', 'DELIVERED') "
                            +
                            "ORDER BY oi.order.dateTime DESC";

                    Stock stock = session.createQuery(hql, Stock.class)
                            .setParameter("user", user)
                            .setParameter("product", product)
                            .setMaxResults(1)
                            .uniqueResult();

                    if (stock == null) {
                        message = "You have not purchased this product.";
                    } else {
                        // 4. Save Review
                        Review review = new Review();
                        review.setUser(user);
                        review.setProduct(product);
                        review.setStock(stock);
                        review.setReview(reviewDTO.getReview());
                        review.setRating(reviewDTO.getRating());

                        session.save(review);

                        // 5. Save Images (if any)
                        if (reviewDTO.getImages() != null && !reviewDTO.getImages().isEmpty()) {
                            // Ensure directory exists
                            String uploadDir = appPath + File.separator + "assets" + File.separator + "images"
                                    + File.separator + "reviews";
                            File dir = new File(uploadDir);
                            if (!dir.exists())
                                dir.mkdirs();

                            for (String base64Image : reviewDTO.getImages()) {
                                if (base64Image != null && !base64Image.isEmpty()) {
                                    // Basic Base64 parsing (assuming "data:image/png;base64,..." format usually,
                                    // needs stripping)
                                    String[] parts = base64Image.split(",");
                                    String imageString = parts.length > 1 ? parts[1] : parts[0];

                                    byte[] imageBytes = Base64.getDecoder().decode(imageString);
                                    String fileName = UUID.randomUUID().toString() + ".png"; // Assuming png for
                                                                                             // simplicity or detect
                                                                                             // type
                                    File file = new File(uploadDir, fileName);

                                    try (FileOutputStream fos = new FileOutputStream(file)) {
                                        fos.write(imageBytes);
                                    }

                                    ReviewImage reviewImage = new ReviewImage();
                                    reviewImage.setReview(review);
                                    reviewImage.setPath("assets/images/reviews/" + fileName);
                                    session.save(reviewImage);
                                }
                            }
                        }

                        transaction.commit();
                        status = true;
                        message = "Review submitted successfully!";
                    }
                }
            }
        } catch (Exception e) {
            if (transaction != null)
                transaction.rollback();
            e.printStackTrace();
            message = "Error submitting review";
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getReviews(int productId) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Review> reviews = session.createQuery(
                    "SELECT r FROM Review r WHERE r.product.id = :pid ORDER BY r.createdAt DESC", Review.class)
                    .setParameter("pid", productId)
                    .list();

            List<ReviewDTO> dtos = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

            for (Review r : reviews) {
                ReviewDTO dto = new ReviewDTO();
                dto.setId(r.getId());
                dto.setReview(r.getReview());
                dto.setRating(r.getRating());
                dto.setUserName(r.getUser().getFirstName() + " " + r.getUser().getLastName());
                dto.setDate(sdf.format(r.getCreatedAt()));

                // Variant info
                String variant = r.getStock().getSize().getName() + " | " + r.getStock().getColor().getName();
                dto.setVariant(variant);

                // Images
                List<String> imgPaths = session.createQuery(
                        "SELECT ri.path FROM ReviewImage ri WHERE ri.review = :r", String.class)
                        .setParameter("r", r)
                        .list();
                dto.setImages(imgPaths);

                dtos.add(dto);
            }

            responseObject.add("reviews", AppUtil.GSON.toJsonTree(dtos));
            status = true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        return AppUtil.GSON.toJson(responseObject);
    }
}
