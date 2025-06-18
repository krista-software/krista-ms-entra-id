package com.kristasoft.outlook.model;

import java.util.List;

/**
 * Represents the result of an inbox email listing operation.
 * Contains both the list of emails and the total count.
 */
public class InboxResult {
    private final List<EmailMessage> emails;
    private final int totalCount;
    
    public InboxResult(List<EmailMessage> emails, int totalCount) {
        this.emails = emails;
        this.totalCount = totalCount;
    }
    
    public List<EmailMessage> getEmails() {
        return emails;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
}