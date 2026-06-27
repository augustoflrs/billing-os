package com.billingos.dte;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factura de Consumidor Final — documento tipo 01.
 * Receptor may be anonymous (no NIT required).
 */
@Component
class FacturaStrategy extends BaseDtePayloadBuilder {

    @Override
    public String documentTypeCode() { return "01"; }

    @Override
    protected int dteVersion() { return 1; }

    @Override
    protected Map<String, Object> buildReceptor(DteContext ctx) {
        var customer   = ctx.customer();
        var taxProfile = ctx.taxProfile();

        String docType = taxProfile != null ? taxProfile.getDocumentType() : "13"; // 13=DUI
        String docNum  = taxProfile != null ? taxProfile.getDocumentNumber() : null;

        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("tipoDocumento",  docType);
        rec.put("numDocumento",   docNum);
        rec.put("nrc",            null);
        rec.put("nombre",         customer.getLegalName());
        rec.put("codActividad",   null);
        rec.put("descActividad",  null);
        rec.put("direccion",      null);
        rec.put("telefono",       customer.getPhone());
        rec.put("correo",         customer.getEmail());
        return rec;
    }
}
