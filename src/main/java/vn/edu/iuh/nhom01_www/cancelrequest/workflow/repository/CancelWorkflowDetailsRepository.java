package vn.edu.iuh.nhom01_www.cancelrequest.workflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.model.CancelWorkflowDetails;

public interface CancelWorkflowDetailsRepository extends JpaRepository<CancelWorkflowDetails, Long> {
}
