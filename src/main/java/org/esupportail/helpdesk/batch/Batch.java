package org.esupportail.helpdesk.batch;

import java.util.List;

import org.esupportail.commons.batch.BatchException;
import org.esupportail.commons.services.application.ApplicationService;
import org.esupportail.commons.services.application.ApplicationUtils;
import org.esupportail.commons.services.application.VersionningUtils;
import org.esupportail.commons.services.database.DatabaseUtils;
import org.esupportail.commons.services.exceptionHandling.ExceptionUtils;
import org.esupportail.commons.services.logging.Logger;
import org.esupportail.commons.services.logging.LoggerImpl;
import org.esupportail.commons.utils.BeanUtils;
import org.esupportail.helpdesk.domain.DomainService;
import org.esupportail.helpdesk.domain.beans.Ticket;
import org.esupportail.helpdesk.services.archiving.Archiver;
import org.esupportail.helpdesk.services.expiration.Expirator;
import org.esupportail.helpdesk.services.feed.ErrorHolder;
import org.esupportail.helpdesk.services.feed.Feeder;
import org.esupportail.helpdesk.services.indexing.Indexer;
import org.esupportail.helpdesk.services.recall.Recaller;

public class Batch {

    /**
     * Logger maison ESUP (LoggerImpl).
     */
    private static final Logger LOG = new LoggerImpl(Batch.class);

    private static final String DOMAIN_SERVICE_BEAN = "domainService";
    private static final String INDEXER_BEAN = "indexer";
    private static final String ARCHIVER_BEAN = "archiver";
    private static final String EXPIRATOR_BEAN = "expirator";
    private static final String RECALLER_BEAN = "recaller";
    private static final String FEEDER_BEAN = "feeder";

    private Batch() {
        throw new UnsupportedOperationException();
    }

    private static DomainService getDomainService() {
        return (DomainService) BeanUtils.getBean(DOMAIN_SERVICE_BEAN);
    }

    private static Indexer getIndexer() {
        return (Indexer) BeanUtils.getBean(INDEXER_BEAN);
    }

    private static Archiver getArchiver() {
        return (Archiver) BeanUtils.getBean(ARCHIVER_BEAN);
    }

    private static Expirator getExpirator() {
        return (Expirator) BeanUtils.getBean(EXPIRATOR_BEAN);
    }

    private static Recaller getRecaller() {
        return (Recaller) BeanUtils.getBean(RECALLER_BEAN);
    }

    private static Feeder getFeeder() {
        return (Feeder) BeanUtils.getBean(FEEDER_BEAN);
    }

    /**
     * Archive tickets by academic year.
     */
    private static void archiveTicketsByAcademicYear(final String year) {
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);

            LOG.info("Starting archive process for academic year " + year);

            getDomainService().archiveTicketsByAcademicYear(year);

            DatabaseUtils.commit();
            DatabaseUtils.close();

            LOG.info("Archiving done for academic year " + year);
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            LOG.error(">>> [Batch] Archive process FAILED for academic year " + year, t);
            throw new BatchException(t);
        }
    }

    /**
     * Dispatch depending on the arguments.
     */
    protected static void dispatch(final String[] args) {
        if (args.length == 0) {
            syntax();
            return;
        }

        switch (args[0]) {
            case "test-beans":
                testBeans();
                break;

            case "update-index":
                updateIndex(false);
                break;

            case "rebuild-index":
                updateIndex(true);
                break;

            case "unlock-index":
                unlockIndex();
                break;

            case "archive-tickets":
                archiveTickets();
                break;

            case "archive-ticket-by-academic-year":
                if (args.length < 2) {
                    throw new IllegalArgumentException(
                        "Usage: archive-ticket-by-academic-year <year>");
                }
                archiveTicketsByAcademicYear(args[1]);
                break;

            case "unlock-archive-tickets":
                unlockArchiveTickets();
                break;

            case "expire-tickets":
                expireTickets(true);
                break;

            case "expire-tickets-no-email":
                expireTickets(false);
                break;

            case "unlock-expire-tickets":
                unlockExpireTickets();
                break;

            case "recall-tickets":
                recallTickets();
                break;

            case "unlock-recall-tickets":
                unlockRecallTickets();
                break;

            case "send-ticket-reports":
                sendTicketReports();
                break;

            case "send-faq-reports":
                sendFaqReports();
                break;

            case "delete-all-tickets":
                deleteAllTickets();
                break;

            case "delete-archive-ticket-by-days":
                if (args.length < 2) {
                    throw new IllegalArgumentException("Usage: delete-archive-ticket-by-days <days>");
                }
                deleteArchivedTickets(args[1]);
                break;

            case "delete-ticket":
                if (args.length < 2) {
                    throw new IllegalArgumentException("Usage: delete-ticket <ticketNumber>");
                }
                deleteTicket(args[1]);
                break;

            default:
                syntax();
                break;
        }
    }

    private static void syntax() {
        throw new IllegalArgumentException(
            "syntax: " + Batch.class.getSimpleName() + " <options>"
            + "\n- archive-ticket-by-academic-year <year>"
            + "\n- archive-tickets"
            + "\n- delete-ticket <id>"
            + "\n- delete-archive-ticket-by-days <days>"
            + " ... etc ..."
        );
    }

    /* ========================
     * AUTRES METHODES EXISTANTES
     * ======================== */

    private static void testBeans() {
        DatabaseUtils.test();
        ApplicationUtils.createApplicationService();
    }

    private static void updateIndex(final boolean rebuild) {
        Indexer indexer = getIndexer();
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            if (rebuild) {
                indexer.removeIndex();
            }
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void unlockIndex() {
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            getIndexer().unlockIndex();
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void archiveTickets() {
        Archiver archiver = getArchiver();
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            archiver.archive();
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void unlockArchiveTickets() {
        Archiver archiver = getArchiver();
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            archiver.unlock();
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void expireTickets(final boolean alerts) {
        Expirator expirator = getExpirator();
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            expirator.expire(alerts);
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void unlockExpireTickets() {
        Expirator expirator = getExpirator();
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            expirator.unlock();
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void recallTickets() {
        Recaller recaller = getRecaller();
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            recaller.recall();
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void unlockRecallTickets() {
        Recaller recaller = getRecaller();
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            recaller.unlock();
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void sendTicketReports() {
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            getDomainService().sendTicketReports();
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void sendFaqReports() {
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            getDomainService().sendFaqReports();
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void deleteAllTickets() {
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            getDomainService().deleteAllTickets();
            getIndexer().updateIndex(true);
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void deleteArchivedTickets(final String days) {
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            getDomainService().deleteArchivedTickets(Integer.parseInt(days));
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    private static void deleteTicket(final String ticketNumber) {
        try {
            DatabaseUtils.open();
            DatabaseUtils.begin();
            VersionningUtils.checkVersion(true, false);
            getDomainService().deleteTicketById(Long.parseLong(ticketNumber));
            DatabaseUtils.commit();
            DatabaseUtils.close();
        } catch (Throwable t) {
            DatabaseUtils.rollback();
            DatabaseUtils.close();
            throw new BatchException(t);
        }
    }

    /**
     * Main method called by ant.
     */
    public static void main(final String[] args) {
        try {
            ApplicationService applicationService = ApplicationUtils.createApplicationService();
            LOG.info(applicationService.getName() + " v" + applicationService.getVersion());
            dispatch(args);
        } catch (Throwable t) {
            ExceptionUtils.catchException(t);
        }
    }
}
