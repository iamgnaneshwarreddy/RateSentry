package com.ratesentry.ratesentry.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "client_configs")
public class ClientConfig {

    @Id
    private String clientId;

    private int maxRequests;
    private int windowSeconds;
    private String algorithm;
}