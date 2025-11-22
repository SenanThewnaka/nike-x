package com.zentora.nike_x.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "mobile")
public class Mobile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "contry_code", length = 45, nullable = false)
    private String countryCode;

    @Column(name = "mobile", length = 45, nullable = false)
    private String mobile;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Mobile() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
