package vn.edu.iuh.nhom01_www.route.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.route.model.Route;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

}
