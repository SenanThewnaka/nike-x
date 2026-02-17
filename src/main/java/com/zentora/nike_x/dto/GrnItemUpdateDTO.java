package com.zentora.nike_x.dto;

public class GrnItemUpdateDTO {
    private Integer grnItemId;
    private Integer quantity;
    private Double buyingPrice;
    private Double sellingPrice;
    private Integer colorId;
    private Integer sizeId;

    public GrnItemUpdateDTO() {
    }

    public Integer getGrnItemId() {
        return grnItemId;
    }

    public void setGrnItemId(Integer grnItemId) {
        this.grnItemId = grnItemId;
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

    public Integer getColorId() {
        return colorId;
    }

    public void setColorId(Integer colorId) {
        this.colorId = colorId;
    }

    public Integer getSizeId() {
        return sizeId;
    }

    public void setSizeId(Integer sizeId) {
        this.sizeId = sizeId;
    }

    private Integer productId;

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }
}
