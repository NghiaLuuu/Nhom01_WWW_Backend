package vn.edu.iuh.nhom01_www.cancelrequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;

@Repository
public interface CancelRequestRepository extends JpaRepository<CancelRequest, Long> {

}
