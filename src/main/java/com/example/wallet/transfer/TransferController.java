package com.example.wallet.transfer;

import com.example.wallet.transfer.dto.CountResponse;
import com.example.wallet.transfer.dto.TransferByNamesRequest;
import com.example.wallet.transfer.dto.TransferRequest;
import com.example.wallet.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/transfers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TransferController {

    private final TransferService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(@Valid @RequestBody TransferRequest req) {
        return service.transfer(req);
    }

    @PostMapping(value = "/by-names", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse createByNames(@Valid @RequestBody TransferByNamesRequest req) {
        return service.transferByNames(req);
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    public TransferResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }

    @GetMapping("/count")
    public CountResponse count() {
        return service.count();
    }
}