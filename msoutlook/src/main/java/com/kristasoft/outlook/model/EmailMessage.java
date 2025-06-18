package com.kristasoft.outlook.model;

import java.time.Instant;

/**
 * Represents a standardized email message with metadata.
 * Follows the Single Responsibility Principle by focusing only on email data representation.
 */
public class EmailMessage {
    private String messageId;
    private String subject;
    private String senderName;
    private String senderEmail;
    private Instant receivedDateTime;
    private Boolean isRead;
    private String previewText;
    private String importance;
    private Boolean hasAttachments;
    
    // Builder pattern for clean object creation
    public static class Builder {
        private final EmailMessage message;
        
        public Builder() {
            message = new EmailMessage();
        }
        
        public Builder messageId(String messageId) {
            message.messageId = messageId;
            return this;
        }
        
        public Builder subject(String subject) {
            message.subject = subject;
            return this;
        }
        
        public Builder senderName(String senderName) {
            message.senderName = senderName;
            return this;
        }
        
        public Builder senderEmail(String senderEmail) {
            message.senderEmail = senderEmail;
            return this;
        }
        
        public Builder receivedDateTime(Instant receivedDateTime) {
            message.receivedDateTime = receivedDateTime;
            return this;
        }
        
        public Builder isRead(Boolean isRead) {
            message.isRead = isRead;
            return this;
        }
        
        public Builder previewText(String previewText) {
            message.previewText = previewText;
            return this;
        }
        
        public Builder importance(String importance) {
            message.importance = importance;
            return this;
        }
        
        public Builder hasAttachments(Boolean hasAttachments) {
            message.hasAttachments = hasAttachments;
            return this;
        }
        
        public EmailMessage build() {
            return message;
        }
    }
    
    // Private constructor to enforce builder usage
    private EmailMessage() {}
    
    // Getters
    public String getMessageId() {
        return messageId;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public String getSenderEmail() {
        return senderEmail;
    }
    
    public Instant getReceivedDateTime() {
        return receivedDateTime;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public String getPreviewText() {
        return previewText;
    }
    
    public String getImportance() {
        return importance;
    }
    
    public Boolean getHasAttachments() {
        return hasAttachments;
    }
}