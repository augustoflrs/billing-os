package com.billingos.dte;

import com.billingos.invoice.InvoiceLine;
import com.billingos.invoice.InvoiceLineTax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Shared helpers for building MH DTE JSON payloads (Factura 01, CCF 03).
 *
 * All monetary values are rounded to 2 decimal places as required by MH.
 * The raw invoice stores 4-decimal precision; we truncate here for the DTE.
 */
abstract class BaseDtePayloadBuilder implements DteDocumentStrategy {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ZoneId SV_ZONE = ZoneId.of("America/El_Salvador");

    // MH DTE version: 1 for Factura, 3 for CCF
    protected abstract int dteVersion();

    @Override
    public Map<String, Object> buildPayload(DteContext ctx, String generationCode, String controlNumber) {
        var invoice = ctx.invoice();
        var company = ctx.company();
        var branch  = ctx.branch();
        var pos     = ctx.pos();
        var date    = invoice.getInvoiceDate().atZone(SV_ZONE).toLocalDateTime();

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("identificacion",    buildIdentificacion(dteVersion(), documentTypeCode(),
                                       controlNumber, generationCode, date));
        root.put("documentoRelacionado", null);
        root.put("emisor",            buildEmisor(company, branch, pos));
        root.put("receptor",          buildReceptor(ctx));
        root.put("otrosDocumentos",   null);
        root.put("ventaTercero",      null);
        root.put("cuerpoDocumento",   buildCuerpoDocumento(ctx));
        root.put("resumen",           buildResumen(ctx));
        root.put("extension",         null);
        root.put("apendice",          null);
        return root;
    }

    private static Map<String, Object> buildIdentificacion(int version, String tipoDte,
                                                            String controlNumber, String genCode,
                                                            java.time.LocalDateTime date) {
        Map<String, Object> id = new LinkedHashMap<>();
        id.put("version",          version);
        id.put("ambiente",         "00");   // 00=pruebas, 01=produccion
        id.put("tipoDte",          tipoDte);
        id.put("numeroControl",    controlNumber);
        id.put("codigoGeneracion", genCode);
        id.put("tipoModelo",       1);       // 1=transmision normal
        id.put("tipoOperacion",    1);       // 1=normal
        id.put("tipoContingencia", null);
        id.put("motivoContin",     null);
        id.put("fecEmi",           date.format(DATE_FMT));
        id.put("horEmi",           date.format(TIME_FMT));
        id.put("tipoMoneda",       "USD");
        return id;
    }

    private static Map<String, Object> buildEmisor(
            com.billingos.company.Company company,
            com.billingos.branch.Branch branch,
            com.billingos.branch.PointOfSale pos) {

        Map<String, Object> dir = new LinkedHashMap<>();
        dir.put("departamento", branch.getDepartmentCode() != null ? branch.getDepartmentCode() : "06");
        dir.put("municipio",    branch.getMunicipalityCode() != null ? branch.getMunicipalityCode() : "14");
        dir.put("complemento",  branch.getAddressLine1());

        String actCode = company.getEconomicActivity() != null
                ? company.getEconomicActivity().getCode() : null;
        String actName = company.getEconomicActivity() != null
                ? company.getEconomicActivity().getName() : null;

        Map<String, Object> em = new LinkedHashMap<>();
        em.put("nit",                formatNit(company.getNit()));
        em.put("nrc",                company.getNrc() != null ? company.getNrc().replace("-", "") : null);
        em.put("nombre",             company.getLegalName());
        em.put("codActividad",       actCode);
        em.put("descActividad",      actName);
        em.put("nombreComercial",    company.getTradeName() != null ? company.getTradeName() : company.getLegalName());
        em.put("tipoEstablecimiento","01");  // local comercial
        em.put("direccion",          dir);
        em.put("telefono",           company.getPhone());
        em.put("correo",             company.getEmail());
        em.put("codEstableMH",       null);
        em.put("codEstable",         pad4(branch.getCode()));
        em.put("codPuntoVentaMH",    null);
        em.put("codPuntoVenta",      pad4(pos.getCode()));
        return em;
    }

    protected abstract Map<String, Object> buildReceptor(DteContext ctx);

