package com.harshalsharma.passkeydemo.backendserv.domain;

import java.text.DecimalFormat;

public class LocationPrecisionMapper {

    static final DecimalFormat df = new DecimalFormat("#.##"); // Pattern for two decimal places (nearly 1km)

    public static double mapLocation(double variable) {
        return Double.parseDouble(df.format(variable));
    }

}
