package org.esupportail.helpdesk.services.navigation;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneId;

import org.esupportail.helpdesk.domain.DomainService;
import org.esupportail.helpdesk.domain.beans.Ticket;

/**
 * Navigator for archiving tickets by academic year.
 */
public class TicketNavigatorForArchiving {

    private DomainService domainService;

    private String academicYear;

    public void setDomainService(final DomainService domainService) {
        this.domainService = domainService;
    }

    public void setAcademicYear(final String academicYear) {
        this.academicYear = academicYear;
    }

    public List<Ticket> getTickets() {
        if (academicYear == null) {
            throw new IllegalStateException("Academic year must be set before calling getTickets()");
        }

        // Récupère tous les tickets depuis DomainService
        List<Ticket> allTickets = domainService.getTickets(0, Integer.MAX_VALUE);

        // Filtre par année de création
        return allTickets.stream()
                .filter(t -> {
                    if (t.getCreationDate() == null) {
                        return false;
                    }
                    LocalDate creationYear = t.getCreationDate().toInstant()
                                              .atZone(ZoneId.systemDefault())
                                              .toLocalDate();
                    return String.valueOf(creationYear.getYear()).equals(academicYear);
                })
                .collect(Collectors.toList());
    }
}
