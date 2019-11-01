# TLS-Client-Auth

This repo is about using Java 11 features to implement a client that agrees on a Client Certificate Authentication when requesting a service.
Client Certificate Authentication is a mutual certificate based authentication, where the client provides its Client Certificate to the Server to prove its identity.

To test the flow, we need to create couple of certificates.
Lets use openssl to create them in the certs directory.


# Creating a private certificate authority

```
openssl req -newkey rsa:2048 -nodes -keyout certs/appRootCA.key -x509 -days 365 -out certs/appRootCA.pem -subj "/C=NO/ST=Oslo/L=Oslo/O=Senthadev DEV/OU=Java Dept/CN=ca.senthadev.com"
```

root CA private key = appRootCA.key
(Since its for testing, I'm storing the private key in the repo. Please store the real keys safely)
root CA public key = appRootCA.pem

# Creating a server certificate (api.senthadev.com) and signing with our root CA

```
# mkdir -p certs/server

openssl genrsa -out certs/server/server.key 2048
openssl req -new -key certs/server/server.key -out certs/server/server.csr -subj "/C=NO/ST=Oslo/L=Oslo/O=Senthadev DEV/OU=Java API Service/CN=api.senthadev.com"
openssl x509 -req -in certs/server/server.csr -CA certs/appRootCA.pem -CAkey certs/appRootCA.key -CAcreateserial -out certs/server/server.pem -days 365 -sha256

# openssl x509 -in certs/server/server.pem -noout -text
# Issuer: C=NO, ST=Oslo, L=Oslo, O=Senthadev DEV, OU=Java Dept, CN=ca.senthadev.com
# Subject: C=NO, ST=Oslo, L=Oslo, O=Senthadev DEV, OU=Java API Service, CN=api.senthadev.com

```

If we want to create a certificate with 'Subject Alternative name' to support multiple domains, please refer this [Openssl-csr-with-subjectAltName](https://www.endpoint.com/blog/2014/10/30/openssl-csr-with-alternative-names-one)

# Creating a client certificate (client1.senthadev.com)

```
# mkdir -p certs/client1

openssl genrsa -out certs/client1/client1.key 2048
openssl req -new -key certs/client1/client1.key -out certs/client1/client1.csr -subj "/C=NO/ST=Oslo/L=Oslo/O=Senthadev DEV/OU=Clients Dept/CN=client1.senthadev.com"
openssl x509 -req -in certs/client1/client1.csr -CA certs/appRootCA.pem -CAkey certs/appRootCA.key -CAcreateserial -out certs/client1/client1.pem -days 365 -sha256

# Do note that I'm missing the extended key usage options here. I will update them soon.

# openssl x509 -in certs/client1/client1.pem -noout -text
# Issuer: C=NO, ST=Oslo, L=Oslo, O=Senthadev DEV, OU=Java Dept, CN=ca.senthadev.com
# Subject: C=NO, ST=Oslo, L=Oslo, O=Senthadev DEV, OU=Clients Dept, CN=client1.senthadev.com

```

Certificates are created.
To use them in Java application, have to import them into the keyStore files.
Lets do it.

# Creating a TrustStore (apptruststore.jks) to store the our private root certificates

```
keytool -import -file certs/appRootCA.pem -alias appRootCA -keystore certs/client1/apptruststore.jks

# Password = test123
#
# Enter keystore password:
# Re-enter new password:
# Owner: CN=ca.senthadev.com, OU=Java Dept, O=Senthadev DEV, L=Oslo, ST=Oslo, C=NO
# Issuer: CN=ca.senthadev.com, OU=Java Dept, O=Senthadev DEV, L=Oslo, ST=Oslo, C=NO
# Serial number: c93a78fe843a019a
# Valid from: Thu Oct 31 23:26:47 CET 2019 until: Fri Oct 30 23:26:47 CET 2020
# Certificate fingerprints:
#	 SHA1: 88:DB:3C:AC:AF:69:82:D3:12:18:61:C6:16:62:D9:2F:37:D0:14:11
#	 SHA256: 04:41:20:25:88:E3:10:CD:51:AF:65:F0:0E:C4:8D:A4:20:EB:52:2A:EE:8E:B1:39:7D:D0:F3:09:5B:2D:A4:C4
# Signature algorithm name: SHA256withRSA
# Subject Public Key Algorithm: 2048-bit RSA key
# Version: 1
# Trust this certificate? [no]:  Y
# Certificate was added to keystore

```

# Creating a KeyStore (client1keystore.jks) to store the our client1.key

```
# Creating an pkcs12 file bundle

openssl pkcs12 -export -in certs/client1/client1.pem -inkey certs/client1/client1.key -out certs/client1/client1keystore.p12

# import client1keystore.p1 to the keystore
keytool -importkeystore -srckeystore certs/client1/client1keystore.p12 -srcstoretype pkcs12 -destkeystore certs/client1/client1keystore.jks -deststoretype pkcs12

# output
#
# Importing keystore certs/client1/client1keystore.p12 to certs/client1/client1keystore.jks...
# Enter destination keystore password: (test123)
# Re-enter new password:
# Enter source keystore password: (test)
# Entry for alias 1 successfully imported.
# Import command completed:  1 entries successfully imported, 0 entries failed or cancelled
```

That's it.
We have what needed to test the flow


# Start a server with generated server keys


```
# we are using openssl s_server to launch a server

openssl s_server -key certs/server/server.key -cert certs/server/server.pem -CAfile certs/appRootCA.pem -accept 18443 -www -Verify 3

# Since our server is running as api.senthadev.com, lets add an entry in the /etc/hosts file as
# 192.168.1.5 api.senthadev.com
# Lets test it via openssl s_client tool itself

openssl s_client -cert certs/client1/client1.pem -key certs/client1/client1.key -CAfile certs/appRootCA.pem -connect api.senthadev.com:18443

# Output
#
# Verify return code: 0 (ok)
```

Lets test it with Java

```
Test method:

com.senthadev.upstreams.TestClient/testGetoutput

This should print certificates along with
Verify return code: 0 (ok)

```

