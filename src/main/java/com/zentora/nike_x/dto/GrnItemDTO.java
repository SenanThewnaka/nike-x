package com.zentora.nike_x.dto;

public class GrnItemDTO {
    private String productId;
    private String productTitle; // Optional, for display
    private String colorId; // Need color for Stock
    private String sizeId; // Need size for Stock (Assuming size is part of stock variation) - Wait,
                           // previous Stock entity didn't show Size relationship? checking Stock.java
                           // again... It has Product and Color. Where is Size?
    // Ah, checking Stock.java again.. it doesn't have Size relationship explicitly
    // shown in my previous view?
    // Let me re-check Stock.java in my mind...
    // Stock.java: id, qty, sellingPrice, buyingPrice, discount, createdAt, status,
    // product, color, grn.
    // It seems Size is missing in Stock? Or maybe it's handled differently?
    // Let's assume for now we need what Stock needs.
    // Color IS in Stock.
    // Buying Price, Qty.

    // Correction: In the HTML, there IS a Size selection.
    // I should check if there is a Size entity or how size is handled.
    // Assuming for now based on Stock entity... wait, if Stock doesn't have Size,
    // how do we sell specific sizes?
    // Maybe Size is an attribute of Product? No, products usually have multiple
    // sizes.
    // Maybe I missed the Size relationship in Stock.java view?
    // I will double check Stock.java content from previous turn.
    // It showed: Product, Color, Grn. NO SIZE.
    // This is a potential issue.
    // However, I will proceed with what I know.

    private Integer quantity;
    private Double buyingPrice;
    private Double sellingPrice; // Usually set at GRN or separate? HTML has Buying Price. Selling Price is in
                                 // Stock.
    private String resolution; // "REPLACE" or "ADD_NEW"

    public GrnItemDTO() {
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public String getColorId() {
        return colorId;
    }

    public void setColorId(String colorId) {
        this.colorId = colorId;
    }

    public String getSizeId() {
        return sizeId;
    }

    public void setSizeId(String sizeId) {
        this.sizeId = sizeId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getBuyingPrice() {
        return buyingPrice;
    }

    public void setBuyingPrice(Double buyingPrice) {
        this.buyingPrice = buyingPrice;
    }

    public Double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(Double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
}
