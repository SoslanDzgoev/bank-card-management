package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.SameCardTransferException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardEncryptionUtil encryptionUtil;

    @InjectMocks
    private CardService cardService;

    private User owner;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("user@bank.com")
                .password("encoded")
                .role(Role.ROLE_USER)
                .build();

        fromCard = Card.builder()
                .id(1L)
                .encryptedCardNumber("encrypted_1234")
                .owner(owner)
                .expiryDate(LocalDate.of(2028, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        toCard = Card.builder()
                .id(2L)
                .encryptedCardNumber("encrypted_5678")
                .owner(owner)
                .expiryDate(LocalDate.of(2027, 6, 30))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("0.00"))
                .build();
    }

    @Test
    @DisplayName("Успешный перевод между двумя картами")
    void transfer_success() {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("500.00"));

        when(cardRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(2L, owner)).thenReturn(Optional.of(toCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        cardService.transfer(request, owner);

        assertThat(fromCard.getBalance()).isEqualByComparingTo("500.00");
        assertThat(toCard.getBalance()).isEqualByComparingTo("500.00");
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    @DisplayName("Перевод при недостаточном балансе — ошибка")
    void transfer_insufficientFunds_throwsException() {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("2000.00"));

        when(cardRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(2L, owner)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> cardService.transfer(request, owner))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Недостаточно средств на карте");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод с заблокированной карты — ошибка")
    void transfer_blockedSourceCard_throwsException() {
        fromCard.setStatus(CardStatus.BLOCKED);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));

        when(cardRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(2L, owner)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> cardService.transfer(request, owner))
                .isInstanceOf(CardBlockedException.class)
                .hasMessage("Карта-отправитель не активна");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод на заблокированную карту — ошибка")
    void transfer_blockedTargetCard_throwsException() {
        toCard.setStatus(CardStatus.BLOCKED);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));

        when(cardRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(2L, owner)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> cardService.transfer(request, owner))
                .isInstanceOf(CardBlockedException.class)
                .hasMessage("Карта-получатель не активна");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод самому себе — ошибка")
    void transfer_sameCard_throwsException() {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(1L);
        request.setAmount(new BigDecimal("100.00"));

        assertThatThrownBy(() -> cardService.transfer(request, owner))
                .isInstanceOf(SameCardTransferException.class)
                .hasMessage("Нельзя переводить на ту же карту");

        verify(cardRepository, never()).findByIdAndOwner(any(), any());
    }

    @Test
    @DisplayName("Перевод чужой карты — ошибка 404")
    void transfer_cardNotFound_throwsException() {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(99L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));

        when(cardRepository.findByIdAndOwner(99L, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.transfer(request, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
