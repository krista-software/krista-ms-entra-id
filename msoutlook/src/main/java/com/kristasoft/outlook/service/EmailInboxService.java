package com.kristasoft.outlook.service;

import com.kristasoft.outlook.exception.EmailInboxException;
import com.kristasoft.outlook.model.EmailMessage;
import com.kristasoft.outlook.model.InboxResult;

/**
 * Service interface for retrieving emails from the inbox of a connected email account.
 * Follows the Interface Segregation Principle by providing a focused interface.
 */
public interface EmailInboxService {
    
    /**
     * Lists emails from the inbox folder of the connected email account.
     * 
     * @param pageSize Number of emails to retrieve per request (default: 25, maximum: 999)
     * @param pageNumber Page number for pagination (default: 0)
     * @param includeReadStatus Whether to include read/unread status (default: true)
     * @return InboxResult containing the list of emails and total count
     * @throws EmailInboxException if there's an error retrieving the emails
     */
    InboxResult listInboxEmails(Integer pageSize, Integer pageNumber, Boolean includeReadStatus) 
            throws EmailInboxException;
}