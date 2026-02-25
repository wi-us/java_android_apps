package com.example.a4_2_food;

public class Cookbook {
    public long id;
    public String nameRu;
    public String nameEn;
    public String nameEs;

    public Cookbook(long id, String nameRu, String nameEn, String nameEs) {
        this.id = id;
        this.nameRu = nameRu;
        this.nameEn = nameEn;
        this.nameEs = nameEs;
    }

    public String getName(String lang) {
        if ("en".equals(lang)) return nameEn;
        if ("es".equals(lang)) return nameEs;
        return nameRu;
    }
}
