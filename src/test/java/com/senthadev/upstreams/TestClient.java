package com.senthadev.upstreams;

import org.junit.Test;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestClient {

    @Test
    public void testPrintCerts() {
        Path trustStore = Paths.get("/Users/sentha/senthadev/src/tls-client-auth/certs/client1/apptruststore.jks");
        Path keyStore = Paths.get("/Users/sentha/senthadev/src/tls-client-auth/certs/client1/client1keystore.jks");
        char [] pass = "test123".toCharArray();

        Client client = new Client(trustStore, pass, keyStore, pass);
        client.printCerts(URI.create("https://api.senthadev.com:18443/"));
    }

    @Test
    public void testGetoutput() {
        Path trustStore = Paths.get("/Users/sentha/senthadev/src/tls-client-auth/certs/client1/apptruststore.jks");
        Path keyStore = Paths.get("/Users/sentha/senthadev/src/tls-client-auth/certs/client1/client1keystore.jks");
        char [] pass = "test123".toCharArray();

        Client client = new Client(trustStore, pass, keyStore, pass);
        client.makeRequest(URI.create("https://api.senthadev.com:18443/"))
                .thenAccept(System.out::println)
                .join();
    }

    @Test
    public void testGeneralOutput() {
        Client client = new Client();
        client.makeRequest(URI.create("https://www.google.no/"))
                .thenAccept(System.out::println)
                .join();
    }

    @Test
    public void testFailWithoutClientCert() {
        Client client = new Client();
        client.makeRequest(URI.create("https://api.senthadev.com:18443/"))
                .thenAccept(System.out::println)
                .join();
    }
}
