package com.ratesentry.ratesentry;

import com.ratesentry.ratesentry.model.ClientConfig;
import com.ratesentry.ratesentry.repository.ClientConfigRepository;
import com.ratesentry.ratesentry.service.ClientConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientConfigServiceTest {

    @Mock
    private ClientConfigRepository clientConfigRepository;

    @InjectMocks
    private ClientConfigService clientConfigService;

    @Test
    void registerClient_shouldSaveAndReturnConfig() {
        ClientConfig config = new ClientConfig();
        config.setClientId("test-client");
        config.setMaxRequests(10);
        config.setWindowSeconds(60);
        config.setAlgorithm("SLIDING_WINDOW");

        when(clientConfigRepository.save(config)).thenReturn(config);

        ClientConfig result = clientConfigService.registerClient(config);

        assertEquals("test-client", result.getClientId());
        assertEquals(10, result.getMaxRequests());
        verify(clientConfigRepository, times(1)).save(config);
    }

    @Test
    void getClient_shouldReturnConfig_whenExists() {
        ClientConfig config = new ClientConfig();
        config.setClientId("test-client");

        when(clientConfigRepository.findById("test-client")).thenReturn(Optional.of(config));

        ClientConfig result = clientConfigService.getClient("test-client");

        assertEquals("test-client", result.getClientId());
    }

    @Test
    void getClient_shouldThrowException_whenNotExists() {
        when(clientConfigRepository.findById("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            clientConfigService.getClient("unknown");
        });

        assertEquals("Client not found: unknown", ex.getMessage());
    }
}