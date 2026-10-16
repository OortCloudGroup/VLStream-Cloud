package com.ruoyi.vlstream.test.vlstream.compute;

/** Uses the normal node DTO, including all inherited credential/tenant JSON exclusions. */
public class CompanyManagedComputeNode extends ComputeNode {
    private String companyOfferCode;
    private boolean companyPublicationAllowed;
    public boolean isCompanyManaged() { return true; }
    public String getConfigurationState() { return "PENDING_CREDENTIALS"; }
    public String getCompanyOfferCode() { return companyOfferCode; }
    public void setCompanyOfferCode(String value) { companyOfferCode=value; }
    public boolean isCompanyPublicationAllowed() { return companyPublicationAllowed; }
    public void setCompanyPublicationAllowed(boolean value) { companyPublicationAllowed=value; }
}
