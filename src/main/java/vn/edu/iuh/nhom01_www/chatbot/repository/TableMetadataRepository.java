package vn.edu.iuh.nhom01_www.chatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.chatbot.model.TableMetadata;

@Repository
public interface TableMetadataRepository extends JpaRepository<TableMetadata, Long> {
}
