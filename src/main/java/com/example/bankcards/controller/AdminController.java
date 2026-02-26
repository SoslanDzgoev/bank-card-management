package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.DepositRequest;
import com.example.bankcards.dto.UserResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin-only operations")
public class AdminController {

    private final CardService cardService;
    private final UserService userService;

    @GetMapping("/users")
    @Operation(summary = "Get all users")
    public Page<UserResponse> getAllUsers(@PageableDefault(size = 10) Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete user")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PostMapping("/cards")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new card for user")
    public CardResponse createCard(@Valid @RequestBody CreateCardRequest request) {
        return cardService.createCard(request);
    }

    @GetMapping("/cards")
    @Operation(summary = "Get all cards")
    public Page<CardResponse> getAllCards(@PageableDefault(size = 10) Pageable pageable) {
        return cardService.getAllCards(pageable);
    }

    @GetMapping("/cards/{id}")
    @Operation(summary = "Get card by ID")
    public CardResponse getCardById(@PathVariable Long id) {
        return cardService.getCardById(id);
    }

    @PostMapping("/cards/{id}/deposit")
    @Operation(summary = "Deposit funds to a card")
    public CardResponse deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest request) {
        return cardService.deposit(id, request.getAmount());
    }

    @PatchMapping("/cards/{id}/block")
    @Operation(summary = "Block a card")
    public CardResponse blockCard(@PathVariable Long id) {
        return cardService.blockCard(id);
    }

    @PatchMapping("/cards/{id}/activate")
    @Operation(summary = "Activate a card")
    public CardResponse activateCard(@PathVariable Long id) {
        return cardService.activateCard(id);
    }

    @DeleteMapping("/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a card")
    public void deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
    }
}
