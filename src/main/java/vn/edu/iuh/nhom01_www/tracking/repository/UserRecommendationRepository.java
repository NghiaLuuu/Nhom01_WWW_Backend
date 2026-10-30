package vn.edu.iuh.nhom01_www.tracking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.tracking.model.UserRecommendation;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRecommendationRepository extends JpaRepository<UserRecommendation, Long> {

    List<UserRecommendation> findTop5ByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<UserRecommendation> findTop5ByAnonymousIdOrderByCreatedAtDesc(String anonymousId);

    List<UserRecommendation> findTop5ByIpAddressOrderByCreatedAtDesc(String ipAddress);

    void deleteByAnonymousId(String anonymousId);

    void deleteByCustomerId(UUID customerId);
}

