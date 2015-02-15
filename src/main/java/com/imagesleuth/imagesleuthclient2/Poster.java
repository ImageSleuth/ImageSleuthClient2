/*
 * Copyright 2014 The Friedland Group, Inc.
 *
 * -----------------------------------------------------------------
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.imagesleuth.imagesleuthclient2;

import com.thefriedlandgroup.XMLTools2.Test;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NByteArrayEntity;

/**
 *
 * @author Noah Friedland at The Friedland Group, Inc
 */
public class Poster {

    final ArrayList<Header> harray = new ArrayList<>();
    final String urlval;
    final CredentialsProvider credsProvider = new BasicCredentialsProvider();
    final RequestConfig requestConfig;

    public Poster(String url,
            String user,
            String password) {
        Test.testNull(url);
        Test.testNull(user);
        Test.testNull(password);

        //System.out.println("user: " + user + " password: " + password);

        harray.add(new BasicHeader("Authorization", "Basic " + Base64.encodeBase64String(
                (user + ":" + password).getBytes())));

        urlval = (!url.startsWith("http"))
                ? "http://" + url + "/api/v1/job"
                : url + "/api/v1/job";
        System.out.println("Poster: post url " + urlval);

        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(user, password);
        credsProvider.setCredentials(AuthScope.ANY, credentials);
        requestConfig = RequestConfig.custom()
                .setSocketTimeout(50000)
                .setConnectTimeout(30000).build();
    }

    public String Post(final File imgFile) throws IOException, ImageReadException, InterruptedException {

        final ArrayList<String> idArray = new ArrayList<>();

        final CountDownLatch latch = new CountDownLatch(1);

        try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
                .setDefaultHeaders(harray)
                .setDefaultCredentialsProvider(credsProvider)
                .setDefaultRequestConfig(requestConfig)
                .build()) {
            httpclient.start();
            final byte[] imageAsBytes = IOUtils.toByteArray(new FileInputStream(imgFile));
            final ImageInfo info = Imaging.getImageInfo(imageAsBytes);
            final HttpPost request = new HttpPost(urlval);
            String boundary = UUID.randomUUID().toString();
            HttpEntity mpEntity = MultipartEntityBuilder.create()
                    .setBoundary("-------------" + boundary)
                    .addBinaryBody("file", imageAsBytes, ContentType.create(info.getMimeType()), imgFile.getName())
                    .build();
            ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
            mpEntity.writeTo(baoStream);
            request.setHeader("Content-Type", "multipart/form-data;boundary=-------------" + boundary);
            //equest.setHeader("Content-Type", "multipart/form-data");                               
            NByteArrayEntity entity = new NByteArrayEntity(baoStream.toByteArray(), ContentType.MULTIPART_FORM_DATA);
            request.setEntity(entity);
            httpclient.execute(request, new FutureCallback<HttpResponse>() {

                @Override
                public void completed(final HttpResponse response) {
                    int code = response.getStatusLine().getStatusCode();
                    //System.out.println(" response code: " + code + " for image: " + imgFile.getName());
                    if (response.getEntity() != null && code == 202) {
                        StringWriter writer = new StringWriter();
                        try {
                            IOUtils.copy(response.getEntity().getContent(), writer);
                            idArray.add(writer.toString());
                            writer.close();
                            //System.out.println(" response id: " + id + " for image "+ img.getName()); 
                            latch.countDown();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        System.out.println(" response code: " + code + " for image: " + imgFile.getName()
                                + " reason " + response.getStatusLine().getReasonPhrase());
                    }
                }

                @Override
                public void failed(final Exception ex) {
                    System.out.println(request.getRequestLine() + imgFile.getName() + "->" + ex);
                    latch.countDown();
                }

                @Override
                public void cancelled() {
                    System.out.println(request.getRequestLine() + " cancelled");
                    latch.countDown();
                }

            });
            latch.await();
        }
        if (idArray.isEmpty()) {
            return null;
        } else {
            return idArray.get(0);
        }
    }
}
