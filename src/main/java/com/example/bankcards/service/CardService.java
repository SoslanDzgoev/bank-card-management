package com.example.bankcards.service;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.SameCardTransferException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardEncryptionUtil encryptionUtil;

    /**
     * Создаёт новую карту для указанного пользователя.
     * Номер карты шифруется перед сохранением в БД.
     * Начальный баланс — 0, статус — ACTIVE.
     */
    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + request.getOwnerId()));

        Card card = Card.builder()
                .encryptedCardNumber(encryptionUtil.encrypt(request.getCardNumber()))
                .owner(owner)
                .expiryDate(request.getExpiryDate())
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();

        return toResponse(cardRepository.save(card));
    }

    /**
     * Возвращает все карты системы с пагинацией (только для администратора).
     */
    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Возвращает карты текущего пользователя с пагинацией.
     * Поддерживает фильтрацию по статусу карты.
     */
    @Transactional(readOnly = true)
    public Page<CardResponse> getMyCards(User currentUser, CardStatus status, Pageable pageable) {
        if (status != null) {
            return cardRepository.findByOwnerAndStatus(currentUser, status, pageable).map(this::toResponse);
        }
        return cardRepository.findByOwner(currentUser, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CardResponse getCardById(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена: " + id));
        return toResponse(card);
    }

    /**
     * Пополняет баланс карты (только для администратора).
     * Пополнение возможно только для активных карт.
     */
    @Transactional
    public CardResponse deposit(Long cardId, BigDecimal amount) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена: " + cardId));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new CardBlockedException("Пополнение недоступно: карта не активна");
        }
        card.setBalance(card.getBalance().add(amount));
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse blockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена: " + cardId));
        card.setStatus(CardStatus.BLOCKED);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена: " + cardId));
        card.setStatus(CardStatus.ACTIVE);
        return toResponse(cardRepository.save(card));
    }

    /**
     * Позволяет владельцу карты запросить её блокировку.
     * Используется метод findByIdAndOwner для защиты от IDOR атак:
     * пользователь не может заблокировать чужую карту, зная её ID.
     */
    @Transactional
    public void requestBlock(Long cardId, User currentUser) {
        Card card = cardRepository.findByIdAndOwner(cardId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена: " + cardId));
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена: " + cardId));
        cardRepository.delete(card);
    }

    /**
     * Перевод средств между картами одного владельца.
     *
     * Метод атомарен (@Transactional): если что-то пойдёт не так после списания,
     * вся операция откатится и деньги не потеряются.
     *
     * Проверки перед переводом:
     * 1. Нельзя переводить самому себе (fromCard == toCard)
     * 2. Обе карты должны принадлежать текущему пользователю (IDOR защита)
     * 3. Обе карты должны быть активны
     * 4. На карте-отправителе должно быть достаточно средств
     */
    @Transactional
    public void transfer(TransferRequest request, User currentUser) {
        if (request.getFromCardId().equals(request.getToCardId())) {
            throw new SameCardTransferException("Нельзя переводить на ту же карту");
        }

        Card fromCard = cardRepository.findByIdAndOwner(request.getFromCardId(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Карта-отправитель не найдена"));

        Card toCard = cardRepository.findByIdAndOwner(request.getToCardId(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Карта-получатель не найдена"));

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardBlockedException("Карта-отправитель не активна");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardBlockedException("Карта-получатель не активна");
        }
        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на карте");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
    }

    /**
     * Конвертирует сущность Card в DTO для отправки клиенту.
     * Расшифровывает номер карты и сразу маскирует его.
     * Клиент никогда не видит ни зашифрованную, ни открытую версию номера.
     */
    private CardResponse toResponse(Card card) {
        String decrypted = encryptionUtil.decrypt(card.getEncryptedCardNumber());
        return CardResponse.builder()
                .id(card.getId())
                .maskedCardNumber(encryptionUtil.mask(decrypted))
                .ownerEmail(card.getOwner().getEmail())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .build();
    }
}
