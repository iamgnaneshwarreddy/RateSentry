package com.ratesentry.ratesentry.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rate_limit_logs")
public class RateLimitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientId;
    private String endpoint;
    private boolean allowed;
    private String algorithm;
    private LocalDateTime timestamp;
}