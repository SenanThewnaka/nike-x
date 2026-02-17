package com.zentora.nike_x.dto;

import java.util.List;

public class GrnDTO {
    private Integer id; // For retrieval if needed
    private String supplierId;
    private String supplierInvoiceNumber;
    private Double totalAmount;
    private List<GrnItemDTO> grnItems;

    public GrnDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierInvoiceNumber() {
        return supplierInvoiceNumber;
    }

    public void setSupplierInvoiceNumber(String supplierInvoiceNumber) {
        this.supplierInvoiceNumber = supplierInvoiceNumber;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<GrnItemDTO> getGrnItems() {
        return grnItems;
    }

    public void setGrnItems(List<GrnItemDTO> grnItems) {
        this.grnItems = grnItems;
    }
}
