package com.iaexample.iaexamples.fuctions.terraform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerraformCodeHolderTest {

    private TerraformCodeHolder codeHolder;

    @BeforeEach
    void setUp() {
        codeHolder = new TerraformCodeHolder();
    }

    @Test
    @DisplayName("Should store and retrieve Terraform code")
    void shouldStoreAndRetrieveCode() {
        String tfCode = "resource \"aws_s3_bucket\" \"test\" {}";

        codeHolder.store(tfCode);

        assertEquals(tfCode, codeHolder.retrieve());
    }

    @Test
    @DisplayName("Should return true when there is pending code")
    void shouldReturnTrueWhenHasPendingCode() {
        codeHolder.store("resource \"aws_s3_bucket\" \"test\" {}");

        assertTrue(codeHolder.hasPendingCode());
    }

    @Test
    @DisplayName("Should return false when no code has been stored")
    void shouldReturnFalseWhenNoCodeStored() {
        assertFalse(codeHolder.hasPendingCode());
    }

    @Test
    @DisplayName("Should return false when stored code is empty")
    void shouldReturnFalseWhenCodeIsEmpty() {
        codeHolder.store("");

        assertFalse(codeHolder.hasPendingCode());
    }

    @Test
    @DisplayName("Should clear pending code")
    void shouldClearPendingCode() {
        codeHolder.store("resource \"aws_s3_bucket\" \"test\" {}");

        codeHolder.clear();

        assertFalse(codeHolder.hasPendingCode());
        assertNull(codeHolder.retrieve());
    }

    @Test
    @DisplayName("Should overwrite previously stored code")
    void shouldOverwritePreviouslyStoredCode() {
        codeHolder.store("first code");
        codeHolder.store("second code");

        assertEquals("second code", codeHolder.retrieve());
    }
}
