package vn.edu.iuh.nhom01_www.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

}
