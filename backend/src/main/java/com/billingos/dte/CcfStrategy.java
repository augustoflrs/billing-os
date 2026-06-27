package com.billingos.dte;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Comprobante de Crédito Fiscal — documento tipo 03.
 * Receptor must have NIT + NRC (contribuyente).
 */
@Component
class CcfStrategy extends BaseDtePayloadBuilder {

    @Override
    public String documentTypeCode() { return "03"; }

    @Override
    protected int dteVersion() { return 3; }

    @Override
    protected Map<String, Object> buildReceptor(DteContext ctx) {
        var customer   = ctx.customer();
        var taxProfile = ctx.taxProfile();

        if (taxProfile == null) {
            throw new IllegalStateException(
                "Customer " + customer.getId() + " has no tax profile; required for CCF (03)");
        }

        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("tipoDocumento",  taxProfile.getDocumentType());
        rec.put("numDocumento",   taxProfile.getDocumentNumber());
        rec.put("nrc",            taxProfile.getNrc());
        rec.put("nombre",         customer.getLegalName());
        rec.put("codActividad",   taxProfile.getEconomicActivityCode());
        rec.put("descActividad",  null);
        rec.put("direccion",      null);
        rec.put("telefono",       customer.getPhone());
        rec.put("correo",         customer.getEmail());
        return rec;
    }
}
