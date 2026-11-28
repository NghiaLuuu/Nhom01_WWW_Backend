package vn.edu.iuh.nhom01_www.cancelrequest.workflow.model;

import jakarta.persistence.*;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;

import java.time.LocalDateTime;

/** Supplemental data; the existing CancelRequest entity/table is not changed. */
@Entity
@Table(name = "cancel_request_workflow_details")
public class CancelWorkflowDetails {
    @Id
    @Column(name = "cancel_request_id")
    private Long requestId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancel_request_id")
    private CancelRequest request;

    @Column(name = "request_reason", columnDefinition = "TEXT")
    private String requestReason;

    @Column(name = "submitted_by_email", length = 255)
    private String submittedByEmail;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "decision_note", columnDefinition = "TEXT")
    private String decisionNote;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CancelWorkflowDetails() {
    }

    public CancelWorkflowDetails(CancelRequest request, String reason, String submittedByEmail, String source, LocalDateTime submittedAt) {
        this.request = request;
        this.requestId = request.getId();
        this.requestReason = reason;
        this.submittedByEmail = submittedByEmail;
        this.source = source;
        this.submittedAt = submittedAt;
    }

    public void recordDecision(String note, LocalDateTime processedAt) {
        this.decisionNote = note;
        this.processedAt = processedAt;
    }

    public Long getRequestId() { return requestId; }
    public String getRequestReason() { return requestReason; }
    public String getSource() { return source; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getDecisionNote() { return decisionNote; }
    public Long getVersion() { return version; }
}
