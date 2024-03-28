package project;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.*;

public class Test implements Runnable {

    private TimeUnit timeUnit;
    private int requestLimit;
    private Semaphore semaphore;

    public Test(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        if (requestLimit > 0) {
            this.requestLimit = requestLimit;
        } else {
            throw new IllegalArgumentException();
        }
        semaphore = new Semaphore(requestLimit);
    }

    @Override
    public void run() {
        try {
            semaphore.acquire();
            makeRequest();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
    }

    public void makeRequest() {
        HttpRequest request = null;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .POST(HttpRequest.BodyPublishers.ofFile(Path.of("C:\\Users\\GAMER\\Desktop\\JSON.txt")))
                    .build();
        } catch (URISyntaxException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(response.body());
    }


    public static void main(String[] args) throws InterruptedException {
        final int maxReq = 5;
        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        final long timeAmount = 1;
        // ExecutorService executorService = Executors.newFixedThreadPool(5);
        Test test = new Test(timeUnit, maxReq);
        for (int i = 0; i < 35; i++) {
            test.makeRequest();
        }
//        for (int i = 0; i < 25; i++) {
//            executorService.submit(new Test(q));
    }

}
