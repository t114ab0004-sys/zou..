package com.crawler;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Java 網路爬蟲
 * 使用方式：修改 main() 裡的 startUrl 和 maxPages 即可執行
 */
public class WebCrawler {

    private final Set<String> visitedUrls = new HashSet<>();
    private final Queue<String> urlQueue   = new LinkedList<>();
    private final List<PageData> results   = new ArrayList<>();

    private final String targetDomain; // 限定爬同一網域
    private final int    maxPages;
    private static final Scanner sc = new Scanner(System.in);

    // ─── 設定區（在這裡修改目標網址與最大頁數）──────────────────────────
    public static void main(String[] args) {
        System.out.print("請輸入要爬的網址：");
        String startUrl = sc.nextLine(); // ← 改成你要爬的網址
        System.out.print("請輸入最大頁數：");
        int    maxPages = Integer.parseInt(sc.nextLine()); // ← 最多爬幾頁
        boolean saveCSV = true;                   // ← 是否儲存成 output.csv

        WebCrawler crawler = new WebCrawler(startUrl, maxPages);
        crawler.start();

        if (saveCSV) {
            crawler.saveToCSV("output.csv");
        }
    }
    // ────────────────────────────────────────────────────────────────────

    public WebCrawler(String startUrl, int maxPages) {
        this.maxPages = maxPages;
        urlQueue.add(startUrl);

        // 解析主網域（只爬同一網域的頁面）
        String domain = "";
        try {
            domain = new URL(startUrl).getHost();
        } catch (Exception e) {
            System.out.println("[警告] 無法解析網域，將爬取所有連結");
        }
        this.targetDomain = domain;
    }

    public void start() {
        System.out.println("=================================================");
        System.out.println("  Java 網路爬蟲啟動");
        System.out.println("  目標網域：" + targetDomain);
        System.out.println("  最大頁數：" + maxPages);
        System.out.println("=================================================\n");

        int count = 0;

        while (!urlQueue.isEmpty() && count < maxPages) {
            String url = urlQueue.poll();

            // 跳過已訪問
            if (visitedUrls.contains(url)) continue;

            // 只爬同一網域
            if (!isSameDomain(url)) continue;

            visitedUrls.add(url);

            try {
                System.out.printf("[%2d] 爬取中：%s%n", count + 1, url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (compatible; JavaCrawler/1.0)")
                        .timeout(8000)
                        .get();

                String title     = doc.title();
                String bodyText  = doc.body().text();
                int    wordCount = bodyText.split("\\s+").length;

                System.out.println("     標題：" + title);
                System.out.println("     字數：" + wordCount);

                // 收集結果
                results.add(new PageData(url, title, wordCount));

                // 擷取頁面所有連結並加入佇列
                Elements links = doc.select("a[href]");
                int newLinks = 0;
                for (Element link : links) {
                    String absUrl = link.absUrl("href");
                    if (!absUrl.isEmpty()
                            && absUrl.startsWith("http")
                            && !visitedUrls.contains(absUrl)
                            && isSameDomain(absUrl)) {
                        urlQueue.add(absUrl);
                        newLinks++;
                    }
                }
                System.out.println("     新連結：" + newLinks + " 個\n");

                count++;
                Thread.sleep(500); // 禮貌性延遲 0.5 秒

            } catch (IOException e) {
                System.out.println("     [錯誤] 無法連線：" + e.getMessage() + "\n");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("=================================================");
        System.out.printf("  完成！成功爬取 %d 頁%n", count);
        System.out.println("=================================================");
    }

    /** 儲存結果為 CSV 檔 */
    public void saveToCSV(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("url,title,wordCount");
            for (PageData page : results) {
                // 逗號、引號跳脫處理
                String safeTitle = "\"" + page.title.replace("\"", "\"\"") + "\"";
                String safeUrl   = "\"" + page.url.replace("\"", "\"\"") + "\"";
                writer.println(safeUrl + "," + safeTitle + "," + page.wordCount);
            }
            System.out.println("\n結果已儲存至：" + filename);
        } catch (IOException e) {
            System.out.println("[錯誤] 無法寫入 CSV：" + e.getMessage());
        }
    }

    /** 判斷是否同一網域 */
    private boolean isSameDomain(String url) {
        if (targetDomain.isEmpty()) return true;
        try {
            String host = new URL(url).getHost();
            return host.equals(targetDomain) || host.endsWith("." + targetDomain);
        } catch (Exception e) {
            return false;
        }
    }

    /** 頁面資料結構 */
    static class PageData {
        String url;
        String title;
        int    wordCount;

        PageData(String url, String title, int wordCount) {
            this.url       = url;
            this.title     = title;
            this.wordCount = wordCount;
        }
    }
}
