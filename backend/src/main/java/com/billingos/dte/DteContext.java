package com.billingos.dte;

import com.billingos.branch.Branch;
import com.billingos.branch.PointOfSale;
import com.billingos.company.Company;
import com.billingos.customer.Customer;
import com.billingos.customer.CustomerTaxProfile;
import com.billingos.invoice.Invoice;

public record DteContext(
        Invoice invoice,
        PointOfSale pos,
        Branch branch,
        Company company,
        Customer customer,
        CustomerTaxProfile taxProfile
) {}
