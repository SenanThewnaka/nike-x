package com.zentora.nike_x.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "model")
@NamedQueries({
        @NamedQuery(name = "Model.findByName", query = "SELECT m FROM Model m WHERE lower(m.name) = lower(:name)")
})
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 45, nullable = false)
    private String name;

    public Model() {
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
