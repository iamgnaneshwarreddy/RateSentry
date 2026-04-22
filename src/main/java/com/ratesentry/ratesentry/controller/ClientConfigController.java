package com.ratesentry.ratesentry.controller;

import com.ratesentry.ratesentry.model.ClientConfig;
import com.ratesentry.ratesentry.service.ClientConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
public class ClientConfigController {

    @Autowired
    private ClientConfigService clientConfigService;

    @PostMapping
    public ResponseEntity<ClientConfig> registerClient(@RequestBody ClientConfig config) {
        return ResponseEntity.ok(clientConfigService.registerClient(config));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ClientConfig> getClient(@PathVariable String clientId) {
        return ResponseEntity.ok(clientConfigService.getClient(clientId));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ClientConfig> updateClient(@PathVariable String clientId, @RequestBody ClientConfig config) {
        config.setClientId(clientId);
        return ResponseEntity.ok(clientConfigService.registerClient(config));
    }
}