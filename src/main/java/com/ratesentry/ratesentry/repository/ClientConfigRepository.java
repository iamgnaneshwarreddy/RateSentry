package com.ratesentry.ratesentry.repository;

import com.ratesentry.ratesentry.model.ClientConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientConfigRepository extends JpaRepository<ClientConfig, String> {
}