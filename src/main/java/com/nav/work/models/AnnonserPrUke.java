package com.nav.work.models;

import lombok.Builder;

@Builder
public class AnnonserPrUke
{
    private String uke;
    private int antallKotlin;
    private int antallJava;

    public AnnonserPrUke(String uke, int antallKotlin, int antallJava) {
        this.uke = uke;
        this.antallKotlin = antallKotlin;
        this.antallJava = antallJava;
    }

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

}