package com.egen.fitogen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentType {

    private int id;
    private String name;
    private String code;

    @Override
    public String toString() {
        if (name == null) {
            return "";
        }
        if (code == null || code.isBlank()) {
            return name;
        }
        return name + " / " + code;
    }
}