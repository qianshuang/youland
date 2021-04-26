package com.youland;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * @author qs
 */
public class Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    // final result
    private static CopyOnWriteArrayList<String> coal = Lists.newCopyOnWriteArrayList();

    // user query
    private static String query = "youland";

    // should not have more than 20 HTTP request at any time
    private static Semaphore semaphore = new Semaphore(20);

    /**
     * get web content of the url
     * @param url
     * @return
     */
    private static String getContentFormUrl(String url) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().disableAutomaticRetries().build();
        CloseableHttpResponse response = null;
        try {
            semaphore.acquire();
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
        semaphore.release();
        return null;
    }

    /**
     * get valid and complete url
     * @param queryUrl
     * @param href
     * @return
     */
    private static String getCompleteUrl(String queryUrl, String href) {
        if (href.startsWith(queryUrl)) {
            return href;
        } else if (href.startsWith("/")) {
            return queryUrl + href.substring(1);
        } else {
            return null;
        }
    }

    /**
     * get all hrefs within the web content
     * @param queryUrl
     * @param content
     * @param urlInPage
     */
    private static void getHrefOfContent(String queryUrl, String content, Queue<String> urlInPage) {
        if (StringUtils.isNotBlank(content)) {
            String[] contents = content.split("<a href=\"");
            if (contents.length > 1) {
                for (int i = 1; i < contents.length; i++) {
                    int endHref = contents[i].indexOf("\"");
                    String aHref = getCompleteUrl(queryUrl, contents[i].substring(0, endHref));
                    if (StringUtils.isNotBlank(aHref) && !urlInPage.contains(aHref)) {
                        urlInPage.add(aHref);
                    }
                }
            }
        }
    }

    /**
     * main method
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        InputStream path = Crawler.class.getResourceAsStream("/web_crawler_url_list.txt");
        List<String> lines = IOUtils.readLines(path, "utf-8");
        List<String> oriUrls = Lists.newArrayList();
        for (String line : lines) {
            oriUrls.add(line.split(",")[1].trim().replaceAll("\"", ""));
        }
        CountDownLatch cdl = new CountDownLatch(oriUrls.size());
        for (int i = 0; i < oriUrls.size(); i++) {
            final String url = "http://www." + oriUrls.get(i);
            final Queue<String> urlInPage = Queues.newArrayDeque();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String content = getContentFormUrl(url);
                    if (StringUtils.isNotBlank(content) && content.contains(query)) {
                        coal.add(url);
                        logger.info(url);
                    }
                    getHrefOfContent(url, content, urlInPage);
                    while (!urlInPage.isEmpty()) {
                        String url_ = urlInPage.poll();
                        String content_ = getContentFormUrl(url_);
                        if (StringUtils.isNotBlank(content_) && content_.contains(query)) {
                            coal.add(url_);
                            logger.info(url_);
                        }
                        getHrefOfContent(url, content_, urlInPage);
                    }
                    cdl.countDown();
                }
            }).start();
        }
        cdl.await();
        FileUtils.writeLines(new File("/Users/wcy/Downloads/result.txt"), coal);
        System.out.println("Done...");
    }
}
