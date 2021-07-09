package com.matthey.pmm.metal.transfers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.matthey.pmm.metal.transfers.EndurConnector;
import com.matthey.pmm.metal.transfers.Run;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndurConnectorTest {

    private static final String baseUrl = "https://test.com:8080";
    @Mock
    private RestTemplate restTemplate;
    private EndurConnector sut;

    @BeforeEach
    void setUp() {
        sut = new EndurConnector(restTemplate, baseUrl);
    }

    @Test
    void get_data_from_endur() {
        when(restTemplate.getForObject(anyString(),
                                       eq(TestClass.class),
                                       any(Object.class))).thenReturn(new TestClass());
        sut.get("/get_object", TestClass.class, "Param1", "Param2");
        verify(restTemplate, times(1)).getForObject("https://test.com:8080/get_object",
                                                    TestClass.class,
                                                    "Param1",
                                                    "Param2");
    }

    @Test
    void throw_exception_when_get_data_return_null() {
        assertThatThrownBy(() -> sut.get("/get_object", TestClass.class, "Param1", "Param2")).isInstanceOf(
                NullPointerException.class);
    }

    @Test
    void update_data_to_endur() {
        var request = new TestClass();
        sut.put("/put_object", request, "Param3", "Param4");
        verify(restTemplate, times(1)).put("https://test.com:8080/put_object", request, "Param3", "Param4");
    }

    @Test
    void add_data_to_endur() {
        var request = new TestClass();
        sut.post("/post_object", request, "Param");
        verify(restTemplate, times(1)).postForLocation("https://test.com:8080/post_object", request, "Param");
    }

    @Test
    void save_runs() {
        var url = "/test_path?user={user}";
        List<Run> runs = List.of(mock(Run.class), mock(Run.class), mock(Run.class));
        var user = "test user";
        sut.saveRuns(url, runs, user);
        verify(restTemplate, times(1)).postForLocation("https://test.com:8080/test_path?user={user}", runs, user);
    }

    @Test
    void no_error_should_throw_during_save_runs() {
        doThrow(RuntimeException.class).when(restTemplate).postForLocation(anyString(), anyList(), anyString());
        sut.saveRuns("", List.of(), "");
    }

    private static class TestClass {
    }
}