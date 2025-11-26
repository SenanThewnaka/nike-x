package com.zentora.nike_x.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "status")
@NamedQuery(name = "Status.findByValue",
        query = "FROM Status s WHERE s.type=:type")
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "type", length = 45, nullable = false)
    private String type;

    public Status() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public enum Type {
        ACTIVE,
        PENDING,
        INACTIVE,
        BLOCKED,
        DELIVERED,
        PACKING,
        APPROVED,
        REJECTED,
        CANCELED,
        VERIFIED,
        RECEIVED,
        COMPLETED
    }
}
