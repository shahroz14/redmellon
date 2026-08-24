package com.redmellon.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "ingest.broker=ZERODHA",
    "ingest.api-key=test-key",
    "ingest.api-secret=test-secret",
    "ingest.base-url=https://test.example.com",
    "ingest.auto-login=false",
    "ingest.storage-dir=/tmp/redmellon-test",
    "ingest.symbols=",
    "ingest.accounts[0].user-id=test-user",
    "ingest.accounts[0].password=test-pass",
    "ingest.accounts[0].totp-secret=AAAAAAAAAAAAAAAA"
})
class LiveIngestApplicationTests {

    @Test
    void contextLoads() {
    }
}
