/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.scim2.testsuite.core.httpclient;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.wso2.scim2.testsuite.core.exception.ComplianceException;
import org.wso2.scim2.testsuite.core.protocol.ComplianceTestMetaDataHolder;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HostnameVerifier;

/**
 * This class is to depicts the HTTP client.
 */
public class HTTPClient {

    private static CloseableHttpClient httpClient = null;

    public static HttpClient getHttpClient() throws ComplianceException {

        if (httpClient == null) {
            TrustStrategy trustAllStrategy = (chain, authType) -> true;
            SSLContextBuilder builder = new SSLContextBuilder();
            try {
                builder.loadTrustMaterial(trustAllStrategy);
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                throw new ComplianceException("Error in setting up the http client");
            }

            SSLConnectionSocketFactory ssl;
            try {
                HostnameVerifier allHostsValid = (hostname, session) -> true;
                ssl = new SSLConnectionSocketFactory(builder.build(), allHostsValid);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new ComplianceException("Error in setting up the http client");
            }
            httpClient = HttpClients.custom().setSSLSocketFactory(ssl).build();
            return httpClient;
        }
        return httpClient;
    }

    public static HttpRequestBase setAuthorizationHeader(ComplianceTestMetaDataHolder complianceTestMetaDataHolder,
                                                         HttpRequestBase method) {

        String auth = complianceTestMetaDataHolder.getUsername() + ":" + complianceTestMetaDataHolder.getPassword();
        if (!auth.equals(":")) {
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);
            method.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }
        if (!complianceTestMetaDataHolder.getAuthorization_header().equals("")) {
            String authHeader = "Bearer " + complianceTestMetaDataHolder.getAuthorization_header();
            method.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return method;
    }

    public static HttpRequestBase setAuthorizationHeader(String userName, String password,
                                                         HttpRequestBase method) {

        String auth = userName + ":" + password;
        if (!auth.equals(":")) {
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);
            method.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return method;
    }

}


