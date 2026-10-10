package org.maranji.app.web;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.maranji.actionrouter.ActionRouter;
import org.maranji.marketdata.actions.AvailableAssetsQuery;
import org.maranji.marketdata.actions.RefreshMarketDataCommand;

@RestController
@RequestMapping("/api/assets")
class AssetsController {

    private final ActionRouter actionRouter;

    AssetsController(ActionRouter actionRouter) {
        this.actionRouter = actionRouter;
    }

    @GetMapping
    Set<String> list() {
        return actionRouter.route(new AvailableAssetsQuery());
    }

    @PostMapping("/refresh")
    ResponseEntity<Void> refresh() {
        actionRouter.route(new RefreshMarketDataCommand());
        return ResponseEntity.accepted().build();
    }
}
