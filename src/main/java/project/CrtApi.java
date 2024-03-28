package project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

public class CrtApi {

    private volatile static CrtApi instance;
    private ScheduledExecutorService executorService;
    private BlockingQueue<Request> queue;

    private CrtApi() {
    }

    private CrtApi(TimeUnit timeUnit, int requestLimit) {
        this.queue = new ArrayBlockingQueue<>(requestLimit);
        this.executorService = Executors.newScheduledThreadPool(requestLimit);
        for (int i = 0; i < requestLimit; i++) {
            this.executorService.scheduleWithFixedDelay(new DocumentClient(queue), 0, 1, timeUnit);
        }
    }

    public static CrtApi getInstance(TimeUnit timeUnit, int requestLimit) {
        if (instance == null) {
            synchronized (CrtApi.class) {
                if (instance == null) {
                    instance = new CrtApi(timeUnit, requestLimit);
                }
            }
        }
        return instance;
    }

    public void execute(Request request) {
        try {
            queue.put(request);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private class DocumentClient implements Runnable {

        private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

        private final BlockingQueue<Request> queue;
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;

        private DocumentClient(BlockingQueue<Request> queue) {
            this.queue = queue;
            httpClient = HttpClient.newHttpClient();
            this.objectMapper = new ObjectMapper();
        }

        @Override
        public void run() {
            Request body;
            try {
                body = queue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            HttpRequest request;
            try {
                request = HttpRequest.newBuilder()
                        .uri(new URI(URL))
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                        .build();
            } catch (URISyntaxException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println(response.body());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Data
    @NoArgsConstructor
    public static class Request {

        private Description description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;
        @JsonProperty("doc_type")
        private String docType;
        private boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonDeserialize(using = LocalDateDeserializer.class)
        @JsonSerialize(using = LocalDateSerializer.class)
        @JsonProperty("production_date")
        private LocalDate productionDate;
        @JsonProperty("production_type")
        private String productionType;
        private List<Product> products;
        @JsonDeserialize(using = LocalDateDeserializer.class)
        @JsonSerialize(using = LocalDateSerializer.class)
        @JsonProperty("reg_date")
        private LocalDate regDate;
        @JsonProperty("reg_number")
        private String regNumber;

        @Data
        @NoArgsConstructor
        public static class Description {

            private String participantInn;
        }

        @Data
        @NoArgsConstructor
        public static class Product {

            @JsonProperty("certificate_document")
            private String certificateDocument;
            @JsonDeserialize(using = LocalDateDeserializer.class)
            @JsonSerialize(using = LocalDateSerializer.class)
            @JsonProperty("certificate_document_date")
            private LocalDate certificateDocumentDate;
            @JsonProperty("certificate_document_number")
            private String certificateDocumentNumber;
            @JsonProperty("owner_inn")
            private String ownerInn;
            @JsonProperty("producer_inn")
            private String producerInn;
            @JsonDeserialize(using = LocalDateDeserializer.class)
            @JsonSerialize(using = LocalDateSerializer.class)
            @JsonProperty("production_date")
            private LocalDate productionDate;
            @JsonProperty("tnved_code")
            private String tnvedCode;
            @JsonProperty("uit_code")
            private String uitCode;
            @JsonProperty("uitu_code")
            private String uituCode;
        }
    }

}


