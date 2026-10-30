package vn.edu.iuh.nhom01_www.recommendation.service;

import vn.edu.iuh.nhom01_www.recommendation.model.PersonaType;
import vn.edu.iuh.nhom01_www.tracking.dto.RecommendedTripDto;

import java.util.List;
import java.util.UUID;

public interface RecommendationService {

    List<RecommendedTripDto> getPersonalizedRecommendations(String anonymousId, UUID customerId, String ipAddress, String currentCity, int limit);

    default List<RecommendedTripDto> getPersonalizedRecommendations(String anonymousId, UUID customerId, String ipAddress, int limit) {
        return getPersonalizedRecommendations(anonymousId, customerId, ipAddress, null, limit);
    }

    PersonaType inferUserPersona(String anonymousId, UUID customerId, String ipAddress);

    void refreshRecommendationsForActiveUsers();

    void invalidateAndRefreshUser(String anonymousId, UUID customerId, String ipAddress);
}
