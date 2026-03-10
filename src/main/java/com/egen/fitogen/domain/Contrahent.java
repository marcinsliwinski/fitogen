package com.egen.fitogen.domain;

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

    private String phytosanitaryNumber;
}