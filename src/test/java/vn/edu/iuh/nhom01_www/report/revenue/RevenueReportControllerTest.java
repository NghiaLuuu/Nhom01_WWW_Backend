package vn.edu.iuh.nhom01_www.report.revenue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RevenueReportControllerTest {

    @BeforeEach
    void setUp() {
    }

    @Test
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
    }

    @Test
    }

    @Test
        verifyNoInteractions(service);
    }

    @Test
    }

    @Test
    }

    @Test
    }
}
