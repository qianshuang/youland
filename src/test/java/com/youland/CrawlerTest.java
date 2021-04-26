package com.youland;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author qs
 */
public class CrawlerTest {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerTest.class);

    private String getContentFormUrl(String url) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().disableAutomaticRetries().build();
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet(url);
            response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                return EntityUtils.toString(responseEntity);
            }
        } catch (Exception e) {
            logger.error("send http request failed...", e);
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    logger.error("close httpClient failed...", e);
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.error("close response failed...", e);
                }
            }
        }
        return null;
    }

    @Test
    public void testGetContentFormUrl() {
        System.out.println(getContentFormUrl("http://www.baidu.com/"));
    }
}
