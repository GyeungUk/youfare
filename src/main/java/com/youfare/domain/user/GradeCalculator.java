package com.youfare.domain.user;

public class GradeCalculator {

    public static String calculate(Integer point) {
        if (point == null) return "새싹";
        if (point >= 1000) return "나무";
        if (point >= 500)  return "열매";
        if (point >= 200)  return "꽃";
        if (point >= 50)   return "잎새";
        return "새싹";
    }
}
