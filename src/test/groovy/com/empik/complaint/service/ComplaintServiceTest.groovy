package com.empik.complaint.service

import com.empik.complaint.api.dto.ComplaintCreateRequest
import com.empik.complaint.client.GeoLocationClient
import com.empik.complaint.model.Complaint
import com.empik.complaint.repository.ComplaintRepository
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification

import java.time.LocalDateTime

import static java.util.Objects.nonNull

class ComplaintServiceTest extends Specification {

    ComplaintRepository complaintRepository
    GeoLocationClient geoLocationClient
    ComplaintService complaintService

    def setup() {
        complaintRepository = Mock(ComplaintRepository)
        geoLocationClient = Mock(GeoLocationClient)
        complaintService = new ComplaintService(complaintRepository, geoLocationClient)
    }

    def "should increment counter when complaint with same product and complainant exists"() {
        given:
        def request = new ComplaintCreateRequest("product-123", "New content that should be ignored", "customer-456")
        def ipAddress = "192.168.1.1"

        def existingComplaint = Complaint.builder()
                .id("existing-id")
                .productId("product-123")
                .content("Original content")
                .complainantId("customer-456")
                .country("Poland")
                .counter(1)
                .build()


        1 * complaintRepository.findByProductIdAndComplainantId("product-123", "customer-456") >> Mono.just(existingComplaint)

        1 * complaintRepository.save(_ as Complaint) >> { Complaint savedComplaint ->
            Mono.just(savedComplaint)
        }

        0 * geoLocationClient.getCountryFromIp(_)

        when:
        def result = complaintService.createComplaint(request, ipAddress)

        then:
        StepVerifier.create(result)
                .assertNext({ complaint ->
                    assert complaint.productId == "product-123"
                    assert complaint.content == "Original content"
                    assert complaint.complainantId == "customer-456"
                    assert complaint.country == "Poland"
                    assert complaint.counter == 2
                    assert complaint.id == "existing-id"
                })
                .verifyComplete()
    }

    def "should create a new complaint when product and complainant combination is new"() {
        given:
        def request = new ComplaintCreateRequest("product-123", "Product is broken", "customer-456")
        def ipAddress = "192.168.1.1"
        1 *  complaintRepository.findByProductIdAndComplainantId("product-123", "customer-456") >> Mono.empty()
        1 *   geoLocationClient.getCountryFromIp(ipAddress) >> Mono.just("Poland")
        1 *  complaintRepository.save(_ as Complaint) >> { Complaint complaint ->
            complaint.id = "generated-id"
            return Mono.just(complaint)
        }

        when:
        def result = complaintService.createComplaint(request, ipAddress)

        then:

        StepVerifier.create(result)
                .assertNext({ complaint ->
                    assert complaint.productId == "product-123"
                    assert complaint.content == "Product is broken"
                    assert complaint.complainantId == "customer-456"
                    assert complaint.country == "Poland"
                    assert complaint.counter == 1
                    assert complaint.id == "generated-id"
                })
                .verifyComplete()
    }


    def "should update complaint content"() {
        given:
        def complaintId = "complaint-id"
        def newContent = "Updated content"
        def ipAddress = "192.168.1.1"

        def existingComplaint = Complaint.builder()
                .id(complaintId)
                .productId("product-123")
                .content("Original content")
                .complainantId("customer-456")
                .country("Poland")
                .counter(1)
                .build()

        1 *   complaintRepository.findById(_ as String) >> { String id ->
            if (id == complaintId) return Mono.just(existingComplaint)
            return Mono.empty()
        }
        1 *  complaintRepository.save(_ as Complaint) >> { Complaint complaint -> Mono.just(complaint) }

        when:
        def result = complaintService.updateComplaintContent(complaintId, newContent, ipAddress)

        then:
        StepVerifier.create(result)
                .assertNext({ complaint ->
                    assert complaint.content == "Updated content"
                    assert nonNull(complaint.updateDate)
                })
                .verifyComplete()
    }

    def "should throw error when complaint not found by id"() {
        given:
        def complaintId = "non-existent-id"

        1 *  complaintRepository.findById(_ as String) >> { String id ->
            return Mono.empty()
        }

        when:
        def result = complaintService.getComplaintById(complaintId)

        then:
        StepVerifier.create(result)
                .expectErrorMatches({ error ->
                    error instanceof RuntimeException &&
                            error.message.contains("Complaint not found with ID: ${complaintId}")
                })
                .verify()
    }

