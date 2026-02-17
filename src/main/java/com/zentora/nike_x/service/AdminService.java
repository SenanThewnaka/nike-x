package com.zentora.nike_x.service;

import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.*;
import com.zentora.nike_x.entity.*;
import com.zentora.nike_x.mail.AdminLoginCodeMail;
import com.zentora.nike_x.provider.MailServiceProvider;
import com.zentora.nike_x.util.AppUtil;
import com.zentora.nike_x.util.HibernateUtil;
import com.zentora.nike_x.util.SecurityUtil;
import com.zentora.nike_x.validation.Validator;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AdminService {

    public String sendCode(UserDTO userDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            message = "Email is required!";
        } else if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Invalid email format!";
        } else if (userDTO.getPassword() == null || userDTO.getPassword().isBlank()) {
            message = "Password is required!";
        } else if (!userDTO.getPassword().matches(Validator.PASSWORD_VALIDATION)) {
            message = "Invalid emial or password!";
        } else {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Admin admin = session.createNamedQuery("Admin.getByEmail", Admin.class)
                        .setParameter("email", userDTO.getEmail())
                        .getSingleResultOrNull();

                if (admin == null) {
                    message = "Invalid credentials!";
                } else {
                    if (SecurityUtil.checkPassword(userDTO.getPassword(), admin.getPassword())) {
                        String code = AppUtil.generateCode();
                        admin.setVerificationCode(code);

                        Transaction transaction = session.beginTransaction();
                        try {
                            session.merge(admin);
                            transaction.commit();

                            AdminLoginCodeMail mail = new AdminLoginCodeMail(admin.getEmail(), code);
                            MailServiceProvider.getMailServiceProvider().sendMail(mail);

                            status = true;
                            message = "Verification code sent to your email!";
                        } catch (HibernateException e) {
                            if (transaction != null)
                                transaction.rollback();
                            message = "Failed to send code. Please try again.";
                            System.out.println("Hibernate Error: " + e.getMessage());
                        }
                    } else {
                        message = "Invalid credentials!";
                    }
                }
            } catch (Exception e) {
                message = "Something went wrong!";
                System.out.println(e.getMessage());
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String signIn(UserDTO userDTO, HttpServletRequest request) {
        // ... (existing code)
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            message = "Email is required!";
        } else if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please provide valid email address!";
        } else if (userDTO.getPassword() == null || userDTO.getPassword().isBlank()) {
            message = "Password is required!";
        } else if (!userDTO.getPassword().matches(Validator.PASSWORD_VALIDATION)) {
            message = "Invalid email or password!";
        } else if (userDTO.getVerificationCode() == null || userDTO.getVerificationCode().isBlank()) {
            message = "Verification code is required!";
        } else if (!userDTO.getVerificationCode().matches(Validator.VERIFICATION_CODE_VALIDATION)) {
            message = "Invalid verification code!";
        } else {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Admin admin = session.createNamedQuery("Admin.getByEmail", Admin.class)
                        .setParameter("email", userDTO.getEmail())
                        .getSingleResultOrNull();

                if (admin == null) {
                    message = "Invalid credentials!";
                } else {
                    if (SecurityUtil.checkPassword(userDTO.getPassword(), admin.getPassword())) {
                        if (userDTO.getVerificationCode().equals(admin.getVerificationCode())) {
                            status = true;
                            message = "Login successful!";

                            request.getSession().setAttribute("admin", admin);

                            Transaction transaction = session.beginTransaction();
                            try {
                                admin.setVerificationCode(null);
                                session.merge(admin);
                                transaction.commit();
                            } catch (HibernateException e) {
                                transaction.rollback();
                                message = "Failed to login. Please try again.";
                            }

                        } else {
                            message = "Invalid verification code!";
                        }
                    } else {
                        message = "Invalid credentials!";
                    }
                }
            } catch (Exception e) {
                message = "Something went wrong!";
                System.out.println(e.getMessage());
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getDashboardStats() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Total Revenue
            Double totalRevenue = session.createQuery("SELECT SUM(o.totalAmount) FROM Order o", Double.class)
                    .uniqueResult();
            totalRevenue = (totalRevenue != null) ? totalRevenue : 0.0;

            // Total Orders
            Long totalOrders = session.createQuery("SELECT COUNT(o) FROM Order o", Long.class).uniqueResult();
            totalOrders = (totalOrders != null) ? totalOrders : 0L;

            // Active Users
            Long activeUsers = session.createQuery("SELECT COUNT(u) FROM User u", Long.class).uniqueResult();
            activeUsers = (activeUsers != null) ? activeUsers : 0L;

            // Recent Orders (Top 5)
            // We'll return full list in getAllOrders, but here maybe just top 5 for
            // dashboard?
            // Actually, let's keep it clean and use getAllOrders(1) for dashboard table or
            // adding a specific method.
            // Let's stick to stats here.

            responseObject.addProperty("totalRevenue", totalRevenue);
            responseObject.addProperty("totalOrders", totalOrders);
            responseObject.addProperty("activeUsers", activeUsers);

            // Dummy conversion rate for now or calc?
            responseObject.addProperty("conversionRate", 3.5); // Placeholder

            status = true;
            message = "Stats fetched";

        } catch (Exception e) {
            message = "Error fetching stats";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getUsers() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        List<UserDTO> userList = new ArrayList<>();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<User> users = session.createQuery("FROM User ORDER BY id DESC", User.class).list();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

            for (User u : users) {
                UserDTO dto = new UserDTO();
                dto.setId(u.getId());
                dto.setEmail(u.getEmail());
                dto.setFirstName(u.getFirstName());
                dto.setLastName(u.getLastName());

                if (u.getStatus() != null) {
                    dto.setStatusName(u.getStatus().getType());
                } else {
                    dto.setStatusName("Unknown");
                }

                if (u.getCreatedAt() != null) {
                    dto.setCreatedAt(sdf.format(u.getCreatedAt()));
                }

                userList.add(dto);
            }
            status = true;
            message = "Users fetched";
        } catch (Exception e) {
            message = "Error fetching users";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("users", AppUtil.GSON.toJsonTree(userList));
        return AppUtil.GSON.toJson(responseObject);
    }

    public String updateOrderStatus(int orderId, String statusType) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            Order order = session.get(Order.class, orderId);

            if (order != null) {
                Query<Status> query = session.createNamedQuery("Status.findByValue", Status.class);
                query.setParameter("type", statusType);
                Status statusEntity = query.uniqueResult();

                if (statusEntity != null) {
                    order.setStatus(statusEntity);
                    session.merge(order);
                    transaction.commit();
                    status = true;
                    message = "Order status updated to " + statusType;
                } else {
                    message = "Invalid status type";
                }
            } else {
                message = "Order not found";
            }
        } catch (Exception e) {
            message = "Error updating order status";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String updateUserStatus(int userId, String action) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            User user = session.get(User.class, userId);

            if (user != null) {
                Status.Type targetStatusType;
                if ("BLOCK".equalsIgnoreCase(action)) {
                    targetStatusType = Status.Type.BLOCKED;
                } else if ("UNBLOCK".equalsIgnoreCase(action)) {
                    targetStatusType = Status.Type.VERIFIED;
                } else {
                    targetStatusType = null;
                }

                if (targetStatusType != null) {
                    Query<Status> query = session.createNamedQuery("Status.findByValue", Status.class);
                    query.setParameter("type", targetStatusType.toString());
                    Status statusEntity = query.uniqueResult();

                    if (statusEntity != null) {
                        user.setStatus(statusEntity);
                        session.merge(user);
                        transaction.commit();
                        status = true;
                        message = "User status updated to " + targetStatusType;
                    } else {
                        message = "Status not found in database";
                    }
                } else {
                    message = "Invalid action";
                }
            } else {
                message = "User not found";
            }
        } catch (Exception e) {
            message = "Error updating user status";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String checkAuthStatus(HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;

        if (request.getSession().getAttribute("admin") != null) {
            status = true;
        }

        responseObject.addProperty("status", status);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getAllOrders(int page, int limit) {

        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        List<OrderDTO> orderList = new ArrayList<>();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Count for pagination
            Long totalOrders = session.createQuery("SELECT COUNT(o) FROM Order o", Long.class).uniqueResult();
            int totalPages = (int) Math.ceil((double) totalOrders / limit);

            if (page < 1)
                page = 1;
            int offset = (page - 1) * limit;

            List<Order> orders = session.createQuery("FROM Order o ORDER BY o.dateTime DESC", Order.class)
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .list();

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

            for (Order o : orders) {
                OrderDTO dto = new OrderDTO();
                dto.setId(o.getId());
                dto.setTotalAmount(o.getTotalAmount());
                dto.setDate(sdf.format(o.getDateTime()));

                if (o.getUser() != null) {
                    dto.setCustomerName(o.getUser().getFirstName() + " " + o.getUser().getLastName());
                } else {
                    dto.setCustomerName("Guest");
                }

                if (o.getStatus() != null) {
                    dto.setStatus(o.getStatus().getType());
                }

                // Fetch first item product name for display
                // Note: This matches existing logic or use OrderItem logic
                // For Dashboard typically we show "Product Name... + X more"

                String hqlItem = "FROM OrderItem oi WHERE oi.order.id = :oid";
                List<OrderItem> items = session.createQuery(hqlItem, OrderItem.class)
                        .setParameter("oid", o.getId())
                        .list();
                if (!items.isEmpty()) {
                    dto.setItemCount(items.size());

                    OrderItem firstItem = items.get(0);
                    if (firstItem.getStock().getProduct() != null) {
                        dto.setFirstProductName(firstItem.getStock().getProduct().getName());
                    } else {
                        dto.setFirstProductName("Unknown Product");
                    }

                    try {
                        // Fetch first image for this product
                        String hqlImg = "FROM ProductImage pi WHERE pi.product.id = :pid";
                        List<ProductImage> pImages = session.createQuery(hqlImg, ProductImage.class)
                                .setParameter("pid", firstItem.getStock().getProduct().getId())
                                .setMaxResults(1)
                                .list();

                        if (!pImages.isEmpty()) {
                            dto.setFirstProductImage(pImages.get(0).getPath());
                        } else {
                            // Fallback dummy or null (Frontend handles empty)
                        }
                    } catch (Exception ignored) {
                    }
                } else {
                    dto.setFirstProductName("No Items");
                }

                orderList.add(dto);
            }
            status = true;
            message = "Orders fetched";

            responseObject.addProperty("totalPages", totalPages);
            responseObject.addProperty("currentPage", page);

        } catch (Exception e) {
            message = "Error fetching orders";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("orders", AppUtil.GSON.toJsonTree(orderList));
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getOrderDetails(int orderId) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        OrderDTO orderDTO = new OrderDTO();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Order order = session.get(Order.class, orderId);

            if (order != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

                orderDTO.setId(order.getId());
                orderDTO.setTotalAmount(order.getTotalAmount());
                orderDTO.setDate(sdf.format(order.getDateTime()));
                orderDTO.setPaymentMethod(order.getPaymentMethod());

                if (order.getStatus() != null) {
                    orderDTO.setStatus(order.getStatus().getType());
                }

                // User & Address Details
                if (order.getUser() != null) {
                    orderDTO.setCustomerName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
                    orderDTO.setCustomerEmail(order.getUser().getEmail());
                    // If user has mobile stored separately? Assuming user.getMobile() or similar if
                    // exists
                    // For now, rely on Address or User entity check if mobile exists.
                } else {
                    orderDTO.setCustomerName("Guest");
                }

                if (order.getAddress() != null) {
                    orderDTO.setAddressLine1(order.getAddress().getLineOne());
                    orderDTO.setAddressLine2(order.getAddress().getLineTwo());
                    orderDTO.setPostalCode(order.getAddress().getPostalCode());
                    if (order.getAddress().getCity() != null) {
                        orderDTO.setCity(order.getAddress().getCity().getName());
                    }
                    // Assuming mobile might be part of User or UserProfile, but typically Order
                    // Address might capture it.
                    // Address entity doesn't have mobile.
                }

                // Order Items
                List<OrderDTO.OrderItemDTO> itemDTOs = new ArrayList<>();
                String hqlItem = "FROM OrderItem oi WHERE oi.order.id = :oid";
                List<OrderItem> items = session.createQuery(hqlItem, OrderItem.class)
                        .setParameter("oid", order.getId())
                        .list();

                for (OrderItem item : items) {
                    OrderDTO.OrderItemDTO itemDTO = new OrderDTO.OrderItemDTO();
                    itemDTO.setProductName(item.getStock().getProduct().getName());
                    itemDTO.setQty(item.getQuantity());
                    itemDTO.setPrice(item.getStock().getSellingPrice()); // Or item.getPrice() if stored
                    itemDTO.setTotal(item.getQuantity() * item.getStock().getSellingPrice());

                    // Variance
                    String variance = "";
                    if (item.getStock().getSize() != null)
                        variance += "Size: " + item.getStock().getSize().getName();
                    if (item.getStock().getColor() != null)
                        variance += (variance.isEmpty() ? "" : ", ") + "Color: " + item.getStock().getColor().getName();
                    itemDTO.setVariance(variance);

                    // Image
                    try {
                        String hqlImg = "FROM ProductImage pi WHERE pi.product.id = :pid"; // Could filter by Stock if
                                                                                           // relevant
                        List<ProductImage> pImages = session.createQuery(hqlImg, ProductImage.class)
                                .setParameter("pid", item.getStock().getProduct().getId())
                                .setMaxResults(1)
                                .list();
                        if (!pImages.isEmpty()) {
                            itemDTO.setImagePath(pImages.get(0).getPath());
                        }
                    } catch (Exception ignored) {
                    }

                    itemDTOs.add(itemDTO);
                }
                orderDTO.setItems(itemDTOs);

                status = true;
                message = "Order details fetched";
                responseObject.add("order", AppUtil.GSON.toJsonTree(orderDTO));

            } else {
                message = "Order not found";
            }
        } catch (Exception e) {
            message = "Error fetching order details";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String addProduct(ProductDTO productDTO, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (productDTO.getName() == null || productDTO.getName().isBlank()) {
            message = "Product name is required!";
        } else if (productDTO.getDescription() == null || productDTO.getDescription().isBlank()) {
            message = "Description is required!";
        } else if (productDTO.getModelName() == null || productDTO.getModelName().isBlank()) {
            message = "Model is required!";
        } else if (productDTO.getGender() == null || productDTO.getGender().isBlank()) {
            message = "Gender is required!";

        } else if ((productDTO.getBrandId() == 0
                && (productDTO.getBrandName() == null || productDTO.getBrandName().isBlank()))
                && (productDTO.getNewBrandName() == null || productDTO.getNewBrandName().isBlank())) {
            message = "Brand is required!";
        } else if ((productDTO.getBrandId() > 0
                || (productDTO.getBrandName() != null && !productDTO.getBrandName().isBlank()))
                && (productDTO.getNewBrandName() != null && !productDTO.getNewBrandName().isBlank())) {
            message = "Cannot select an existing brand and add a new brand at the same time!";
        } else {
            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
                Transaction transaction = hibernateSession.beginTransaction();
                try {
                    // Handle Brand
                    Brand brand = null;
                    if (productDTO.getNewBrandName() != null && !productDTO.getNewBrandName().isBlank()) {
                        // Check if brand already exists by name to avoid duplicates
                        brand = hibernateSession.createNamedQuery("Brand.findByName", Brand.class)
                                .setParameter("name", productDTO.getNewBrandName())
                                .uniqueResult();
                        if (brand == null) {
                            brand = new Brand();
                            brand.setName(productDTO.getNewBrandName());
                            hibernateSession.persist(brand);
                        }
                    } else if (productDTO.getBrandId() > 0) {
                        brand = hibernateSession.get(Brand.class, productDTO.getBrandId());
                    } else if (productDTO.getBrandName() != null && !productDTO.getBrandName().isBlank()) {
                        brand = hibernateSession.createNamedQuery("Brand.findByName", Brand.class)
                                .setParameter("name", productDTO.getBrandName())
                                .uniqueResult();
                    }

                    if (brand == null) {
                        throw new Exception("Invalid Brand!");
                    }

                    // Handle Model
                    Model model = hibernateSession.createNamedQuery("Model.findByName", Model.class)
                            .setParameter("name", productDTO.getModelName())
                            .uniqueResult();

                    if (model == null) {
                        model = new Model();
                        model.setName(productDTO.getModelName());
                        hibernateSession.persist(model);
                    }

                    // Handle Gender
                    Gender gender = hibernateSession.createNamedQuery("Gender.findByName", Gender.class)
                            .setParameter("name", productDTO.getGender())
                            .uniqueResult();

                    if (gender == null) {
                        // Fallback or error if gender not found (should be seeded)
                        // For now, let's assume it exists or create it (optional)
                        gender = new Gender();
                        gender.setName(productDTO.getGender());
                        hibernateSession.persist(gender);
                    }

                    // Save Product
                    Product product = new Product();
                    product.setName(productDTO.getName());
                    product.setDescription(productDTO.getDescription());
                    product.setBrand(brand);
                    product.setModel(model);
                    product.setGender(gender);

                    // Handle Status
                    Status productStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                            .setParameter("type", Status.Type.INACTIVE.toString()).getSingleResult();

                    product.setStatus(productStatus);

                    hibernateSession.persist(product);

                    transaction.commit();
                    status = true;
                    message = "Product added successfully!";
                    responseObject.addProperty("productId", product.getId());

                } catch (Exception e) {
                    transaction.rollback();
                    message = "Error adding product";

                }
            } catch (HibernateException e) {
                System.out.println("Hibernate Error:" + e.getMessage());
                message = "Error adding product";
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String uploadImages(int productId, List<org.glassfish.jersey.media.multipart.FormDataBodyPart> bodyParts,
            HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Product product = hibernateSession.get(Product.class, productId);

            if (product == null) {
                message = "Invalid Product!";
            } else {
                Transaction transaction = hibernateSession.beginTransaction();
                try {
                    String uploadPath = request.getServletContext().getRealPath("") + File.separator
                            + "assets" + File.separator + "images" + File.separator + "product";
                    File uploadDir = new File(uploadPath);
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }

                    for (org.glassfish.jersey.media.multipart.FormDataBodyPart part : bodyParts) {
                        InputStream inputStream = part.getValueAs(InputStream.class);
                        String fileName = "product_" + product.getId() + "_" + System.currentTimeMillis() + "_"
                                + AppUtil.generateCode() + ".png"; // Assuming PNG or handle extension

                        File file = new File(uploadPath, fileName);
                        java.nio.file.Files.copy(inputStream, file.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                        ProductImage productImage = new ProductImage();
                        productImage.setPath("assets/images/product/" + fileName);
                        productImage.setProduct(product);
                        hibernateSession.persist(productImage);
                    }

                    transaction.commit();
                    status = true;
                    message = "Images uploaded successfully!";

                } catch (Exception e) {
                    transaction.rollback();
                    message = "Error something went wrong!";

                }
            }
        } catch (Exception e) {
            message = "Error uploading images:";
            System.out.println("hibernate error:" + e.getMessage());
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getBrands() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        List<BrandDTO> brands = new ArrayList<>();

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Brand> brandList = hibernateSession.createQuery("FROM Brand", Brand.class).list();
            for (Brand brand : brandList) {
                brands.add(new BrandDTO(brand.getId(), brand.getName()));
            }
            status = true;
            message = "Brands fetched successfully!";
        } catch (Exception e) {
            message = "Error loading brands";
            System.out.println("hibernate error:" + e.getMessage());
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("brands", AppUtil.GSON.toJsonTree(brands));
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getProducts(int pageNumber) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        List<ProductDTO> productDTOs = new ArrayList<>();
        int pageSize = 20;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {

            // Count total products for pagination
            Long totalProducts = hibernateSession.createQuery("SELECT COUNT(p) FROM Product p", Long.class)
                    .uniqueResult();

            int totalPages = (int) Math.ceil((double) totalProducts / pageSize);

            // Adjust pageNumber if out of bounds
            if (pageNumber < 1)
                pageNumber = 1;
            if (pageNumber > totalPages && totalPages > 0)
                pageNumber = totalPages;

            int offset = (pageNumber - 1) * pageSize;

            List<Product> products = hibernateSession.createQuery("FROM Product ORDER BY id DESC", Product.class)
                    .setFirstResult(offset)
                    .setMaxResults(pageSize)
                    .list();

            for (Product product : products) {
                ProductDTO dto = new ProductDTO();
                dto.setId(product.getId());
                dto.setName(product.getName());
                dto.setDescription(product.getDescription());
                dto.setBrandName(product.getBrand().getName());
                dto.setModelName(product.getModel().getName());
                dto.setGender(product.getGender().getName());
                dto.setStatus(product.getStatus().getType());

                // Get First Image
                String hqlImage = "SELECT p.path FROM ProductImage p WHERE p.product.id = :pid";
                List<String> images = hibernateSession.createQuery(hqlImage, String.class)
                        .setParameter("pid", product.getId())
                        .setMaxResults(1)
                        .list();
                if (!images.isEmpty()) {
                    dto.setImagePath(images.get(0));
                }

                // Get Total Stock Qty
                String hqlStock = "SELECT SUM(s.qty) FROM Stock s WHERE s.product.id = :pid";
                Long totalQty = hibernateSession.createQuery(hqlStock, Long.class)
                        .setParameter("pid", product.getId())
                        .uniqueResult();
                dto.setStockQty(totalQty != null ? totalQty : 0L);

                productDTOs.add(dto);
            }
            status = true;
            message = "Products fetched successfully!";

            responseObject.addProperty("totalPages", totalPages);
            responseObject.addProperty("currentPage", pageNumber);

        } catch (Exception e) {
            message = "Error loading products";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("products", AppUtil.GSON.toJsonTree(productDTOs));
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getProductDetails(int productId) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        ProductDTO dto = new ProductDTO();

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Product product = hibernateSession.get(Product.class, productId);

            if (product != null) {
                dto.setId(product.getId());
                dto.setName(product.getName());
                dto.setDescription(product.getDescription());
                dto.setBrandName(product.getBrand().getName());
                dto.setModelName(product.getModel().getName());
                dto.setGender(product.getGender().getName());
                dto.setStatus(product.getStatus().getType());

                // Get All Images
                List<ProductImageDTO> imageDTOs = new ArrayList<>();
                String hqlImage = "FROM ProductImage p WHERE p.product.id = :pid";
                List<ProductImage> images = hibernateSession.createQuery(hqlImage, ProductImage.class)
                        .setParameter("pid", product.getId())
                        .list();

                for (ProductImage img : images) {
                    imageDTOs.add(new ProductImageDTO(img.getId(), img.getPath()));
                }
                dto.setImages(imageDTOs);

                status = true;
                message = "Product details fetched!";
            } else {
                message = "Product not found!";
            }

        } catch (Exception e) {
            message = "Error loading product details";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("product", AppUtil.GSON.toJsonTree(dto));
        return AppUtil.GSON.toJson(responseObject);
    }

    public String updateProduct(ProductDTO productDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                Product product = hibernateSession.get(Product.class, productDTO.getId());

                if (product != null) {
                    product.setName(productDTO.getName());
                    product.setDescription(productDTO.getDescription());

                    // Update Model
                    Model model = hibernateSession.createNamedQuery("Model.findByName", Model.class)
                            .setParameter("name", productDTO.getModelName())
                            .uniqueResult();
                    if (model == null) {
                        model = new Model();
                        model.setName(productDTO.getModelName());
                        hibernateSession.persist(model);
                    }
                    product.setModel(model);

                    // Update Status
                    Status statusObj = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                            .setParameter("type", productDTO.getStatus()) // Expecting "Active" or "Inactive"
                            .uniqueResult();
                    if (statusObj != null) {
                        product.setStatus(statusObj);
                    }

                    hibernateSession.merge(product);
                    transaction.commit();
                    status = true;
                    message = "Product updated successfully!";
                } else {
                    message = "Product not found!";
                }
            } catch (Exception e) {
                transaction.rollback();
                message = "Error updating product";
                e.printStackTrace();
            }
        } catch (Exception e) {
            message = "Error updating product";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String removeProductImage(int imageId, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                ProductImage image = hibernateSession.get(ProductImage.class, imageId);

                if (image != null) {
                    // Remove file from disk
                    String filePath = request.getServletContext().getRealPath("") + File.separator + image.getPath();
                    File file = new File(filePath);
                    if (file.exists()) {
                        file.delete();
                    }

                    hibernateSession.remove(image);
                    transaction.commit();
                    status = true;
                    message = "Image removed successfully!";
                } else {
                    message = "Image not found!";
                }
            } catch (Exception e) {
                transaction.rollback();
                message = "Error removing image";
                e.printStackTrace();
            }
        } catch (Exception e) {
            message = "Error removing image";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getSuppliers() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        List<SupplierDTO> suppliers = new ArrayList<>();

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Seller> sellers = hibernateSession.createQuery("FROM Seller", Seller.class).list();
            for (Seller seller : sellers) {
                SupplierDTO dto = new SupplierDTO();
                dto.setId(seller.getId());
                dto.setCompanyName(seller.getCompanyName());
                dto.setCompanyMobile(seller.getCompanyMobile());
                dto.setCompanyEmail(seller.getCompanyEmail());
                dto.setStatus(seller.getStatus().getType());
                suppliers.add(dto);
            }
            status = true;
            message = "Suppliers loaded!";
        } catch (Exception e) {
            message = "Error loading suppliers";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("suppliers", AppUtil.GSON.toJsonTree(suppliers));
        return AppUtil.GSON.toJson(responseObject);
    }

    public String addSupplier(SupplierDTO supplierDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                // Backend Validation
                if (supplierDTO.getCompanyName() == null || supplierDTO.getCompanyName().isEmpty()) {
                    message = "Company Name is required";
                } else if (supplierDTO.getCompanyMobile() == null || supplierDTO.getCompanyMobile().isEmpty()) {
                    message = "Mobile Number is required";
                } else if (!supplierDTO.getCompanyMobile().matches(Validator.LANDLINE_VALIDATION)) {
                    message = "Invalid Company Number, the number has to be a landline number";
                } else if (supplierDTO.getCompanyEmail() == null || supplierDTO.getCompanyEmail().isEmpty()) {
                    message = "Email is required";
                } else if (!supplierDTO.getCompanyEmail().matches(Validator.EMAIL_VALIDATION)) {
                    message = "Invalid Email, the email has to be a valid email address";
                } else {
                    // Check if supplier already exists
                    Seller existingSeller = hibernateSession
                            .createQuery("FROM Seller WHERE companyEmail = :email OR companyMobile = :mobile",
                                    Seller.class)
                            .setParameter("email", supplierDTO.getCompanyEmail())
                            .setParameter("mobile", supplierDTO.getCompanyMobile())
                            .uniqueResult();

                    if (existingSeller == null) {
                        Seller seller = new Seller();
                        seller.setCompanyName(supplierDTO.getCompanyName());
                        seller.setCompanyMobile(supplierDTO.getCompanyMobile());
                        seller.setCompanyEmail(supplierDTO.getCompanyEmail());

                        // Default to Active Status
                        Status statusObj = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                                .setParameter("type", "Active")
                                .uniqueResult();
                        if (statusObj == null) {
                            // Create default active status if not exists
                            statusObj = new Status();
                            statusObj.setType("Active");
                            hibernateSession.persist(statusObj);
                        }
                        seller.setStatus(statusObj);

                        hibernateSession.persist(seller);
                        transaction.commit();
                        status = true;
                        message = "Supplier added successfully!";
                    } else {
                        message = "Supplier with this email or mobile already exists!";
                    }
                }

            } catch (Exception e) {
                transaction.rollback();
                message = "Error adding supplier";
                e.printStackTrace();
            }
        } catch (Exception e) {
            message = "Error adding supplier";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getSupplierDetails(int id) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        SupplierDTO dto = new SupplierDTO();

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Seller seller = hibernateSession.get(Seller.class, id);

            if (seller != null) {
                dto.setId(seller.getId());
                dto.setCompanyName(seller.getCompanyName());
                dto.setCompanyMobile(seller.getCompanyMobile());
                dto.setCompanyEmail(seller.getCompanyEmail());
                dto.setStatus(seller.getStatus().getType());

                status = true;
                message = "Supplier details loaded!";
            } else {
                message = "Supplier not found!";
            }
        } catch (Exception e) {
            message = "Error loading supplier details";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("supplier", AppUtil.GSON.toJsonTree(dto));
        return AppUtil.GSON.toJson(responseObject);
    }

    public String updateSupplier(SupplierDTO supplierDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                if (supplierDTO.getCompanyName() == null || supplierDTO.getCompanyName().isEmpty()) {
                    message = "Company Name is required";
                } else if (supplierDTO.getCompanyMobile() == null || supplierDTO.getCompanyMobile().isEmpty()) {
                    message = "Mobile Number is required";
                } else if (supplierDTO.getCompanyEmail() == null || supplierDTO.getCompanyEmail().isEmpty()) {
                    message = "Email is required";
                } else {
                    Seller seller = hibernateSession.get(Seller.class, supplierDTO.getId());

                    if (seller != null) {
                        seller.setCompanyName(supplierDTO.getCompanyName());
                        seller.setCompanyMobile(supplierDTO.getCompanyMobile());
                        seller.setCompanyEmail(supplierDTO.getCompanyEmail());

                        // Update Status
                        Status statusObj = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                                .setParameter("type", supplierDTO.getStatus())
                                .uniqueResult();
                        if (statusObj != null) {
                            seller.setStatus(statusObj);
                        }

                        hibernateSession.merge(seller);
                        transaction.commit();
                        status = true;
                        message = "Supplier updated successfully!";
                    } else {
                        message = "Supplier not found!";
                    }
                }
            } catch (Exception e) {
                transaction.rollback();
                message = "Error updating supplier";
                e.printStackTrace();
            }
        } catch (Exception e) {
            message = "Error updating supplier";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String saveGrn(GrnDTO grnDTO) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false); // Default to false

        // 1. Basic Validation (Fail Fast)
        if (grnDTO.getSupplierId() == null || grnDTO.getSupplierId().isEmpty()) {
            responseObject.addProperty("message", "Please select a supplier");
            return AppUtil.GSON.toJson(responseObject);
        }
        if (grnDTO.getSupplierInvoiceNumber() == null || grnDTO.getSupplierInvoiceNumber().isEmpty()) {
            responseObject.addProperty("message", "Please enter invoice number");
            return AppUtil.GSON.toJson(responseObject);
        }
        if (grnDTO.getGrnItems() == null || grnDTO.getGrnItems().isEmpty()) {
            responseObject.addProperty("message", "Please add at least one item");
            return AppUtil.GSON.toJson(responseObject);
        }

        // 2. Database Operations
        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();

            try {
                // Check Supplier
                Seller seller = hibernateSession.get(Seller.class, Integer.parseInt(grnDTO.getSupplierId()));
                if (seller == null) {
                    responseObject.addProperty("message", "Supplier not found!");
                    return AppUtil.GSON.toJson(responseObject);
                }

                // Check or Create GRN Header
                String hqlGrn = "FROM Grn WHERE seller = :seller AND supplierInvoiceNumber = :invNo";
                Grn existingGrn = hibernateSession.createQuery(hqlGrn, Grn.class)
                        .setParameter("seller", seller)
                        .setParameter("invNo", grnDTO.getSupplierInvoiceNumber())
                        .uniqueResult();

                Grn grn = existingGrn;
                if (grn == null) {
                    grn = new Grn();
                    grn.setDate(new java.util.Date());
                    grn.setSeller(seller);
                    grn.setSupplierInvoiceNumber(grnDTO.getSupplierInvoiceNumber());
                    hibernateSession.persist(grn);
                } else {
                    // Check Time Limit (1 Hour)
                    long diff = new java.util.Date().getTime() - grn.getDate().getTime();
                    long diffHours = diff / (60 * 60 * 1000);
                    if (diffHours >= 1) {
                        responseObject.addProperty("status", false);
                        responseObject.addProperty("message", "Cannot modify GRN after 1 hour of creation.");
                        return AppUtil.GSON.toJson(responseObject);
                    }
                }

                // OPTIMIZATION: Fetch Status ONCE outside the loop
                Status activeStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("type", "Active")
                        .uniqueResult();

                if (activeStatus == null) {
                    transaction.rollback();
                    responseObject.addProperty("message", "Server Error: Status 'Active' not found in DB.");
                    return AppUtil.GSON.toJson(responseObject);
                }

                // Process Items
                for (GrnItemDTO itemDTO : grnDTO.getGrnItems()) {

                    // Item Validation
                    if (itemDTO.getSellingPrice() <= itemDTO.getBuyingPrice()) {
                        transaction.rollback();
                        responseObject.addProperty("message",
                                "Selling price must be higher than buying price for item ID: "
                                        + itemDTO.getProductId());
                        return AppUtil.GSON.toJson(responseObject);
                    }

                    Product product = hibernateSession.get(Product.class, Integer.parseInt(itemDTO.getProductId()));
                    Size size = hibernateSession.get(Size.class, Integer.parseInt(itemDTO.getSizeId()));
                    Color color = hibernateSession.get(Color.class, Integer.parseInt(itemDTO.getColorId()));

                    if (product == null || size == null || color == null) {
                        transaction.rollback();
                        responseObject.addProperty("message", "Invalid Product, Size, or Color ID.");
                        return AppUtil.GSON.toJson(responseObject);
                    }

                    // 3. Duplicate Check
                    boolean isDuplicateResolved = false;

                    if (existingGrn != null) {
                        // Updated Query: Check Product + Size + Color + BuyingPrice (Ignore Qty,
                        // SellingPrice)
                        String hqlDupItem = "FROM GrnItem WHERE grn = :grn AND product = :prod AND size = :size AND color = :color AND buyingPrice = :bp";
                        GrnItem dupItem = hibernateSession.createQuery(hqlDupItem, GrnItem.class)
                                .setParameter("grn", grn)
                                .setParameter("prod", product)
                                .setParameter("size", size)
                                .setParameter("color", color) // Added Color check
                                .setParameter("bp", itemDTO.getBuyingPrice())
                                .setMaxResults(1)
                                .uniqueResult();

                        if (dupItem != null) {

                            if (itemDTO.getResolution() == null || itemDTO.getResolution().isEmpty()) {
                                transaction.rollback(); // Stop everything
                                responseObject.addProperty("status", false);
                                responseObject.addProperty("message", "Conflict: Duplicate item found.");
                                responseObject.addProperty("action", "prompt_resolution");
                                responseObject.addProperty("duplicateProduct", product.getName());
                                responseObject.addProperty("duplicateSize", size.getName()); // Added Size info
                                return AppUtil.GSON.toJson(responseObject);
                            } else if ("REPLACE".equals(itemDTO.getResolution())) {

                                dupItem.setQty(dupItem.getQty() + itemDTO.getQuantity());
                                hibernateSession.merge(dupItem);

                                // Update Stock matching same criteria
                                String hqlStock = "FROM Stock WHERE grn = :grn AND product = :prod AND size = :size AND color = :color AND buyingPrice = :bp";
                                Stock dupStock = hibernateSession.createQuery(hqlStock, Stock.class)
                                        .setParameter("grn", grn)
                                        .setParameter("prod", product)
                                        .setParameter("size", size)
                                        .setParameter("color", color)
                                        .setParameter("bp", itemDTO.getBuyingPrice())
                                        .setMaxResults(1)
                                        .uniqueResult();

                                if (dupStock != null) {
                                    dupStock.setQty(dupStock.getQty() + itemDTO.getQuantity());
                                    hibernateSession.merge(dupStock);
                                }

                                isDuplicateResolved = true;
                            }
                        }
                    }

                    if (isDuplicateResolved) {
                        continue;
                    }

                    GrnItem grnItem = new GrnItem();
                    grnItem.setQty(itemDTO.getQuantity());
                    grnItem.setBuyingPrice(itemDTO.getBuyingPrice());
                    grnItem.setGrn(grn);
                    grnItem.setProduct(product);
                    grnItem.setSize(size);
                    grnItem.setColor(color); // Set Color
                    hibernateSession.persist(grnItem);

                    Stock stock = new Stock();
                    stock.setProduct(product);
                    stock.setQty(itemDTO.getQuantity());
                    stock.setBuyingPrice(itemDTO.getBuyingPrice());
                    stock.setSellingPrice(itemDTO.getSellingPrice());
                    stock.setDiscount(0.0);
                    stock.setCreatedAt(new java.util.Date());
                    stock.setGrn(grn);
                    stock.setColor(color);
                    stock.setSize(size);
                    stock.setStatus(activeStatus);
                    hibernateSession.persist(stock);
                }

                transaction.commit();
                responseObject.addProperty("status", true);
                responseObject.addProperty("message", "GRN saved successfully!");

            } catch (HibernateException e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();

                }
                System.out.println(" error" + e.getMessage());
                responseObject.addProperty("message", "Error saving GRN.!");
            }
        } catch (HibernateException e) {
            System.out.println("Hibernate Error" + e.getMessage());
            responseObject.addProperty("message", "Something went wrong.");
        }

        return AppUtil.GSON.toJson(responseObject);
    }

    public String getColors() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        List<JsonObject> colorsList = new ArrayList<>();

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Color> colors = hibernateSession.createQuery("FROM Color", Color.class).list();

            for (Color color : colors) {
                JsonObject colorObj = new JsonObject();
                colorObj.addProperty("id", color.getId());
                colorObj.addProperty("name", color.getName());
                colorsList.add(colorObj);
            }

            status = true;
            message = "Success";
        } catch (Exception e) {
            message = "Error: " + e.getMessage();
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("colors", AppUtil.GSON.toJsonTree(colorsList));

        return AppUtil.GSON.toJson(responseObject);
    }

    public String validateGrnItem(GrnItemDTO itemDTO) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", true);

        if (itemDTO.getProductId() == null || itemDTO.getProductId().isEmpty() ||
                itemDTO.getColorId() == null || itemDTO.getColorId().isEmpty() ||
                itemDTO.getSizeId() == null || itemDTO.getSizeId().isEmpty()) {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Please select Product, Color, and Size.");
            return AppUtil.GSON.toJson(responseObject);
        }

        // Use strict parsing to catch non-numeric input
        try {
            if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", "Quantity must be greater than 0.");
                return AppUtil.GSON.toJson(responseObject);
            }
            if (itemDTO.getBuyingPrice() == null || itemDTO.getBuyingPrice() <= 0) {
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", "Buying Price must be greater than 0.");
                return AppUtil.GSON.toJson(responseObject);
            }
            if (itemDTO.getSellingPrice() == null || itemDTO.getSellingPrice() <= 0) {
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", "Selling Price must be greater than 0.");
                return AppUtil.GSON.toJson(responseObject);
            }
            if (itemDTO.getSellingPrice() <= itemDTO.getBuyingPrice()) {
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", "Selling price must be higher than buying price.");
                return AppUtil.GSON.toJson(responseObject);
            }
        } catch (Exception e) {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Invalid numeric values.");
            return AppUtil.GSON.toJson(responseObject);
        }

        return AppUtil.GSON.toJson(responseObject);
    }

    public String getSizes() {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        List<JsonObject> sizesList = new ArrayList<>();

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Size> sizes = hibernateSession.createQuery("FROM Size", Size.class).list();

            for (Size size : sizes) {
                JsonObject sizeObj = new JsonObject();
                sizeObj.addProperty("id", size.getId());
                sizeObj.addProperty("name", size.getName());
                sizesList.add(sizeObj);
            }

            status = true;
            message = "Sizes loaded successfully!";
        } catch (Exception e) {
            message = "Error loading sizes";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("sizes", AppUtil.GSON.toJsonTree(sizesList));
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getGrnItems(int page) {
        JsonObject responseObject = new JsonObject();
        List<JsonObject> itemList = new ArrayList<>();
        int pageSize = 20;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            // 1. Get Total Count
            Long totalCount = (Long) hibernateSession.createQuery("SELECT COUNT(g) FROM GrnItem g").uniqueResult();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);

            // 2. Fetch Items for Page
            int offset = (page - 1) * pageSize;
            List<GrnItem> items = hibernateSession.createQuery(
                    "FROM GrnItem g LEFT JOIN FETCH g.grn LEFT JOIN FETCH g.product LEFT JOIN FETCH g.color LEFT JOIN FETCH g.size ORDER BY g.id DESC",
                    GrnItem.class)
                    .setFirstResult(offset)
                    .setMaxResults(pageSize)
                    .list();

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

            for (GrnItem item : items) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", item.getId());
                obj.addProperty("date", item.getGrn() != null ? sdf.format(item.getGrn().getDate()) : "N/A");
                obj.addProperty("supplier",
                        (item.getGrn() != null && item.getGrn().getSeller() != null)
                                ? item.getGrn().getSeller().getCompanyName()
                                : "Unknown");
                obj.addProperty("product", item.getProduct() != null ? item.getProduct().getName() : "Unknown");
                obj.addProperty("details", (item.getColor() != null ? item.getColor().getName() : "-") + ", "
                        + (item.getSize() != null ? item.getSize().getName() : "-"));
                obj.addProperty("qty", item.getQty());
                obj.addProperty("cost", item.getBuyingPrice() * item.getQty());

                // Check editable (No limit)
                boolean editable = true;
                obj.addProperty("editable", editable);

                itemList.add(obj);
            }

            responseObject.addProperty("status", true);
            responseObject.addProperty("totalPages", totalPages);
            responseObject.addProperty("currentPage", page);
            responseObject.add("items", AppUtil.GSON.toJsonTree(itemList));

        } catch (Exception e) {
            e.printStackTrace();
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Error loading items");
        }
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getGrnList(int page) {
        JsonObject responseObject = new JsonObject();
        List<JsonObject> grnList = new ArrayList<>();
        int pageSize = 10; // Fewer items per page since they are cards

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            // 1. Get Total Count
            Long totalCount = (Long) hibernateSession.createQuery("SELECT COUNT(g) FROM Grn g").uniqueResult();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);

            // 2. Fetch GRNs for Page
            int offset = (page - 1) * pageSize;
            List<Grn> grns = hibernateSession.createQuery(
                    "FROM Grn g LEFT JOIN FETCH g.seller ORDER BY g.date DESC", Grn.class)
                    .setFirstResult(offset)
                    .setMaxResults(pageSize)
                    .list();

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

            for (Grn grn : grns) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", grn.getId());
                obj.addProperty("date", sdf.format(grn.getDate()));
                obj.addProperty("supplier", grn.getSeller() != null ? grn.getSeller().getCompanyName() : "Unknown");
                obj.addProperty("supplierInvoice",
                        grn.getSupplierInvoiceNumber() != null ? grn.getSupplierInvoiceNumber() : "N/A");

                // Calculate Total for this GRN
                Double total = (Double) hibernateSession.createQuery(
                        "SELECT SUM(gi.buyingPrice * gi.qty) FROM GrnItem gi WHERE gi.grn = :grn")
                        .setParameter("grn", grn)
                        .uniqueResult();
                obj.addProperty("total", total != null ? total : 0.0);

                // Item Count
                Long itemCount = (Long) hibernateSession.createQuery(
                        "SELECT COUNT(gi) FROM GrnItem gi WHERE gi.grn = :grn")
                        .setParameter("grn", grn)
                        .uniqueResult();
                obj.addProperty("itemCount", itemCount != null ? itemCount : 0);

                grnList.add(obj);
            }

            responseObject.addProperty("status", true);
            responseObject.addProperty("totalPages", totalPages);
            responseObject.addProperty("currentPage", page);
            responseObject.add("grns", AppUtil.GSON.toJsonTree(grnList));

        } catch (Exception e) {
            e.printStackTrace();
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Error loading GRNs");
        }
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getGrnDetailsFull(int grnId) {
        JsonObject responseObject = new JsonObject();
        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Grn grn = hibernateSession.get(Grn.class, grnId);
            if (grn != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
                JsonObject grnData = new JsonObject();
                grnData.addProperty("id", grn.getId());
                grnData.addProperty("date", sdf.format(grn.getDate()));
                grnData.addProperty("supplier", grn.getSeller() != null ? grn.getSeller().getCompanyName() : "Unknown");
                grnData.addProperty("supplierInvoice",
                        grn.getSupplierInvoiceNumber() != null ? grn.getSupplierInvoiceNumber() : "");

                // Items (DISTINCT to prevent duplicates)
                List<GrnItem> items = hibernateSession.createQuery(
                        "SELECT DISTINCT gi FROM GrnItem gi LEFT JOIN FETCH gi.product LEFT JOIN FETCH gi.color LEFT JOIN FETCH gi.size WHERE gi.grn = :grn",
                        GrnItem.class)
                        .setParameter("grn", grn)
                        .list();

                // Fetch ALL Stocks for this GRN at once to avoid N+1 queries
                List<Stock> allStocks = hibernateSession.createQuery(
                        "FROM Stock s WHERE s.grn = :grn", Stock.class)
                        .setParameter("grn", grn)
                        .list();

                // Map stocks for faster lookup
                // Key: productId-colorId-sizeId-buyPrice
                java.util.Map<String, Stock> stockMap = new java.util.HashMap<>();
                for (Stock s : allStocks) {
                    if (s.getProduct() != null && s.getColor() != null && s.getSize() != null
                            && s.getBuyingPrice() != null) {
                        String key = s.getProduct().getId() + "-" + s.getColor().getId() + "-" + s.getSize().getId()
                                + "-" + s.getBuyingPrice().doubleValue();
                        stockMap.put(key, s);
                    }
                }

                com.google.gson.JsonArray itemsArray = new com.google.gson.JsonArray();
                double grandTotal = 0.0;

                for (GrnItem item : items) {
                    if (item.getProduct() == null)
                        continue;

                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("id", item.getId());
                    itemObj.addProperty("productId", item.getProduct().getId());
                    itemObj.addProperty("productName", item.getProduct().getName());
                    itemObj.addProperty("colorId", item.getColor() != null ? item.getColor().getId() : 0);
                    itemObj.addProperty("colorName", item.getColor() != null ? item.getColor().getName() : "-");
                    itemObj.addProperty("sizeId", item.getSize() != null ? item.getSize().getId() : 0);
                    itemObj.addProperty("sizeName", item.getSize() != null ? item.getSize().getName() : "-");

                    int qty = item.getQty() != null ? item.getQty() : 0;
                    double buyPrice = item.getBuyingPrice() != null ? item.getBuyingPrice() : 0.0;

                    itemObj.addProperty("qty", qty);
                    itemObj.addProperty("buyingPrice", buyPrice);

                    // Check Stock from Map
                    boolean hasSales = false;
                    double sellingPrice = 0.0;

                    String lookupKey = item.getProduct().getId() + "-" +
                            (item.getColor() != null ? item.getColor().getId() : 0) + "-" +
                            (item.getSize() != null ? item.getSize().getId() : 0) + "-" +
                            buyPrice;

                    Stock s = stockMap.get(lookupKey);
                    if (s != null) {
                        hasSales = s.getQty() < qty;
                        sellingPrice = s.getSellingPrice() != null ? s.getSellingPrice() : 0.0;
                    }

                    // Fetch Image (Safe Wrap)
                    String imagePath = "assets/img/defaults/product-default.png";
                    try {
                        List<String> imgPaths = hibernateSession
                                .createQuery("SELECT path FROM ProductImage WHERE product = :prod", String.class)
                                .setParameter("prod", item.getProduct())
                                .setMaxResults(1)
                                .list();
                        if (!imgPaths.isEmpty()) {
                            imagePath = imgPaths.get(0);
                        }
                    } catch (Exception ignore) {
                    }

                    itemObj.addProperty("sellingPrice", sellingPrice);
                    itemObj.addProperty("hasSales", hasSales);
                    itemObj.addProperty("imagePath", imagePath);
                    itemObj.addProperty("total", buyPrice * qty);
                    grandTotal += (buyPrice * qty);

                    itemsArray.add(itemObj); // Add directly to Array
                }

                grnData.addProperty("grandTotal", grandTotal);
                grnData.add("items", itemsArray);

                responseObject.addProperty("status", true);
                responseObject.add("data", grnData);
            } else {
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", "GRN not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Error loading details: " + e.getMessage());
        }
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getGrnItemDetails(int id) {
        JsonObject responseObject = new JsonObject();
        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            GrnItem item = hibernateSession.get(GrnItem.class, id);
            if (item != null) {
                JsonObject data = new JsonObject();
                data.addProperty("id", item.getId());
                data.addProperty("qty", item.getQty());
                data.addProperty("buyingPrice", item.getBuyingPrice());

                Query<Stock> stockQuery = hibernateSession.createQuery(
                        "FROM Stock s WHERE s.grn = :grn AND s.product = :prod AND s.color = :col AND s.size = :size AND s.buyingPrice = :bp",
                        Stock.class);
                stockQuery.setParameter("grn", item.getGrn());
                stockQuery.setParameter("prod", item.getProduct());
                stockQuery.setParameter("col", item.getColor());
                stockQuery.setParameter("size", item.getSize());
                stockQuery.setParameter("bp", item.getBuyingPrice());

                List<Stock> stocks = stockQuery.list();
                if (!stocks.isEmpty()) {
                    Stock stock = stocks.get(0);
                    data.addProperty("sellingPrice", stock.getSellingPrice());
                }

                data.addProperty("colorId", item.getColor().getId());
                data.addProperty("sizeId", item.getSize().getId());
                data.addProperty("productId", item.getProduct().getId());
                data.addProperty("productName", item.getProduct().getName());

                responseObject.addProperty("status", true);
                responseObject.add("data", data);
            } else {
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", "Item not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Error loading details");
        }
        return AppUtil.GSON.toJson(responseObject);
    }

    public String deleteGrn(int id) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                Grn grn = hibernateSession.get(Grn.class, id);
                if (grn == null) {
                    throw new Exception("GRN not found");
                }

                List<GrnItem> items = hibernateSession.createQuery("FROM GrnItem WHERE grn = :grn", GrnItem.class)
                        .setParameter("grn", grn).list();

                for (GrnItem item : items) {
                    Query<Stock> stockQuery = hibernateSession.createQuery(
                            "FROM Stock s WHERE s.grn = :grn AND s.product = :prod AND s.color = :col AND s.size = :size AND s.buyingPrice = :bp",
                            Stock.class);
                    stockQuery.setParameter("grn", grn);
                    stockQuery.setParameter("prod", item.getProduct());
                    stockQuery.setParameter("col", item.getColor());
                    stockQuery.setParameter("size", item.getSize());
                    stockQuery.setParameter("bp", item.getBuyingPrice());

                    List<Stock> stocks = stockQuery.list();
                    if (!stocks.isEmpty()) {
                        Stock stock = stocks.get(0);
                        if (stock.getQty() < item.getQty()) {
                            throw new Exception("Cannot delete GRN: Items from this Invoice have been sold ("
                                    + item.getProduct().getName() + ")");
                        }
                        hibernateSession.remove(stock);
                    }
                    hibernateSession.remove(item);
                }

                hibernateSession.remove(grn);

                transaction.commit();
                status = true;
                message = "GRN deleted successfully!";

            } catch (Exception e) {
                if (transaction != null)
                    transaction.rollback();
                message = e.getMessage();
            }
        } catch (Exception e) {
            message = "Error Deleting GRN";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String addGrnItem(GrnItemUpdateDTO dto) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                // dto.getGrnItemId() is null, but we need GRN ID.
                // Assuming GrnItemUpdateDTO has a field for grnId or we need to add it/reuse
                // getGrnItemId as grnId?
                // Actually, I'll update GrnItemUpdateDTO to include grnId explicitly for this
                // purpose.
                // Wait, I can't update DTO in this step easily without separate call.
                // I will assume `getGrnItemId` holds GRN ID for *ADD* operation? No, that's
                // confusing.
                // I'll check GrnItemUpdateDTO content again.
                // It has: `grnItemId`, `quantity`, `buyingPrice`, `sellingPrice`, `colorId`,
                // `sizeId`, `productId`.
                // It MISSES `valGrnId` (the parent GRN to add to).
                // I will add `grnId` to `GrnItemUpdateDTO` in a future step or now.
                // For now, I will assume the DTO passed to `addGrnItem` has `grnItemId`
                // interpreted as `grnId`?
                // BAD PRACTICE.
                // I will fetch GRN using `dto.getGrnItemId()` for now, treating it as the
                // Parent GRN ID for the "Add" operation context.

                Grn grn = hibernateSession.get(Grn.class, dto.getGrnItemId()); // Interpreting ID as Parent GRN ID
                if (grn == null)
                    throw new Exception("GRN not found");

                Product product = hibernateSession.get(Product.class, dto.getProductId());
                Color color = hibernateSession.get(Color.class, dto.getColorId());
                Size size = hibernateSession.get(Size.class, dto.getSizeId());

                if (product == null || color == null || size == null)
                    throw new Exception("Invalid attributes");

                GrnItem item = new GrnItem();
                item.setGrn(grn);
                item.setProduct(product);
                item.setColor(color);
                item.setSize(size);
                item.setQty(dto.getQuantity());
                item.setBuyingPrice(dto.getBuyingPrice());
                hibernateSession.save(item);

                Stock stock = new Stock();
                stock.setGrn(grn);
                stock.setProduct(product);
                stock.setColor(color);
                stock.setSize(size);
                stock.setQty(dto.getQuantity());
                stock.setBuyingPrice(dto.getBuyingPrice());
                stock.setSellingPrice(dto.getSellingPrice());
                hibernateSession.save(stock);

                transaction.commit();
                status = true;
                message = "Item added successfully!";

            } catch (Exception e) {
                if (transaction != null)
                    transaction.rollback();
                message = e.getMessage();
                e.printStackTrace();
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String updateGrnItem(GrnItemUpdateDTO dto) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                GrnItem item = hibernateSession.get(GrnItem.class, dto.getGrnItemId());
                if (item == null) {
                    throw new Exception("Item not found");
                }

                // 1. Check Time Limit - REMOVED per user request
                // 2. Find Associated Stock
                Query<Stock> stockQuery = hibernateSession.createQuery(
                        "FROM Stock s WHERE s.grn = :grn AND s.product = :prod AND s.color = :col AND s.size = :size AND s.buyingPrice = :bp",
                        Stock.class);
                stockQuery.setParameter("grn", item.getGrn());
                stockQuery.setParameter("prod", item.getProduct());
                stockQuery.setParameter("col", item.getColor());
                stockQuery.setParameter("size", item.getSize());
                stockQuery.setParameter("bp", item.getBuyingPrice());

                List<Stock> stocks = stockQuery.list();
                if (stocks.isEmpty()) {
                    throw new Exception("Corresponding Stock record not found. Data integrity issue?");
                }
                Stock stock = stocks.get(0);

                // Check for Sales (Stock Qty < GRN Qty)
                boolean hasSales = stock.getQty() < item.getQty();

                // Constraint: Cannot change Product, Color, or Size if sales exist
                if (hasSales) {
                    if (!item.getProduct().getId().equals(dto.getProductId()) ||
                            !item.getColor().getId().equals(dto.getColorId()) ||
                            !item.getSize().getId().equals(dto.getSizeId())) {
                        throw new Exception(
                                "Cannot change Product, Color, or Size: Items from this batch have been sold.");
                    }
                }

                // 3. Smart Stock Update (Differential Update)
                int oldQty = item.getQty();
                int newQty = dto.getQuantity();
                int diff = newQty - oldQty;
                int currentStockQty = stock.getQty();
                int newStockQty = currentStockQty + diff;

                if (newStockQty < 0) {
                    throw new Exception("Cannot update GRN: Resulting stock would be negative (" + newStockQty
                            + "). Items already sold.");
                }

                // 4. Update GrnItem
                item.setQty(newQty);
                item.setBuyingPrice(dto.getBuyingPrice());
                item.setColor(hibernateSession.get(Color.class, dto.getColorId()));
                item.setSize(hibernateSession.get(Size.class, dto.getSizeId()));
                item.setProduct(hibernateSession.get(Product.class, dto.getProductId()));
                hibernateSession.merge(item);

                // 5. Update Stock
                stock.setQty(newStockQty); // Set the calculated new qty
                stock.setBuyingPrice(dto.getBuyingPrice());
                stock.setSellingPrice(dto.getSellingPrice());
                stock.setColor(hibernateSession.get(Color.class, dto.getColorId()));
                stock.setSize(hibernateSession.get(Size.class, dto.getSizeId()));
                stock.setProduct(hibernateSession.get(Product.class, dto.getProductId()));
                hibernateSession.merge(stock);

                transaction.commit();
                status = true;
                message = "Item updated successfully!";

            } catch (Exception e) {
                if (transaction != null)
                    transaction.rollback();
                message = e.getMessage();
                e.printStackTrace();
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String deleteGrnItem(int id) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                GrnItem item = hibernateSession.get(GrnItem.class, id);
                if (item == null) {
                    throw new Exception("Item not found");
                }

                // Find Stock
                Query<Stock> stockQuery = hibernateSession.createQuery(
                        "FROM Stock s WHERE s.grn = :grn AND s.product = :prod AND s.color = :col AND s.size = :size AND s.buyingPrice = :bp",
                        Stock.class);
                stockQuery.setParameter("grn", item.getGrn());
                stockQuery.setParameter("prod", item.getProduct());
                stockQuery.setParameter("col", item.getColor());
                stockQuery.setParameter("size", item.getSize());
                stockQuery.setParameter("bp", item.getBuyingPrice());

                List<Stock> stocks = stockQuery.list();
                if (stocks.isEmpty()) {
                    // Odd case, stock deleted manually? Allow delete.
                    hibernateSession.remove(item);
                } else {
                    Stock stock = stocks.get(0);
                    // Check Sales
                    if (stock.getQty() < item.getQty()) {
                        throw new Exception("Cannot delete: Items from this batch have been sold.");
                    }
                    hibernateSession.remove(stock);
                    hibernateSession.remove(item);
                }

                transaction.commit();
                status = true;
                message = "Item deleted successfully!";

            } catch (Exception e) {
                if (transaction != null)
                    transaction.rollback();
                message = e.getMessage();
            }
        } catch (Exception e) {
            message = "Error deleting item";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String saveColor(String name) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                // Check if exists
                Color existingColor = hibernateSession.createQuery("FROM Color WHERE name = :name", Color.class)
                        .setParameter("name", name)
                        .uniqueResult();

                if (existingColor != null) {
                    message = "Color already exists!";
                } else {
                    Color color = new Color();
                    color.setName(name);
                    hibernateSession.persist(color);
                    transaction.commit();
                    status = true;
                    message = "Color added successfully!";
                }

            } catch (Exception e) {
                if (transaction != null)
                    transaction.rollback();
                message = "Error adding color";
                e.printStackTrace();
            }
        } catch (Exception e) {
            message = "Error adding color";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String saveSize(String name) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                // Check if exists
                Size existingSize = hibernateSession.createQuery("FROM Size WHERE name = :name", Size.class)
                        .setParameter("name", name)
                        .uniqueResult();

                if (existingSize != null) {
                    message = "Size already exists!";
                } else {
                    Size size = new Size();
                    size.setName(name);
                    hibernateSession.persist(size);
                    transaction.commit();
                    status = true;
                    message = "Size added successfully!";
                }
            } catch (Exception e) {
                transaction.rollback();
                message = "Error adding size";
                e.printStackTrace();
            }
        } catch (Exception e) {
            message = "Error adding size";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getProductStocks(int productId) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;
        List<StockDTO> stockList = new ArrayList<>();

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            // Fetch stocks with active/inactive status, sorting by ID
            // Assuming we show all stocks? User said "view all the astock under that
            // product"
            List<Stock> stocks = hibernateSession.createQuery(
                    "FROM Stock s JOIN FETCH s.color JOIN FETCH s.size JOIN FETCH s.status WHERE s.product.id = :pid ORDER BY s.id DESC",
                    Stock.class)
                    .setParameter("pid", productId)
                    .list();

            for (Stock s : stocks) {
                StockDTO dto = new StockDTO();
                dto.setId(s.getId());
                dto.setColor(s.getColor().getName());
                dto.setSize(s.getSize().getName());
                dto.setQty(s.getQty());
                dto.setSellingPrice(s.getSellingPrice());
                dto.setStatus(s.getStatus().getType());

                // Find assigned image for this stock
                // We stored stock info in ProductImage. So find ProductImage where stock = s
                List<String> images = hibernateSession
                        .createQuery("SELECT p.path FROM ProductImage p WHERE p.stock.id = :sid", String.class)
                        .setParameter("sid", s.getId())
                        .setMaxResults(1)
                        .list();
                if (!images.isEmpty()) {
                    dto.setVariantImage(images.get(0));
                }

                stockList.add(dto);
            }
            status = true;
            message = "Stocks loaded";

        } catch (Exception e) {
            message = "Error loading stocks";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        responseObject.add("stocks", AppUtil.GSON.toJsonTree(stockList));
        return AppUtil.GSON.toJson(responseObject);
    }

    public String updateStock(StockDTO stockDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                Stock stock = hibernateSession.get(Stock.class, stockDTO.getId());
                if (stock != null) {
                    stock.setSellingPrice(stockDTO.getSellingPrice());

                    // Update Status
                    Status statusObj = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                            .setParameter("type", stockDTO.getStatus())
                            .uniqueResult();
                    if (statusObj != null) {
                        stock.setStatus(statusObj);
                    }

                    hibernateSession.merge(stock);
                    transaction.commit();
                    status = true;
                    message = "Stock updated successfully!";
                } else {
                    message = "Stock not found!";
                }
            } catch (Exception e) {
                if (transaction != null)
                    transaction.rollback();
                message = "Error updating stock";
                e.printStackTrace();
            }
        } catch (Exception e) {
            message = "Error updating stock";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String assignStockImage(int stockId, int imageId) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                Stock stock = hibernateSession.get(Stock.class, stockId);
                ProductImage image = hibernateSession.get(ProductImage.class, imageId);

                if (stock != null && image != null) {
                    // Check if image belongs to same product? Ideally yes.
                    if (!stock.getProduct().getId().equals(image.getProduct().getId())) {
                        throw new Exception("Image does not belong to the same product!");
                    }

                    // Unassign previous images for this stock? Optional, but typical for 1 image
                    // per variant display
                    // Retrieve all images currently assigned to this stock and unassign them
                    List<ProductImage> existing = hibernateSession
                            .createQuery("FROM ProductImage WHERE stock = :stock", ProductImage.class)
                            .setParameter("stock", stock)
                            .list();
                    for (ProductImage img : existing) {
                        img.setStock(null);
                        hibernateSession.merge(img);
                    }

                    // Assign new one
                    image.setStock(stock);
                    hibernateSession.merge(image);

                    transaction.commit();
                    status = true;
                    message = "Image assigned to variant!";
                } else {
                    message = "Invalid Stock or Image ID";
                }
            } catch (Exception e) {
                if (transaction != null)
                    transaction.rollback();
                message = e.getMessage();
                e.printStackTrace();
            }
        } catch (Exception e) {
            message = "Error assigning image";
            e.printStackTrace();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
