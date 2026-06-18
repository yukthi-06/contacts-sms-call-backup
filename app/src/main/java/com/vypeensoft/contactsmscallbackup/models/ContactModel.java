package com.vypeensoft.contactsmscallbackup.models;

public class ContactModel {
    private String displayName;
    private String phoneNumbers; // comma-separated
    private String emailAddresses; // comma-separated
    private String organization;
    private String notes;

    public ContactModel() {}

    public ContactModel(String displayName, String phoneNumbers, String emailAddresses, String organization, String notes) {
        this.displayName = displayName;
        this.phoneNumbers = phoneNumbers;
        this.emailAddresses = emailAddresses;
        this.organization = organization;
        this.notes = notes;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : "";
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoneNumbers() {
        return phoneNumbers != null ? phoneNumbers : "";
    }

    public void setPhoneNumbers(String phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public String getEmailAddresses() {
        return emailAddresses != null ? emailAddresses : "";
    }

    public void setEmailAddresses(String emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    public String getOrganization() {
        return organization != null ? organization : "";
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
