package com.kristasoft.outlook.service;

import com.kristasoft.outlook.exception.EmailFolderException;

import java.util.List;

/**
 * Service interface for retrieving email folders/labels from a connected email account.
 * Follows the Interface Segregation Principle by providing a focused interface.
 */
public interface EmailFolderService {
    
    /**
     * Lists all folders/labels from the connected email account.
     * 
     * @return A list of folder/label names as strings
     * @throws EmailFolderException if there's an error retrieving the folders
     */
    List<String> listAllFolders() throws EmailFolderException;
}