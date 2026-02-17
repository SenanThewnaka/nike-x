package com.zentora.nike_x.dto;

import java.io.Serializable;
import java.util.List;

public class SearchRequestDTO implements Serializable {
    private String query;
    private Double minPrice;
    private Double maxPrice;
    private List<Integer> brandIds;
    private List<Integer> sizeIds;
    private List<Integer> colorIds;
    private String sort; // NEWEST, PRICE_HI_LO, PRICE_LO_HI, POPULARITY
    private int page = 1;
    private int pageSize = 12;

    public SearchRequestDTO() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public List<Integer> getBrandIds() {
        return brandIds;
    }

    public void setBrandIds(List<Integer> brandIds) {
        this.brandIds = brandIds;
    }

    public List<Integer> getSizeIds() {
        return sizeIds;
    }

    public void setSizeIds(List<Integer> sizeIds) {
        this.sizeIds = sizeIds;
    }

    public List<Integer> getColorIds() {
        return colorIds;
    }

    public void setColorIds(List<Integer> colorIds) {
        this.colorIds = colorIds;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
