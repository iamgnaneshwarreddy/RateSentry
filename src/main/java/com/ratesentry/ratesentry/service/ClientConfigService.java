package com.ratesentry.ratesentry.service;

import com.ratesentry.ratesentry.model.ClientConfig;
import com.ratesentry.ratesentry.repository.ClientConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientConfigService {

    @Autowired
    private ClientConfigRepository clientConfigRepository;

    public ClientConfig registerClient(ClientConfig config) {
        return clientConfigRepository.save(config);
    }

    public ClientConfig getClient(String clientId) {
        return clientConfigRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));
    }
}