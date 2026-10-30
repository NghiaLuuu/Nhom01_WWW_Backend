package vn.edu.iuh.nhom01_www.cancelrequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;

public interface CancelRequestRepository extends JpaRepository<CancelRequest, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<CancelRequest> {
    boolean existsByBooking_Id(Long bookingId);
    java.util.Optional<CancelRequest> findByIdAndBooking_Customer_Id(Long id, java.util.UUID customerId);
    java.util.List<CancelRequest> findByBooking_IdIn(java.util.Collection<Long> bookingIds);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from CancelRequest r where r.id = :id")
    java.util.Optional<CancelRequest> findForUpdateById(@org.springframework.data.repository.query.Param("id") Long id);

}
