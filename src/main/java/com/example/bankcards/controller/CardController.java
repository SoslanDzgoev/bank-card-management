package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cards", description = "User card operations")
public class CardController {

    private final CardService cardService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get my cards with optional status filter and pagination")
    public Page<CardResponse> getMyCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) CardStatus status,
            @PageableDefault(size = 10) Pageable pageable) {

        User currentUser = userService.getEntityByEmail(userDetails.getUsername());
        return cardService.getMyCards(currentUser, status, pageable);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between my cards")
    public ResponseEntity<Void> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request) {

        User currentUser = userService.getEntityByEmail(userDetails.getUsername());
        cardService.transfer(request, currentUser);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/block")
    @Operation(summary = "Request to block my card")
    public ResponseEntity<Void> requestBlock(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        User currentUser = userService.getEntityByEmail(userDetails.getUsername());
        cardService.requestBlock(id, currentUser);
        return ResponseEntity.ok().build();
    }
}