    def "should enrich country if unknown when updating content"() {
        given:
        def complaintId = "complaint-id"
        def newContent = "Updated content"
        def ipAddress = "192.168.1.1"

        def existingComplaint = Complaint.builder()
                .id(complaintId)
                .productId("product-123")
                .content("Original content")
                .complainantId("customer-456")
                .country("Unknown")
                .counter(1)
                .build()

        1 *   complaintRepository.findById(_ as String) >> { String id ->
            if (id == complaintId) return Mono.just(existingComplaint)
            return Mono.empty()
        }
        1 *   geoLocationClient.getCountryFromIp(_ as String) >> { String ip ->
            if (ip == ipAddress) return Mono.just("Poland")
            return Mono.just("Unknown")
        }
        1 *  complaintRepository.save(_ as Complaint) >> { Complaint complaint -> Mono.just(complaint) }

        when:
        def result = complaintService.updateComplaintContent(complaintId, newContent, ipAddress)

        then:
        StepVerifier.create(result)
                .assertNext({ complaint ->
                    assert complaint.content == "Updated content"
                    assert complaint.country == "Poland"
                    assert nonNull(complaint.updateDate)
                })
                .verifyComplete()
    }

    def "should not attempt to update country if already known"() {
        given:
        def complaintId = "complaint-id"
        def newContent = "Updated content"
        def ipAddress = "192.168.1.1"

        def existingComplaint = Complaint.builder()
                .id(complaintId)
                .productId("product-123")
                .content("Original content")
                .complainantId("customer-456")
                .country("Germany")
                .counter(1)
                .build()

        1 * complaintRepository.findById(complaintId) >> Mono.just(existingComplaint)
        0 * geoLocationClient.getCountryFromIp(_)
        1 * complaintRepository.save(_ as Complaint) >> { Complaint complaint -> Mono.just(complaint) }

        when:
        def result = complaintService.updateComplaintContent(complaintId, newContent, ipAddress)

        then:

        StepVerifier.create(result)
                .assertNext({ complaint ->
                    assert complaint.content == "Updated content"
                    assert complaint.country == "Germany"
                })
                .verifyComplete()
    }

    def "should get complaint by id"() {
        given:
        def complaintId = "complaint-id"

        def existingComplaint = Complaint.builder()
                .id(complaintId)
                .productId("product-123")
                .content("Test content")
                .complainantId("customer-456")
                .country("Poland")
                .counter(1)
                .build()

        when:
        def result = complaintService.getComplaintById(complaintId)

        then:
        1 * complaintRepository.findById(complaintId) >> Mono.just(existingComplaint)

        StepVerifier.create(result)
                .assertNext({ complaint ->
                    assert complaint.id == complaintId
                    assert complaint.productId == "product-123"
                    assert complaint.content == "Test content"
                })
                .verifyComplete()
    }

    def "should get complaints with filters"() {
        given:
        def productId = "product-123"
        def complainantId = "customer-456"
        def fromDate = LocalDateTime.now().minusDays(7)
        def toDate = LocalDateTime.now()
        def page = 0
        def size = 10

        def complaint1 = Complaint.builder()
                .id("id-1")
                .productId(productId)
                .content("Content 1")
                .complainantId(complainantId)
                .build()

        when:
        def result = complaintService.getComplaints(productId, complainantId, fromDate, toDate, page, size)

        then:
        1 * complaintRepository.findByFilters(productId, complainantId, fromDate, toDate, page, size) >>
                Flux.just(complaint1)

        StepVerifier.create(result.collectList())
                .assertNext({ complaints ->
                    assert complaints.size() == 1
                    assert complaints[0].id == "id-1"
                    assert complaints[0].productId == productId
                    assert complaints[0].complainantId == complainantId
                })
                .verifyComplete()
    }

    def "should handle multiple complaints when retrieving with filters"() {
        given:
        def productId = "product-123"
        def fromDate = LocalDateTime.now().minusDays(7)
        def toDate = LocalDateTime.now()
        def page = 0
        def size = 10

        def complaint1 = Complaint.builder()
                .id("id-1")
                .productId(productId)
                .content("Content 1")
                .complainantId("customer-456")
                .build()

        def complaint2 = Complaint.builder()
                .id("id-2")
                .productId(productId)
                .content("Content 2")
                .complainantId("customer-789")
                .build()

        when:
        def result = complaintService.getComplaints(productId, null, fromDate, toDate, page, size)

        then:
        1 * complaintRepository.findByFilters(productId, null, fromDate, toDate, page, size) >>
                Flux.just(complaint1, complaint2)

        StepVerifier.create(result.collectList())
                .assertNext({ complaints ->
                    assert complaints.size() == 2
                    assert complaints[0].id == "id-1"
                    assert complaints[1].id == "id-2"
                    assert complaints[0].complainantId == "customer-456"
                    assert complaints[1].complainantId == "customer-789"
                })
                .verifyComplete()
    }
}
