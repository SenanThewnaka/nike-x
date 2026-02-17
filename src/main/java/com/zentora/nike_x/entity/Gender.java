package com.zentora.nike_x.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "gender")
@NamedQueries({
        @NamedQuery(name = "Gender.findByName", query = "SELECT g FROM Gender g WHERE lower(g.name) = lower(:name)")
})
public class Gender {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "gender", length = 20, nullable = false)
    private String name;

    public Gender() {
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
