package vn.edu.iuh.nhom01_www.user.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;
import vn.edu.iuh.nhom01_www.role.model.Role;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "staffs")
public class Staff extends BaseUser {

    @Column(unique = true, nullable = false)
    private String staffCode;

    private LocalDate hireDate;

    private String position;

    private String workStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ElementCollection
    @CollectionTable(name = "staff_permissions", joinColumns = @JoinColumn(name = "staff_id"))
    @Column(name = "permission")
    private List<String> permissions;
}
