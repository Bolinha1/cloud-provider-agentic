package com.iaexample.iaexamples.fuctions.terraform;

import org.springframework.stereotype.Component;

@Component
public class TerraformCodeHolder {

    private String pendingTerraformCode;

    public void store(String tfCode) {
        this.pendingTerraformCode = tfCode;
    }

    public String retrieve() {
        return pendingTerraformCode;
    }

    public boolean hasPendingCode() {
        return pendingTerraformCode != null && !pendingTerraformCode.isEmpty();
    }

    public void clear() {
        this.pendingTerraformCode = null;
    }
}
