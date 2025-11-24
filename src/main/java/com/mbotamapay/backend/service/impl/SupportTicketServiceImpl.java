package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.support.CreateTicketRequest;
import com.mbotamapay.backend.entity.SupportTicket;
import com.mbotamapay.backend.entity.TicketStatus;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.repository.SupportTicketRepository;
import com.mbotamapay.backend.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;

    @Override
    public SupportTicket createTicket(User user, CreateTicketRequest request) {
        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .subject(request.getSubject())
                .message(request.getMessage())
                .status(TicketStatus.OPEN)
                .build();
        return supportTicketRepository.save(ticket);
    }

    @Override
    public List<SupportTicket> getUserTickets(User user) {
        return supportTicketRepository.findByUser(user);
    }

    @Override
    public List<SupportTicket> getAllTickets() {
        return supportTicketRepository.findAll();
    }

    @Override
    public SupportTicket resolveTicket(Long ticketId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Ticket not found"));
        ticket.setStatus(TicketStatus.RESOLVED);
        return supportTicketRepository.save(ticket);
    }
}
