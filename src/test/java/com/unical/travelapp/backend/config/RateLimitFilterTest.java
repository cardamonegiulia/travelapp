package com.unical.travelapp.backend.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

// Prova il rate limiting per IP (richieste anonime): dopo aver esaurito la capacita' del
// bucket, la richiesta successiva riceve 429 con Retry-After, senza raggiungere la catena
// di filtri successiva.
class RateLimitFilterTest {

    private RateLimitFilter filterConCapacita(int capacity) {
        RateLimitFilter filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "authenticatedCapacity", capacity);
        ReflectionTestUtils.setField(filter, "anonymousCapacity", capacity);
        return filter;
    }

    @Test
    void permetteRichiesteEntroLaCapacita() throws Exception {
        RateLimitFilter filter = filterConCapacita(2);
        FilterChain chain = Mockito.mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rifiutaConTropoTroppePerLoStessoIpConCode429EHeaderRetryAfter() throws Exception {
        RateLimitFilter filter = filterConCapacita(1);
        FilterChain chain = Mockito.mock(FilterChain.class);

        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        filter.doFilter(request1, response1, chain);

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        filter.doFilter(request2, response2, chain);

        assertThat(response2.getStatus()).isEqualTo(429);
        assertThat(response2.getHeader("Retry-After")).isNotNull();
        assertThat(response2.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        verify(chain).doFilter(request1, response1);
        verifyNoMoreInteractions(chain);
    }

    @Test
    void ipDiversiHannoBucketIndipendenti() throws Exception {
        RateLimitFilter filter = filterConCapacita(1);
        FilterChain chain = Mockito.mock(FilterChain.class);

        MockHttpServletRequest reqIpA = new MockHttpServletRequest();
        reqIpA.setRemoteAddr("10.0.0.10");
        MockHttpServletResponse respIpA = new MockHttpServletResponse();
        filter.doFilter(reqIpA, respIpA, chain);

        MockHttpServletRequest reqIpB = new MockHttpServletRequest();
        reqIpB.setRemoteAddr("10.0.0.11");
        MockHttpServletResponse respIpB = new MockHttpServletResponse();
        filter.doFilter(reqIpB, respIpB, chain);

        assertThat(respIpA.getStatus()).isEqualTo(200);
        assertThat(respIpB.getStatus()).isEqualTo(200);
    }
}
