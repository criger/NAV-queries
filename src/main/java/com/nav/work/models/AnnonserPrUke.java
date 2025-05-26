package com.nav.work.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
public class AnnonserPrUke
{
    public AnnonserPrUke(String uke, int antallKotlin, int antallJava) {
        this.uke = uke;
        this.antallKotlin = antallKotlin;
        this.antallJava = antallJava;
    }

    private String uke;
    private int antallKotlin;

    public String getUke() {
        return uke;
    }

    public void setUke(String uke) {
        this.uke = uke;
    }

    public int getAntallKotlin() {
        return antallKotlin;
    }

    public void setAntallKotlin(int antallKotlin) {
        this.antallKotlin = antallKotlin;
    }

    public int getAntallJava() {
        return antallJava;
    }

    public void setAntallJava(int antallJava) {
        this.antallJava = antallJava;
    }

    private int antallJava;
}