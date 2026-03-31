package com.ruru.tokenapi.api;

import com.ruru.tokenapi.client.PartnerClientResponse;
import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.client.RegisterPartnerClientRequest;
import com.ruru.tokenapi.config.TokenApiProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin/partner-clients")
public class AdminPartnerClientController extends AdminProtectedController {
    private final PartnerClientService partnerClientService;

    public AdminPartnerClientController(PartnerClientService partnerClientService, TokenApiProperties properties) {
        super(properties);
        this.partnerClientService = partnerClientService;
    }

    @PostMapping
    public Map<String, Object> registerClient(@RequestBody RegisterPartnerClientRequest request) {
        var client = partnerClientService.register(request);
        return Map.of(
            "message", "client registered",
            "clientId", client.clientId(),
            "systemCode", client.systemCode(),
            "callSource", client.callSource(),
            "active", client.active()
        );
    }

    @GetMapping
    public List<PartnerClientResponse> listClients() {
        return partnerClientService.findAllClients().stream()
            .map(PartnerClientResponse::from)
            .toList();
    }

    @GetMapping("/{clientId}")
    public PartnerClientResponse getClient(@PathVariable String clientId) {
        var client = partnerClientService.findClient(clientId);
        if (client == null) {
            throw new ResponseStatusException(NOT_FOUND, "Client not found");
        }
        return PartnerClientResponse.from(client);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
