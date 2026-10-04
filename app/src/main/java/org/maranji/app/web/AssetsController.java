package org.maranji.app.web;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.maranji.contractbus.ContractBus;
import org.maranji.marketdata.contracts.AvailableAssetsQuery;
import org.maranji.marketdata.contracts.RefreshMarketDataCommand;

@RestController
@RequestMapping("/api/assets")
class AssetsController {

    private final ContractBus contractBus;

    AssetsController(ContractBus contractBus) {
        this.contractBus = contractBus;
    }

    @GetMapping
    Set<String> list() {
        return contractBus.send(new AvailableAssetsQuery());
    }

    @PostMapping("/refresh")
    ResponseEntity<Void> refresh() {
        contractBus.send(new RefreshMarketDataCommand());
        return ResponseEntity.accepted().build();
    }
}
