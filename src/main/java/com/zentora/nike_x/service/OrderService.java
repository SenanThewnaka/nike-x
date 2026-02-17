
package com.zentora.nike_x.service;

import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.UserDTO;
import com.zentora.nike_x.entity.Address;
import com.zentora.nike_x.entity.CartItem;
import com.zentora.nike_x.entity.Order;
import com.zentora.nike_x.entity.OrderItem;
import com.zentora.nike_x.entity.Status;
import com.zentora.nike_x.entity.Stock;
import com.zentora.nike_x.entity.User;
import com.zentora.nike_x.util.AppUtil;
import com.zentora.nike_x.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;

public class OrderService {

    // --- PROCESS 1: Full Cart Checkout ---
    public String placeCartOrder(UserDTO userDTO, int addressId, String paymentMethod, String mobileNumber) {
        return processOrder(userDTO, addressId, paymentMethod, mobileNumber, 0);
    }

    // --- PROCESS 2: Single Product Checkout (Direct Buy) ---
    public String placeSingleOrder(UserDTO userDTO, int addressId, String paymentMethod, String mobileNumber,
            int stockId) {
        return processOrder(userDTO, addressId, paymentMethod, mobileNumber, stockId);
    }

    // Internal shared logic (Facade)
    private String processOrder(UserDTO userDTO, int addressId, String paymentMethod, String mobileNumber,
            int stockId) {
        // Force Payment Method to 'Card' to trigger PayHere logic
        paymentMethod = "Card";

        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = validateOrderRequest(userDTO, addressId, paymentMethod, mobileNumber);

        if (message != null) {
            // Validation failed
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", message);
            return AppUtil.GSON.toJson(responseObject);
        }

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = hibernateSession.beginTransaction();

        try {
            // 1. Fetch User & Address
            User user = hibernateSession.get(User.class, userDTO.getId());
            if (user == null)
                throw new Exception("User not found!");

            Address address = hibernateSession.get(Address.class, addressId);
            if (address == null)
                throw new Exception("Address not found!");
            if (!address.getUser().getId().equals(user.getId()))
                throw new Exception("Invalid address!");

            // 2. Fetch Items (Two different strategies)
            List<CartItem> cartItems = new ArrayList<>();

            if (stockId > 0) {
                // Strategy A: Single Item (Direct from Stock, bypassing Cart Table)
                Stock stock = hibernateSession.get(Stock.class, stockId);

                // Fetch Active Status ID
                Status activeStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("type", "ACTIVE")
                        .uniqueResult();

                if (stock == null || stock.getQty() < 1 || !stock.getStatus().getId().equals(activeStatus.getId())) {
                    throw new Exception("Product unavailable or out of stock.");
                }

                // Create transient CartItem container for processing (not persisted)
                CartItem tempItem = new CartItem();
                tempItem.setStock(stock);
                tempItem.setQty(1); // Default to 1 for Buy Now, or pass qty param if needed
                tempItem.setCart(null); // No cart association

                cartItems.add(tempItem);

            } else {
                // Strategy B: Full Cart
                Query<CartItem> q = hibernateSession.createQuery(
                        "FROM CartItem ci WHERE ci.cart.user.id=:uid", CartItem.class);
                q.setParameter("uid", user.getId());
                cartItems = q.getResultList();
            }

            if (cartItems.isEmpty()) {
                throw new Exception("Cart is empty!");
            }

            // 3. Create Order Record
            Order order = createBaseOrder(hibernateSession, user, address, paymentMethod);

            // 4. Process Items & Deduct Stock
            double totalAmount = 0.0;
            for (CartItem cartItem : cartItems) {
                Stock stock = cartItem.getStock();
                int qty = cartItem.getQty();

                if (stock.getQty() < qty) {
                    throw new Exception("Insufficient stock for: " + stock.getProduct().getName());
                }

                // Update Stock
                stock.setQty(stock.getQty() - qty);
                hibernateSession.merge(stock);

                // Add Order Item
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setStock(stock);
                orderItem.setQuantity(qty);
                orderItem.setPrice(stock.getSellingPrice());
                hibernateSession.persist(orderItem);

                totalAmount += (stock.getSellingPrice() * qty);
            }

            // 5. Update Total & Remove from Cart
            order.setTotalAmount(totalAmount);
            hibernateSession.merge(order);

            // 6. Clear Cart (Only if they were real cart items)
            for (CartItem ci : cartItems) {
                if (ci.getCart() != null) {
                    hibernateSession.remove(ci);
                }
            }

            transaction.commit();
            status = true;
            message = "Order placed successfully! Order ID: #" + order.getId();
            responseObject.addProperty("orderId", order.getId()); // Explicitly return ID for frontend redirection

            // 6. Payment Processing (PayHere)
            if ("Card".equalsIgnoreCase(paymentMethod)) {
                try {
                    JsonObject payhereParams = generatePayHereParams(order, user, address, mobileNumber);
                    responseObject.add("payhereParams", payhereParams);
                } catch (Exception paramEx) {
                    paramEx.printStackTrace();
                    status = false; // Mark as failed request
                    message = "Payment Initialization Failed: " + paramEx.getMessage();
                }
            }

        } catch (Exception e) {
            if (transaction != null)
                transaction.rollback();
            e.printStackTrace();
            message = e.getMessage() != null ? e.getMessage() : "Order placement failed!";
        } finally {
            hibernateSession.close();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    // --- Helpers ---

    private String validateOrderRequest(UserDTO user, int addrId, String method, String mobile) {
        if (user == null)
            return "Please sign in first!";
        if (addrId <= 0)
            return "Please select a valid address!";
        if (method == null || method.isBlank())
            return "Please select a payment method!";
        if (mobile == null || !mobile.matches("^07[01245678][0-9]{7}$"))
            return "Invalid mobile number!";
        return null;
    }

    private Order createBaseOrder(Session session, User user, Address address, String paymentMethod) throws Exception {
        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setPaymentMethod(paymentMethod);

        // Fetch Status
        Status pending = session.createQuery("FROM Status s WHERE s.type='PENDING'", Status.class).uniqueResult();
        if (pending == null)
            throw new Exception("Internal Error: Status configuration missing.");

        order.setStatus(pending);
        order.setTotalAmount(0.0); // Initialize to avoid Not-Null constraint violation
        order.setDateTime(new java.util.Date()); // Manually set date if CreationTimestamp fails or to be safe

        session.persist(order);
        return order;
    }

    public String getOrdersByUser(int userId) {
        JsonObject responseObject = new JsonObject();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Query<Order> query = session.createQuery("FROM Order o WHERE o.user.id = :uid ORDER BY o.dateTime DESC",
                    Order.class);
            query.setParameter("uid", userId);
            List<Order> orders = query.list();

            List<com.zentora.nike_x.dto.OrderDTO> orderDTOs = new ArrayList<>();
            for (Order order : orders) {
                com.zentora.nike_x.dto.OrderDTO dto = new com.zentora.nike_x.dto.OrderDTO();
                dto.setId(order.getId());
                dto.setTotalAmount(order.getTotalAmount());
                dto.setStatus(order.getStatus() != null ? order.getStatus().getType() : "Unknown");
                if (order.getDateTime() != null) {
                    dto.setDate(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(order.getDateTime()));
                } else {
                    dto.setDate("N/A");
                }

                // Map Items for Detailed History Card
                List<com.zentora.nike_x.dto.OrderDTO.OrderItemDTO> itemDTOs = new ArrayList<>();
                for (OrderItem item : order.getOrderItems()) {
                    com.zentora.nike_x.dto.OrderDTO.OrderItemDTO itemDto = new com.zentora.nike_x.dto.OrderDTO.OrderItemDTO();
                    if (item.getStock() != null && item.getStock().getProduct() != null) {
                        itemDto.setProductName(item.getStock().getProduct().getName());
                        // Assuming we might want to show image path if DTO has it?
                        // Current DTO definition (checked in step 5363 view of getOrderDetails) doesn't
                        // seem to explicitly set image path in DTO logic of getOrderDetails?
                        // Wait, getOrderDetails logic (lines 236-243) doesn't set image path.
                        // However, OrderItemDTO likely needs it for the new requirement.
                        // I will add image path logic if OrderItemDTO supports it.
                        // Let's check DTO structure or just set it and hope? No, safe bet:
                        // Just map what we have: Name and Variance.
                        // If frontend needs images, we need to ensure DTO has field.
                        // Let's assume for now we map what is there.
                        // Actually, to show "images" as requested, I need to fetch images.
                        // Fetch Image
                        try {
                            String imgPath = null;

                            // 1. Try to find Specific Stock Image
                            String hqlStock = "SELECT pi.path FROM ProductImage pi WHERE pi.stock.id = :sid";
                            Query<String> qStock = session.createQuery(hqlStock, String.class);
                            qStock.setParameter("sid", item.getStock().getId());
                            qStock.setMaxResults(1);
                            imgPath = qStock.uniqueResult();

                            // 2. If no stock specific image, find any product image
                            if (imgPath == null) {
                                String hqlProd = "SELECT pi.path FROM ProductImage pi WHERE pi.product.id = :pid";
                                Query<String> qProd = session.createQuery(hqlProd, String.class);
                                qProd.setParameter("pid", item.getStock().getProduct().getId());
                                qProd.setMaxResults(1);
                                imgPath = qProd.uniqueResult();
                            }

                            if (imgPath != null) {
                                itemDto.setImagePath(imgPath);
                            } else {
                                itemDto.setImagePath("assets/images/products/shoe.png"); // Fallback
                            }
                        } catch (Exception e) {
                            itemDto.setImagePath("assets/images/products/shoe.png"); // Fallback
                        }
                        // Product entity likely has images?
                        // Let's add a quick image fetch if possible or just use placeholder if DTO
                        // lacks field.
                        // Checking getOrderDetails again... it does NOT set image path.
                        // I will set variance and name for now.

                        String variance = (item.getStock().getSize() != null ? item.getStock().getSize().getName()
                                : "-")
                                + " / "
                                + (item.getStock().getColor() != null ? item.getStock().getColor().getName() : "-");
                        itemDto.setVariance(variance);
                    }
                    itemDto.setQty(item.getQuantity());
                    itemDto.setPrice(item.getPrice());
                    itemDto.setTotal(item.getPrice() * item.getQuantity());
                    itemDTOs.add(itemDto);
                }
                dto.setItems(itemDTOs);

                orderDTOs.add(dto);
            }

            responseObject.addProperty("status", true);
            responseObject.add("orders", AppUtil.GSON.toJsonTree(orderDTOs));

        } catch (Exception e) {
            e.printStackTrace();
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Error fetching order history");
        }
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getOrderDetails(int orderId, int userId) {
        JsonObject responseObject = new JsonObject();
        System.out.println("DEBUG: getOrderDetails called for OrderID: " + orderId + ", UserID: " + userId);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Order order = session.get(Order.class, orderId);

            if (order == null) {
                System.out.println("DEBUG: Order not found");
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", "Order not found");
            } else if (order.getUser().getId() != userId) {
                System.out.println("DEBUG: Unauthorized access. Order User: " + order.getUser().getId());
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", "Unauthorized access to order");
            } else {
                System.out.println("DEBUG: Order found, mapping to DTO...");
                // Map to DTO
                com.zentora.nike_x.dto.OrderDTO dto = new com.zentora.nike_x.dto.OrderDTO();
                dto.setId(order.getId());
                if (order.getDateTime() != null) {
                    dto.setDate(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(order.getDateTime()));
                } else {
                    dto.setDate("N/A");
                }

                dto.setTotalAmount(order.getTotalAmount());
                dto.setStatus(order.getStatus() != null ? order.getStatus().getType() : "Unknown");
                dto.setPaymentMethod(order.getPaymentMethod());

                // User Info
                dto.setCustomerName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
                dto.setCustomerEmail(order.getUser().getEmail());
                // Address Info
                if (order.getAddress() != null) {
                    dto.setAddressLine1(order.getAddress().getLineOne());
                    dto.setAddressLine2(order.getAddress().getLineTwo());
                    if (order.getAddress().getCity() != null)
                        dto.setCity(order.getAddress().getCity().getName());
                    dto.setPostalCode(order.getAddress().getPostalCode());
                }

                // Items
                List<com.zentora.nike_x.dto.OrderDTO.OrderItemDTO> itemDTOs = new ArrayList<>();
                for (OrderItem item : order.getOrderItems()) {
                    com.zentora.nike_x.dto.OrderDTO.OrderItemDTO itemDto = new com.zentora.nike_x.dto.OrderDTO.OrderItemDTO();
                    if (item.getStock() != null && item.getStock().getProduct() != null) {
                        itemDto.setProductName(item.getStock().getProduct().getName());
                        String variance = "Size: "
                                + (item.getStock().getSize() != null ? item.getStock().getSize().getName() : "-")
                                + ", Color: "
                                + (item.getStock().getColor() != null ? item.getStock().getColor().getName() : "-");
                        itemDto.setVariance(variance);
                    }
                    itemDto.setQty(item.getQuantity());
                    itemDto.setPrice(item.getPrice());
                    itemDto.setTotal(item.getPrice() * item.getQuantity());

                    itemDTOs.add(itemDto);
                }
                dto.setItems(itemDTOs);

                System.out.println("DEBUG: Mapping complete. Serializing...");
                responseObject.addProperty("status", true);
                responseObject.add("order", AppUtil.GSON.toJsonTree(dto));
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("DEBUG: Exception in getOrderDetails: " + e.getMessage());
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Error fetching order details");
        }
        return AppUtil.GSON.toJson(responseObject);
    }

    private JsonObject generatePayHereParams(Order order, User user, Address address, String mobile) {
        String merchantId = com.zentora.nike_x.util.PaymentConfig.MERCHANT_ID;
        String orderId = String.valueOf(order.getId());
        String amount = com.zentora.nike_x.util.PaymentConfig.getFormattedAmount(order.getTotalAmount());
        String currency = com.zentora.nike_x.util.PaymentConfig.CURRENCY;
        String merchantSecret = com.zentora.nike_x.util.PaymentConfig
                .getMd5(com.zentora.nike_x.util.PaymentConfig.MERCHANT_SECRET);

        String hashStr = merchantId + orderId + amount + currency + merchantSecret;
        String hash = com.zentora.nike_x.util.PaymentConfig.getMd5(hashStr);

        JsonObject params = new JsonObject();
        params.addProperty("merchant_id", merchantId);
        params.addProperty("return_url", "http://localhost:8080/nike-x/");
        params.addProperty("cancel_url", "http://localhost:8080/nike-x/");
        params.addProperty("notify_url", "http://localhost:8080/nike-x/api/order/notify");
        params.addProperty("first_name", user.getFirstName());
        params.addProperty("last_name", user.getLastName());
        params.addProperty("email", user.getEmail());
        params.addProperty("phone", mobile);
        params.addProperty("address", address.getLineOne() + ", " + address.getLineTwo());
        params.addProperty("city", address.getCity() != null ? address.getCity().getName() : "Unknown");
        params.addProperty("country", "Sri Lanka");
        params.addProperty("order_id", orderId);
        params.addProperty("items", "Order #" + orderId);
        params.addProperty("currency", currency);
        params.addProperty("amount", amount);
        params.addProperty("hash", hash);
        params.addProperty("sandbox", true);
        return params;
    }

    public String handlePaymentFailure(int orderId, int userId) {
        JsonObject responseObject = new JsonObject();
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Order order = session.get(Order.class, orderId);

            if (order != null && order.getUser().getId() == userId) {
                if ("PENDING".equalsIgnoreCase(order.getStatus().getType())) {
                    System.out.println("Processing Failed Payment for Order #" + orderId + ". deleting...");

                    // Restore Stock
                    for (OrderItem item : order.getOrderItems()) {
                        Stock stock = item.getStock();
                        stock.setQty(stock.getQty() + item.getQuantity());
                        session.merge(stock);
                    }

                    // Delete Order
                    session.remove(order);

                    transaction.commit();
                    responseObject.addProperty("status", true);
                    responseObject.addProperty("message", "Order cancelled due to payment failure.");
                } else {
                    responseObject.addProperty("status", false);
                    responseObject.addProperty("message",
                            "Order cannot be cancelled. Status: " + order.getStatus().getType());
                }
            } else {
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", "Order not found or access denied.");
            }
        } catch (Exception e) {
            if (transaction != null)
                transaction.rollback();
            e.printStackTrace();
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Error cancelling order");
        } finally {
            session.close();
        }
        return AppUtil.GSON.toJson(responseObject);
    }

    public boolean updatePaymentStatus(int orderId, int statusCode) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        boolean success = false;

        try {
            Order order = session.get(Order.class, orderId);
            if (order != null) {
                // Determine new status based on PayHere status code
                // 2 = Success, 0 = Pending, -1 = Canceled, -2 = Failed, -3 = Chargedback

                String currentStatus = order.getStatus().getType().toUpperCase();

                if (statusCode == 2) {
                    if (!"PAID".equals(currentStatus)) {
                        Status paidStatus = new Status();
                        paidStatus.setId(2); // Assuming 2 is PAID in DB
                        // Or fetch by type "PAID" if unsure of ID.
                        // For safety, let's assume OrderStatus entities are managed.

                        // Let's use HQL to find status to be safe
                        Status paidSt = session
                                .createQuery("FROM Status WHERE type = 'PAID'", Status.class).uniqueResult();
                        if (paidSt != null)
                            order.setStatus(paidSt);

                        session.merge(order);
                        System.out.println("Order #" + orderId + " marked as PAID.");
                    }
                    success = true;
                } else if (statusCode < 0) {
                    // Failed Payment -> Strict Delete
                    if ("PENDING".equals(currentStatus)) { // Only delete if currently pending
                        System.out.println("Payment Failed (Status: " + statusCode + "). Deleting Order #" + orderId);

                        // Restore Stock Logic
                        for (OrderItem item : order.getOrderItems()) {
                            Stock stock = item.getStock();
                            stock.setQty(stock.getQty() + item.getQuantity());
                            session.merge(stock);
                        }

                        // Permanently Delete Order
                        session.remove(order);
                        System.out.println("Order #" + orderId + " deleted from database.");
                    }
                    success = true;
                }
                transaction.commit();
            }
        } catch (Exception e) {
            if (transaction != null)
                transaction.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return success;
    }

    public String verifyOrderAndCapture(int orderId) {
        JsonObject responseObject = new JsonObject();
        boolean verified = false;

        // 1. Check local DB Status first
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Order order = session.get(Order.class, orderId);
            if (order != null) {
                String currentStatus = order.getStatus().getType().toUpperCase();
                if ("PAID".equals(currentStatus)) {
                    verified = true;
                } else if ("PENDING".equals(currentStatus)) {
                    // 2. Not Paid locally. Check via PayHere API
                    System.out.println("Local status is PENDING. Checking PayHere API for Order #" + orderId);
                    int apiStatus = com.zentora.nike_x.util.PayHereApiClient
                            .checkPaymentStatus(String.valueOf(orderId));

                    if (apiStatus == 2) {
                        // Success in API! Update DB. (Reuse updatePaymentStatus logic)
                        session.close();
                        boolean updated = updatePaymentStatus(orderId, 2);
                        if (updated)
                            verified = true;
                    } else if (apiStatus == -2 || apiStatus == -1) {
                        // API says Failed.
                        System.out.println("PayHere API says FAILED. Deleting...");
                        session.close();
                        updatePaymentStatus(orderId, apiStatus); // Will delete/fail
                        verified = false;
                    } else if (apiStatus == -99) {
                        // Config missing. Strict mode: FAIL.
                        System.err.println("Strict Mode: PayHere API not configured. Cannot verify.");
                        verified = false;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (session.isOpen())
                session.close();
        }

        if (verified) {
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Payment Verified");
        } else {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Payment Verification Failed");
            updatePaymentStatus(orderId, -2); // Force fail/delete if verification fails
            // Note: updatePaymentStatus creates its own session to handle deletion
        }
        return AppUtil.GSON.toJson(responseObject);
    }
}
