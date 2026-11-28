package vn.edu.iuh.nhom01_www.cancelrequest.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.controller.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.dto.CancellationDtos.PageResult;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.service.CancellationWorkflowService;

import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CancellationControllerTest {
    private MockMvc mvc;
    private CancellationWorkflowService service;
    @BeforeEach void setup() {
        service = mock(CancellationWorkflowService.class);
        mvc = MockMvcBuilders.standaloneSetup(new StaffCancellationController(service), new CustomerCancellationController(service), new ProfileCancellationController(service))
                .setControllerAdvice(new CancellationExceptionHandler()).build();
    }

    @Test void listsPendingByDefaultAndPreservesExplicitAllStatuses() throws Exception {
        when(service.staffList("", "PENDING", 0, 10)).thenReturn(new PageResult<>(List.of(), 0, 10, 0, 0));
        mvc.perform(get("/api/admin/cancel-requests")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray());
        mvc.perform(get("/api/admin/cancel-requests").param("status", "")).andExpect(status().isOk());
        verify(service).staffList("", "PENDING", 0, 10);
        verify(service).staffList("", "", 0, 10);
    }

    @Test void rejectsBlankReasonsAndMissingBookingId() throws Exception {
        mvc.perform(post("/api/admin/cancel-requests").contentType(MediaType.APPLICATION_JSON).content("{\"bookingId\":7,\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(post("/api/cancellation-workflow/requests").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Lý do\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void supportsTheExistingCustomerProfileWithoutChangingItsCode() throws Exception {
        mvc.perform(patch("/api/bookings/7/request-cancel")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(service).submitFromProfile(7L);
    }

    @Test void approvalRequiresVersionAndExplicitConfirmation() throws Exception {
        mvc.perform(patch("/api/admin/cancel-requests/42/approve").contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"confirm\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/cancel-requests/42/approve").contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":true}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void rejectionRequiresARealReason() throws Exception {
        mvc.perform(patch("/api/admin/cancel-requests/42/reject").contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/cancel-requests/42/reject").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"reason\":\"" + "A".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void rejectsMalformedIdAndJsonWithoutInternalErrors() throws Exception {
        mvc.perform(get("/api/admin/cancel-requests/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/cancel-requests").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
