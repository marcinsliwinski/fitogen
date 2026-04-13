package com.egen.fitogen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contrahent {

    private int id;
    private String name;
    private String country;
    private String countryCode;
    private String postalCode;
    private String city;
    private String street;
    private boolean noStreet;
    private String phytosanitaryNumber;
    private boolean supplier;
    private boolean client;

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}