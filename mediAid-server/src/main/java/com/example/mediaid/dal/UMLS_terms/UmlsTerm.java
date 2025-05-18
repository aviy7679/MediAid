package com.example.mediaid.dal.UMLS_terms;

// package com.example.mediaid.model;
public class UmlsTerm {
    private final String name;
    private final String sab;
    private final String tty;
    private final String ispref;

    public UmlsTerm(String name, String sab, String tty, String ispref) {
        this.name = name;
        this.sab = sab;
        this.tty = tty;
        this.ispref = ispref;
    }

    public String getName() { return name; }
    public String getSab() { return sab; }
    public String getTty() { return tty; }
    public String getIspref() { return ispref; }
}
