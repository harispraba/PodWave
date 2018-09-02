package com.harispraba.voicediary.model;

public class Note {
    private String title;
    private String description;
    private String folderKey;

    public Note() {
    }

    public Note(String title, String description, String folderKey) {
        this.title = title;
        this.description = description;
        this.folderKey = folderKey;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getFolderKey() {
        return folderKey;
    }
}
