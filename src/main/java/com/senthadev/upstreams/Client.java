package com.senthadev.upstreams;

import javax.net.ssl.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Client {

    private final Executor executorService;
    private HttpClient httpClient;

    public Client() {
        executorService = Executors.newFixedThreadPool(3);
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20L))
                .executor(executorService)
                .build();
    }

    public Client(Path trustStore, char [] trustStorePass, Path keyStore, char [] keyStorePass) throws RuntimeException{
        this();

        // Setting the custom SSL context
        // https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
            KeyStore ks = KeyStore.getInstance(keyStore.toFile(), keyStorePass);
            kmf.init(ks, keyStorePass);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
            KeyStore ts = KeyStore.getInstance(trustStore.toFile(), trustStorePass);
            tmf.init(ts);

            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        } catch (Exception oops) {
            throw new RuntimeException("Failed while creating SSL Context", oops);
        }

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20L))
                .sslContext(ctx)
                .executor(executorService)
                .build();
    }

    public CompletableFuture<String> makeRequest(URI endpoint) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(Duration.ofSeconds(30L))
                .build();

        return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .exceptionally(
                        throwable -> {
                            return String.format("Failed to access the uri. Cause %s", throwable.getCause());
                        }
                );
    }

    public void printCerts(URI endpoint) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(Duration.ofSeconds(30L))
                .build();

        httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(
                        resp -> {
                            Optional<SSLSession> ssl = resp.sslSession();
                            if (ssl.isPresent()) {
                                try {
                                    System.out.println("Server Certificates");
                                    for (Certificate peer : ssl.get().getPeerCertificates()) {
                                        System.out.println(peer.toString());
                                    }

                                    System.out.println("Local Certificates");
                                    for (Certificate local : ssl.get().getLocalCertificates()) {
                                        System.out.println(local.toString());
                                    }
                                } catch (SSLPeerUnverifiedException ve) {
                                    throw new RuntimeException(ve);
                                }
                            } else {
                                System.out.println("No certificates received");
                            }
                            return "do nothing";
                        })
                .exceptionally(
                        throwable -> {
                            throwable.getCause().printStackTrace(System.err);
                            return "do nothing";
                        })
                .join();
    }
}
