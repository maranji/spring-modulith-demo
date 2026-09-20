package com.example.app.web;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.marketdata.AvailableAssetsQuery;
import com.example.marketdata.RefreshMarketDataCommand;
import com.example.messaging.MessageBus;

/**
 * Elenco degli asset disponibili e ricaricamento manuale dei dati di prezzo
 * dal disco. Come {@link PriceStatisticsController}, comunica con il
 * modulo Market Data solo tramite messaggi ({@link AvailableAssetsQuery},
 * {@link RefreshMarketDataCommand}), mai attraverso una sua interfaccia di
 * servizio.
 */
@RestController
@RequestMapping("/api/assets")
class AssetsController {

    private final MessageBus messageBus;

    AssetsController(MessageBus messageBus) {
        this.messageBus = messageBus;
    }

    @GetMapping
    Set<String> list() {
        return messageBus.send(new AvailableAssetsQuery());
    }

    @PostMapping("/refresh")
    ResponseEntity<Void> refresh() {
        messageBus.send(new RefreshMarketDataCommand());
        return ResponseEntity.accepted().build();
    }
}
