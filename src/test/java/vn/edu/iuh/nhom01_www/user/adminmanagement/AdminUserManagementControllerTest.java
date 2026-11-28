package vn.edu.iuh.nhom01_www.user.adminmanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.iuh.nhom01_www.user.adminmanagement.controller.AdminUserManagementController;
import vn.edu.iuh.nhom01_www.user.adminmanagement.controller.UserManagementExceptionHandler;
import vn.edu.iuh.nhom01_www.user.adminmanagement.dto.UserManagementDtos.Options;
import vn.edu.iuh.nhom01_www.user.adminmanagement.dto.UserManagementDtos.UserAccount;
import vn.edu.iuh.nhom01_www.user.adminmanagement.service.AdminUserManagementService;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests JSON/validation/error contracts; role authorization is tested separately with real method security. */
class AdminUserManagementControllerTest {
    private AdminUserManagementService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AdminUserManagementService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AdminUserManagementController(service))
                .setControllerAdvice(new UserManagementExceptionHandler()).build();
    }

    @Test
    void returnsTheSharedSuccessEnvelope() throws Exception {
        when(service.options()).thenReturn(new Options(List.of(), List.of()));
        mvc.perform(get("/api/admin/user-management/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.permissions").isArray());
    }

    @Test
    void acceptsJavaRecordRequestsAndSerializesDatesWithoutExposingPasswords() throws Exception {
        UserAccount account = new UserAccount(UUID.randomUUID(), "Nhân viên mới", "new@example.com", null,
                LocalDate.of(1995, 1, 15), "ACTIVE", LocalDateTime.of(2026, 1, 1, 8, 0), null, 0L,
                "NV_001", LocalDate.of(2020, 1, 1), null, null, null, null, List.of(), List.of());
        when(service.createStaff(any())).thenReturn(account);
        mvc.perform(post("/api/admin/user-management/staff").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"details":{"fullName":"Nhân viên mới","email":"new@example.com","staffCode":"NV_001",
                                 "dateOfBirth":"1995-01-15","hireDate":"2020-01-01","roleId":2,"permissions":[]},
                                 "password":"SafePassword123!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dateOfBirth").value("1995-01-15"))
                .andExpect(jsonPath("$.data.hireDate").value("2020-01-01"))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SafePassword123!"))));
        verify(service).createStaff(any());
    }

    @Test
    void rejectsInvalidNestedStaffDetailsBeforeCallingService() throws Exception {
        mvc.perform(post("/api/admin/user-management/staff").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"details":{"fullName":"","email":"invalid","staffCode":"NV 01","roleId":2,"permissions":[]},
                                 "password":"short"}
                                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingVersionAndUnknownStatus() throws Exception {
        mvc.perform(patch("/api/admin/user-management/customers/00000000-0000-4000-8000-000000000001/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELETED\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(service);
    }

    @Test
    void returnsBadRequestForMalformedIdentifiers() throws Exception {
        mvc.perform(get("/api/admin/user-management/staff/not-a-uuid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(service);
    }

    @Test
    void returnsBadRequestForMalformedJsonWithoutEchoingItsContents() throws Exception {
        mvc.perform(post("/api/admin/user-management/staff").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Sensitive123!\","))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Sensitive123!"))));
        verifyNoInteractions(service);
    }
}
