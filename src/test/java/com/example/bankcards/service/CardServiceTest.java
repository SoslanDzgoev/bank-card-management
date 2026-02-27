package com.example.bankcards.service;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
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
import org.junit.jupiter.api.Nested;
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

    @Nested
    @DisplayName("createCard()")
    class CreateCardTests {

        @Test
        @DisplayName("Успешное создание карты — начальный баланс 0, статус ACTIVE")
        void createCard_success() {
            CreateCardRequest request = new CreateCardRequest();
            request.setCardNumber("1234567890123456");
            request.setOwnerId(1L);
            request.setExpiryDate(LocalDate.of(2028, 12, 31));

            when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(encryptionUtil.encrypt("1234567890123456")).thenReturn("encrypted");
            when(encryptionUtil.decrypt("encrypted")).thenReturn("1234567890123456");
            when(encryptionUtil.mask("1234567890123456")).thenReturn("**** **** **** 3456");
            when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
                Card c = inv.getArgument(0);
                c.setId(10L);
                return c;
            });

            CardResponse response = cardService.createCard(request);

            assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);
            assertThat(response.getMaskedCardNumber()).isEqualTo("**** **** **** 3456");
            verify(cardRepository).save(any(Card.class));
        }

        @Test
        @DisplayName("Создание карты для несуществующего пользователя — ошибка 404")
        void createCard_ownerNotFound_throwsException() {
            CreateCardRequest request = new CreateCardRequest();
            request.setCardNumber("1234567890123456");
            request.setOwnerId(99L);
            request.setExpiryDate(LocalDate.of(2028, 12, 31));

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.createCard(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verify(cardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deposit()")
    class DepositTests {

        @Test
        @DisplayName("Успешное пополнение активной карты")
        void deposit_success() {
            when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
            when(encryptionUtil.decrypt("encrypted_1234")).thenReturn("1234567890123456");
            when(encryptionUtil.mask("1234567890123456")).thenReturn("**** **** **** 3456");
            when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

            CardResponse response = cardService.deposit(1L, new BigDecimal("500.00"));

            assertThat(response.getBalance()).isEqualByComparingTo("1500.00");
        }

        @Test
        @DisplayName("Пополнение заблокированной карты — ошибка")
        void deposit_blockedCard_throwsException() {
            fromCard.setStatus(CardStatus.BLOCKED);
            when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));

            assertThatThrownBy(() -> cardService.deposit(1L, new BigDecimal("500.00")))
                    .isInstanceOf(CardBlockedException.class)
                    .hasMessageContaining("не активна");

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Пополнение несуществующей карты — ошибка 404")
        void deposit_cardNotFound_throwsException() {
            when(cardRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.deposit(99L, new BigDecimal("100.00")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("requestBlock()")
    class RequestBlockTests {

        @Test
        @DisplayName("Успешный запрос блокировки владельцем карты")
        void requestBlock_success() {
            when(cardRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(fromCard));

            cardService.requestBlock(1L, owner);

            assertThat(fromCard.getStatus()).isEqualTo(CardStatus.BLOCKED);
            verify(cardRepository).save(fromCard);
        }

        @Test
        @DisplayName("Запрос блокировки чужой карты — ошибка 404 (IDOR защита)")
        void requestBlock_otherUserCard_throwsException() {
            when(cardRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.requestBlock(1L, owner))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCard()")
    class DeleteCardTests {

        @Test
        @DisplayName("Успешное удаление карты")
        void deleteCard_success() {
            when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));

            cardService.deleteCard(1L);

            verify(cardRepository).delete(fromCard);
        }

        @Test
        @DisplayName("Удаление несуществующей карты — ошибка 404")
        void deleteCard_notFound_throwsException() {
            when(cardRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.deleteCard(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verify(cardRepository, never()).delete(any());
        }
    }
}
