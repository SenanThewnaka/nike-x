package com.zentora.nike_x.dto;

import java.io.Serializable;
import java.util.List;

public class ProductDTO implements Serializable {
    private int id;
    private String name;
    private String description;
    private int brandId;
    private String brandName;
    private String newBrandName;
    private String modelName;
    private String gender;
    private List<ProductImageDTO> images;
    private String status;
    private String imagePath;
    private Long stockQty;
    private Double price;

    public ProductDTO() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getBrandId() {
        return brandId;
    }

    public void setBrandId(int brandId) {
        this.brandId = brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getNewBrandName() {
        return newBrandName;
    }

    public void setNewBrandName(String newBrandName) {
        this.newBrandName = newBrandName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<ProductImageDTO> getImages() {
        return images;
    }

    public void setImages(List<ProductImageDTO> images) {
        this.images = images;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Long getStockQty() {
        return stockQty;
    }

    public void setStockQty(Long stockQty) {
        this.stockQty = stockQty;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}