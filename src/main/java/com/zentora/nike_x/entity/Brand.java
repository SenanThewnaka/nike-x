package com.zentora.nike_x.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "brand")
@NamedQueries({
        @NamedQuery(name = "Brand.findByName", query = "SELECT b FROM Brand b WHERE lower(b.name) = lower(:name)")
})
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 45, nullable = false)
    private String name;

    public Brand() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
