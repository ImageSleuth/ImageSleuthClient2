/*
 * Copyright 2014 The Friedland Group, Inc.
 *
 * -----------------------------------------------------------------
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.imagesleuth.imagesleuthclient2;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thefriedlandgroup.XMLTools2.Test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;

/**
 *
 * @author Noah Friedland at The Friedland Group, Inc
 */
public class Getter {

    final static JsonFactory f = new JsonFactory();
    final static ObjectMapper mapper = new ObjectMapper();
    final String url;
    final ArrayList<Header> harray = new ArrayList<>();
    final CredentialsProvider credsProvider = new BasicCredentialsProvider();
    final RequestConfig requestConfig;

    public Getter(String url,
            String user,
            String password) {
        Test.testNull(url);
        Test.testNull(user);
        Test.testNull(password);

        this.url = url;
        harray.add(new BasicHeader("Authorization", "Basic " + Base64.encodeBase64String(
                (user + ":" + password).getBytes())));

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);
        credsProvider.setCredentials(AuthScope.ANY, credentials);
        requestConfig = RequestConfig.custom()
                .setSocketTimeout(50000)
                .setConnectTimeout(30000).build();
    }

    public void get(final String id, final File result) throws InterruptedException, IOException, URISyntaxException {

        Test.testNull(id);
        Test.testNull(result);
        final CountDownLatch latch = new CountDownLatch(1);
        
        try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
                .setDefaultHeaders(harray)
                .setDefaultCredentialsProvider(credsProvider)
                .setDefaultRequestConfig(requestConfig)
                .build()) {
            httpclient.start();
            URIBuilder builder = new URIBuilder();
            if (url.startsWith("https://")) {
                String baseURL = url.substring(8);
                builder.setScheme("https").setHost(baseURL).setPath("/api/v1/results")
                        .setParameter("id", id);
            } else if (url.startsWith("http://")) {
                String baseURL = url.substring(7);
                builder.setScheme("http").setHost(baseURL).setPath("/api/v1/results")
                        .setParameter("id", id);
            } else {
                builder.setScheme("http").setHost(url).setPath("/api/v1/results")
                        .setParameter("id", id);
            }
            URI uri = builder.build();
            final HttpGet request = new HttpGet(uri);
            httpclient.execute(request, new FutureCallback<HttpResponse>() {

                @Override
                public void completed(final HttpResponse response) {

                    int code = response.getStatusLine().getStatusCode();
                    latch.countDown();
                    if (code == 200) {
                        if (response.getEntity() != null) {
                            StringWriter writer = new StringWriter();
                            try {
                                IOUtils.copy(response.getEntity().getContent(), writer);
                                //System.out.println("result: " + writer.toString());
                                JsonNode json = mapper.readTree(writer.toString());
                                String s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                                try (FileWriter fw = new FileWriter(result)) {
                                    fw.write(s);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }

                @Override
                public void failed(final Exception ex) {
                    latch.countDown();
                    System.out.println(request.getRequestLine() + "->" + ex);
                }

                @Override
                public void cancelled() {
                    latch.countDown();
                    System.out.println(request.getRequestLine() + " cancelled");
                }

            });
            latch.await();
        }
        //System.out.println("Shutting down");
    }
}
