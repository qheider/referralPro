package com.actpro.referral.security;

import com.actpro.referral.company.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyContext {

    private static final ThreadLocal<Company> currentCompany = new ThreadLocal<>();

    public void setCurrentCompany(Company company) {
        currentCompany.set(company);
    }

    public Company getCurrentCompany() {
        return currentCompany.get();
    }

    public void clear() {
        currentCompany.remove();
    }
}
