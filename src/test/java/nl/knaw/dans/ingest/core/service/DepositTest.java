package nl.knaw.dans.ingest.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepositTest {

    @Test
    void getOtherDoiId_should_return_null_if_doi_is_null() {
        var deposit = new Deposit();
        deposit.setDataverseIdProtocol("protocol");
        deposit.setDataverseIdAuthority("authority");
        deposit.setDataverseId("id");

        assertNull(deposit.getOtherDoiId());
    }
    @Test
    void getOtherDoiId_should_return_null_if_doi_is_empty_string() {
        var deposit = new Deposit();
        deposit.setDataverseIdProtocol("protocol");
        deposit.setDataverseIdAuthority("authority");
        deposit.setDataverseId("id");
        deposit.setDoi(" ");

        assertNull(deposit.getOtherDoiId());
    }

    @Test
    void getOtherDoiId_should_return_null_if_doi_is_the_same() {
        var deposit = new Deposit();
        deposit.setDataverseIdProtocol("doi");
        deposit.setDataverseIdAuthority("a");
        deposit.setDataverseId("b");
        deposit.setDoi("a/b");

        assertNull(deposit.getOtherDoiId());
    }

    @Test
    void getOtherDoiId_should_return_doi_if_doi_is_different() {
        var deposit = new Deposit();
        deposit.setDataverseIdProtocol("doi");
        deposit.setDataverseIdAuthority("a");
        deposit.setDataverseId("c");
        deposit.setDoi("a/b");

        assertEquals("doi:a/b", deposit.getOtherDoiId());
    }
}