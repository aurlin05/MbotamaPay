package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.SupportTicket;
import com.mbotamapay.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUser(User user);
}
