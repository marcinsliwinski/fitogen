package com.egen.fitogen.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                   // generuje gettery, settery, toString, equals i hashCode
@NoArgsConstructor       // generuje konstruktor bezargumentowy
@AllArgsConstructor      // generuje konstruktor pełny z wszystkimi polami

public class Contrahent {

    private int id;
    private String name;
    private String countryCode;
    private String nip;

    // jeśli chcesz, możesz też dodać konstruktor bez id:
    public Contrahent(String name, String countryCode, String nip) {
        this.name = name;
        this.countryCode = countryCode;
        this.nip = nip;
    }
}