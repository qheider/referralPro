package com.actpro.referral.security;

import com.actpro.referral.company.Company;

public class CompanyContext {

    private static final ThreadLocal<Company> currentCompany = new ThreadLocal<>();

    public static void setCurrentCompany(Company company) {
        currentCompany.set(company);
    }

    public static Company getCurrentCompany() {
        return currentCompany.get();
    }

    public static void clear() {
        currentCompany.remove();
    }
}
