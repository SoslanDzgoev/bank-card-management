package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cards", description = "User card operations")
public class CardController {

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "Get my cards with optional status filter and pagination")
    public Page<CardResponse> getMyCards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) CardStatus status,
            @PageableDefault Pageable pageable) {

        return cardService.getMyCards(userDetails.getUser(), status, pageable);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between my cards")
    public ResponseEntity<Void> transfer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TransferRequest request) {

        cardService.transfer(request, userDetails.getUser());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/block")
    @Operation(summary = "Request to block my card")
    public ResponseEntity<Void> requestBlock(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {

        cardService.requestBlock(id, userDetails.getUser());
        return ResponseEntity.ok().build();
    }
}
