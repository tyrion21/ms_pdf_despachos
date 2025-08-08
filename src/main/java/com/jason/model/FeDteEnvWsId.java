package com.jason.model;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class FeDteEnvWsId implements Serializable {
    @Column(name = "COD_EMP", nullable = false, length = 4)
    private String codEmp;

    @Column(name = "COD_TIPODOC", nullable = false)
    private Integer codTipodoc;

    @Column(name = "CAF", nullable = false)
    private Float caf;

    // Constructor, equals, hashCode (importante para claves compuestas)
    public FeDteEnvWsId() {
    }

    public FeDteEnvWsId(String codEmp, Integer codTipodoc, Float caf) {
        this.codEmp = codEmp;
        this.codTipodoc = codTipodoc;
        this.caf = caf;
    }

    public String getCodEmp() {
        return codEmp;
    }

    public Integer getCodTipodoc() {
        return codTipodoc;
    }

    public Float getCaf() {
        return caf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FeDteEnvWsId that = (FeDteEnvWsId) o;
        return codEmp.equals(that.codEmp) && codTipodoc.equals(that.codTipodoc) && caf.equals(that.caf);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(codEmp, codTipodoc, caf);
    }
}
