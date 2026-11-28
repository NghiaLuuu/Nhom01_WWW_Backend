package vn.edu.iuh.nhom01_www.cancelrequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;

public interface CancelRequestRepository extends JpaRepository<CancelRequest, Long> {

}
