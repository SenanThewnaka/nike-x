package com.zentora.nike_x.dto;

import java.io.Serializable;

public class ProductDTO implements Serializable {
    private int productId;
    private int brandId;
    private String brandName;
    private int modelId;
    private String modelName;
    private String title;
    private String description;
    private int colorId;
    private String colorValue;
    private double price;
    private int qty;

    public ProductDTO() {
    }


}