package com.mbotamapay.backend.service;

import com.mbotamapay.backend.dto.support.CreateTicketRequest;
import com.mbotamapay.backend.entity.SupportTicket;
import com.mbotamapay.backend.entity.User;

import java.util.List;

public interface SupportTicketService {
    SupportTicket createTicket(User user, CreateTicketRequest request);

    List<SupportTicket> getUserTickets(User user);

    List<SupportTicket> getAllTickets(); // Admin

    SupportTicket resolveTicket(Long ticketId);
}
