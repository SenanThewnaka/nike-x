package com.zentora.nike_x.dto;

import java.io.Serializable;

public class ProductImageDTO implements Serializable {
    private int id;
    private String path;

    public ProductImageDTO() {
    }

    public ProductImageDTO(int id, String path) {
        this.id = id;
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
