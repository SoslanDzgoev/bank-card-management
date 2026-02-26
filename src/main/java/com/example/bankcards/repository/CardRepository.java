package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findByOwner(User owner, Pageable pageable);

    Page<Card> findByOwnerAndStatus(User owner, CardStatus status, Pageable pageable);

    Optional<Card> findByIdAndOwner(Long id, User owner);
}