    private List<Map<String, Object>> buildCuerpoDocumento(DteContext ctx) {
        var lines = ctx.invoice().getLines();
        List<Map<String, Object>> body = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            body.add(buildLineItem(i + 1, lines.get(i)));
        }
        return body;
    }

    private Map<String, Object> buildLineItem(int numItem, InvoiceLine line) {
        List<String> tributos = line.getTaxes().stream()
                .map(t -> taxCodeToMhCode(t.getTaxCode()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        double ventaGravada = round2(line.getSubtotalAmount());
        double ivaItem      = round2(line.getTaxAmount());

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("numItem",         numItem);
        item.put("tipoItem",        2);       // 2=servicio; TODO: derive from billable item type
        item.put("numeroDocumento", null);
        item.put("cantidad",        round2bd(line.getQuantity()));
        item.put("codigo",          null);
        item.put("codTributo",      null);
        item.put("uniMedida",       59);      // 59=unidad
        item.put("descripcion",     line.getItemName());
        item.put("precioUni",       round2(line.getUnitPrice()));
        item.put("montoDescu",      round2(line.getDiscountAmount()));
        item.put("ventaNoSuj",      0.00);
        item.put("ventaExenta",     0.00);
        item.put("ventaGravada",    ventaGravada);
        item.put("tributos",        tributos.isEmpty() ? null : tributos);
        item.put("psv",             0.00);
        item.put("noGravado",       0.00);
        item.put("ivaItem",         ivaItem);
        return item;
    }

    private Map<String, Object> buildResumen(DteContext ctx) {
        var inv = ctx.invoice();

        double totalGravada = round2(inv.getSubtotalAmount());
        double totalDescu   = round2(inv.getDiscountAmount());
        double totalIva     = round2(inv.getTaxAmount());
        double totalPagar   = round2(inv.getTotalAmount());

        List<Map<String, Object>> tributos = new ArrayList<>();
        // Aggregate IVA across all lines
        Map<String, BigDecimal> taxTotals = new LinkedHashMap<>();
        for (var line : inv.getLines()) {
            for (InvoiceLineTax t : line.getTaxes()) {
                taxTotals.merge(t.getTaxCode(), t.getTaxAmount(), BigDecimal::add);
            }
        }
        for (var entry : taxTotals.entrySet()) {
            String mhCode = taxCodeToMhCode(entry.getKey());
            if (mhCode != null) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("codigo",      mhCode);
                t.put("descripcion", taxCodeDescription(entry.getKey()));
                t.put("valor",       round2(entry.getValue()));
                tributos.add(t);
            }
        }

        List<Map<String, Object>> pagos = new ArrayList<>();
        Map<String, Object> pago = new LinkedHashMap<>();
        pago.put("codigo",      "01");  // 01=billetes y monedas
        pago.put("montoPago",   totalPagar);
        pago.put("referencia",  null);
        pago.put("plazo",       null);
        pago.put("periodo",     null);
        pagos.add(pago);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalNoSuj",             0.00);
        res.put("totalExenta",            0.00);
        res.put("totalGravada",           totalGravada);
        res.put("subTotalVentas",         totalGravada);
        res.put("descuNoSuj",             0.00);
        res.put("descuExenta",            0.00);
        res.put("descuGravada",           totalDescu);
        res.put("porcentajeDescuento",    totalGravada > 0 ? round2bd(inv.getDiscountAmount().divide(
                inv.getSubtotalAmount().add(inv.getDiscountAmount()).max(BigDecimal.ONE),
                4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))) : 0.00);
        res.put("totalDescu",             totalDescu);
        res.put("tributos",               tributos.isEmpty() ? null : tributos);
        res.put("subTotal",               totalGravada);
        res.put("ivaRete1",               0.00);
        res.put("reteRenta",              0.00);
        res.put("montoTotalOperacion",    totalPagar);
        res.put("totalNoGravado",         0.00);
        res.put("totalPagar",             totalPagar);
        res.put("totalLetras",            AmountToWords.convert(inv.getTotalAmount()));
        res.put("totalIva",               totalIva);
        res.put("saldoFavor",             0.00);
        res.put("condicionOperacion",     1);  // 1=contado
        res.put("pagos",                  pagos);
        res.put("numPagoElectronico",     null);
        return res;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String taxCodeToMhCode(String taxCode) {
        // MH tax codes: "20" = IVA
        if ("IVA".equalsIgnoreCase(taxCode)) return "20";
        return null;
    }

    private static String taxCodeDescription(String taxCode) {
        if ("IVA".equalsIgnoreCase(taxCode)) return "Impuesto al Valor Agregado 13%";
        return taxCode;
    }

    static String formatNit(String nit) {
        if (nit == null) return null;
        // Normalize to digits only, then format as 14-digit string
        String digits = nit.replaceAll("[^0-9]", "");
        return digits;
    }

    static String pad4(String code) {
        if (code == null) return "0001";
        String digits = code.replaceAll("[^0-9A-Za-z]", "");
        return String.format("%-4s", digits).substring(0, Math.min(4, Math.max(digits.length(), 4)))
                     .replace(' ', '0');
    }

    static double round2(BigDecimal v) {
        if (v == null) return 0.00;
        return v.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    static double round2bd(BigDecimal v) {
        if (v == null) return 0.00;
        return v.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
