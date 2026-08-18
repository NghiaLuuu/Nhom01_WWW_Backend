package vn.edu.iuh.nhom01_www.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> {

}
