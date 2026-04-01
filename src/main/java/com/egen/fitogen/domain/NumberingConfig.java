package com.egen.fitogen.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NumberingConfig {

    private int id;
    private NumberingType type;

    private NumberingSectionType section1Type;
    private String section1StaticValue;
    private String section1Separator;

    private NumberingSectionType section2Type;
    private String section2StaticValue;
    private String section2Separator;

    private NumberingSectionType section3Type;
    private String section3StaticValue;
    private String section3Separator;

    private int currentCounter;
}