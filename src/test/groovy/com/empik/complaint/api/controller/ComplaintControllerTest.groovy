package com.empik.complaint.api.controller

import com.empik.complaint.api.dto.ComplaintCreateRequest
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

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl)
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
                .bodyValue(request)
                .exchange()

        then:
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.productId').isEqualTo("product-123")
                .jsonPath('$.content').isEqualTo("Product is broken")
                .jsonPath('$.complainantId').isEqualTo("customer-456")
                .jsonPath('$.country').isEqualTo("Poland")
                .jsonPath('$.counter').isEqualTo(1)
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
                .bodyValue(request)
                .exchange()

        then:
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.productId').isEqualTo("product-123")
                .jsonPath('$.content').isEqualTo("Original content") // Content should not change
                .jsonPath('$.complainantId').isEqualTo("customer-456")
                .jsonPath('$.counter').isEqualTo(2) // Counter should be incremented
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
                .exchange()

        then:
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.productId').isEqualTo("product-123")
                .jsonPath('$.content').isEqualTo("Updated content")
                .jsonPath('$.complainantId').isEqualTo("customer-456")
                .jsonPath('$.updateDate').isNotEmpty()
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
                .expectBody()
                .jsonPath('$.productId').isEqualTo("product-123")
                .jsonPath('$.content').isEqualTo("Test content")
                .jsonPath('$.complainantId').isEqualTo("customer-456")
    }

    def "should get complaints with filters"() {
        given:def now = LocalDateTime.now()
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
                .expectBodyList()
                .hasSize(2)

        when: "filter by complainantId"
        def response2 = webTestClient.get()
                .uri("/api/v1/complaints?complainantId=customer-456")
                .exchange()

        then:
        response2.expectStatus().isOk()
                .expectBodyList()
                .hasSize(2)

        when: "filter by productId and complainantId"
        def response3 = webTestClient.get()
                .uri("/api/v1/complaints?productId=product-123&complainantId=customer-456")
                .exchange()

        then:
        response3.expectStatus().isOk()
                .expectBodyList()
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
                .expectBodyList()
                .hasSize(2)
    }

    def "should return error when complaint not found"() {
        when:
        def response = webTestClient.get()
                .uri("/api/v1/complaints/non-existent-id")
                .exchange()

        then:
        response.expectStatus().is5xxServerError()
    }
}
