package vn.edu.iuh.nhom01_www.chatbot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "table_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tableName;

    @Column(nullable = false)
    private String shortDescription;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String schemaDefinition;
}
