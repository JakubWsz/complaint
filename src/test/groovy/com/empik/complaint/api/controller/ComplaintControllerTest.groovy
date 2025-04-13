package com.empik.complaint.api.controller

import com.empik.complaint.api.dto.ComplaintCreateRequest
import com.empik.complaint.api.dto.ComplaintFullResponse
import com.empik.complaint.api.dto.ComplaintResponse
import com.empik.complaint.client.GeoLocationClient
import com.empik.complaint.model.Complaint
import com.empik.complaint.repository.ComplaintRepository
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.time.LocalDateTime

@SpringBootTest
@Testcontainers
@AutoConfigureWebTestClient
class ComplaintControllerTest extends Specification {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:5.0.9"))

    private static final String TEST_IP_ADDRESS = "192.168.1.100"

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> {
            if (!mongoDBContainer.isRunning()) {
                mongoDBContainer.start()
            }
            return mongoDBContainer.getReplicaSetUrl()
        })
    }

    @Autowired
    WebTestClient webTestClient

    @Autowired
    ComplaintRepository complaintRepository

    @SpringBean
    GeoLocationClient geoLocationClient = Stub(GeoLocationClient)

    def setup() {
        complaintRepository.deleteAll().block()
        geoLocationClient.getCountryFromIp(_ as String) >> Mono.just("Poland")
    }

    def "should create a new complaint"() {
        given:
        def request = new ComplaintCreateRequest("product-123", "Product is broken", "customer-456")

        when:
        def response = webTestClient.post()
                .uri("/api/v1/complaints")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", TEST_IP_ADDRESS)
                .bodyValue(request)
                .exchange()

        then:
        response.expectStatus().isCreated()
                .expectBody(ComplaintResponse.class)
                .consumeWith { result ->
                    def body = result.getResponseBody()
                    assert body.productId() == "product-123"
                    assert body.content() == "Product is broken"
                    assert body.country() == "Poland"
                }

        and: "complaint should be saved in database"
        def complaint = complaintRepository.findByProductIdAndComplainantId("product-123", "customer-456").block()
        complaint != null
        complaint.counter == 1
    }

    def "should increment counter when creating a duplicate complaint"() {
        given:
        def existingComplaint = Complaint.builder()
                .productId("product-123")
                .content("Original content")
                .complainantId("customer-456")
                .country("Poland")
                .counter(1)
                .build()

        complaintRepository.save(existingComplaint).block()

        def request = new ComplaintCreateRequest("product-123", "New content that should be ignored", "customer-456")

        when:
        def response = webTestClient.post()
                .uri("/api/v1/complaints")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", TEST_IP_ADDRESS)
                .bodyValue(request)
                .exchange()

        then:
        response.expectStatus().isCreated()
                .expectBody(ComplaintResponse.class)
                .consumeWith { result ->
                    def body = result.getResponseBody()
                    assert body.productId() == "product-123"
                    assert body.content() == "Original content"
                }

        and: "counter should be incremented (check in database)"
        def updatedComplaint = complaintRepository.findByProductIdAndComplainantId("product-123", "customer-456").block()
        updatedComplaint.counter == 2
    }

    def "should update complaint content"() {
        given:
        def existingComplaint = Complaint.builder()
                .productId("product-123")
                .content("Original content")
                .complainantId("customer-456")
                .country("Poland")
                .counter(1)
                .build()

        def savedComplaint = complaintRepository.save(existingComplaint).block()

        when:
        def response = webTestClient.put()
                .uri("/api/v1/complaints/${savedComplaint.id}/content?content=Updated content")
                .header("X-Forwarded-For", TEST_IP_ADDRESS)
                .exchange()

        then:
        response.expectStatus().isOk()
                .expectBody(ComplaintResponse.class)
                .consumeWith { result ->
                    def body = result.getResponseBody()
                    assert body.productId() == "product-123"
                    assert body.content() == "Updated content"
                    assert body.updateDate() != null
                }
    }

    def "should get complaint by id"() {
        given:
        def existingComplaint = Complaint.builder()
                .productId("product-123")
                .content("Test content")
                .complainantId("customer-456")
                .country("Poland")
                .counter(1)
                .build()

        def savedComplaint = complaintRepository.save(existingComplaint).block()

        when:
        def response = webTestClient.get()
                .uri("/api/v1/complaints/${savedComplaint.id}")
                .exchange()

        then:
        response.expectStatus().isOk()
                .expectBody(ComplaintResponse.class)
                .consumeWith { result ->
                    def body = result.getResponseBody()
                    assert body.productId() == "product-123"
                    assert body.content() == "Test content"
                }
    }

    def "should get complaints with filters"() {
        given:
        def now = LocalDateTime.now()
        def complaint1 = Complaint.builder()
                .productId("product-123")
                .content("Content 1")
                .complainantId("customer-456")
                .creationDate(now.minusDays(5))
                .country("Poland")
                .counter(1)
                .build()

        def complaint2 = Complaint.builder()
                .productId("product-123")
                .content("Content 2")
                .complainantId("customer-789")
                .creationDate(now.minusDays(2))
                .country("Germany")
                .counter(1)
                .build()

        def complaint3 = Complaint.builder()
                .productId("product-456")
                .content("Content 3")
                .complainantId("customer-456")
                .creationDate(now.minusDays(1))
                .country("France")
                .counter(1)
                .build()

        complaintRepository.saveAll([complaint1, complaint2, complaint3]).blockLast()

        when: "filter by productId"
        def response1 = webTestClient.get()
                .uri("/api/v1/complaints?productId=product-123")
                .exchange()

        then:
        response1.expectStatus().isOk()
                .expectBodyList(ComplaintFullResponse.class)
                .hasSize(2)

        when: "filter by complainantId"
        def response2 = webTestClient.get()
                .uri("/api/v1/complaints?complainantId=customer-456")
                .exchange()

        then:
        response2.expectStatus().isOk()
                .expectBodyList(ComplaintFullResponse.class)
                .hasSize(2)

        when: "filter by productId and complainantId"
        def response3 = webTestClient.get()
                .uri("/api/v1/complaints?productId=product-123&complainantId=customer-456")
                .exchange()

        then:
        response3.expectStatus().isOk()
                .expectBodyList(ComplaintFullResponse.class)
                .hasSize(1)

        when: "filter by date range"
        def response4 = webTestClient.get()
                .uri { builder ->
                    builder.path("/api/v1/complaints")
                            .queryParam("fromDate", now.minusDays(3).toString())
                            .build()
                }
                .exchange()

        then:
        response4.expectStatus().isOk()
                .expectBodyList(ComplaintFullResponse.class)
                .hasSize(2)
                .consumeWith { result ->
                    def bodies = result.getResponseBody()
                    bodies.each { body ->
                        assert body.creationDate() >= now.minusDays(3)
                    }
                }
    }

    def "should return error when complaint not found"() {
        when:
        def response = webTestClient.get()
                .uri("/api/v1/complaints/non-existent-id")
                .exchange()

        then:
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath('$.status').isEqualTo(404)
                .jsonPath('$.error').isEqualTo("Not Found")
                .jsonPath('$.message').isEqualTo("Complaint not found with ID: non-existent-id")
                .jsonPath('$.exceptionType').isEqualTo("ComplaintNotFoundException")
    }
}