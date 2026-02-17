package com.zentora.nike_x.dto;

import java.util.List;

public class OrderDTO {
    private int id;
    private String date; // Formatted date string
    private double totalAmount;
    private String status;
    private String paymentMethod;

    // User/Contact Info
    private String customerName;
    private String customerEmail;
    private String customerMobile;

    // Shipping Address
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String postalCode;

    private List<OrderItemDTO> items;
    private String firstProductName;

    public String getFirstProductName() {
        return firstProductName;
    }

    public void setFirstProductName(String firstProductName) {
        this.firstProductName = firstProductName;
    }

    private String firstProductImage;
    private int itemCount;

    public String getFirstProductImage() {
        return firstProductImage;
    }

    public void setFirstProductImage(String firstProductImage) {
        this.firstProductImage = firstProductImage;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public OrderDTO() {
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerMobile() {
        return customerMobile;
    }

    public void setCustomerMobile(String customerMobile) {
        this.customerMobile = customerMobile;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }

    // Nested DTO for Items
    public static class OrderItemDTO {
        private String productName;
        private String variance; // "Size: M, Color: Red"
        private int qty;
        private double price;
        private double total;

        public OrderItemDTO() {
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getVariance() {
            return variance;
        }

        public void setVariance(String variance) {
            this.variance = variance;
        }

        public int getQty() {
            return qty;
        }

        public void setQty(int qty) {
            this.qty = qty;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        private String imagePath;

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }
    }
}
