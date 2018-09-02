package com.harispraba.voicediary.model;

import java.util.ArrayList;
import java.util.HashMap;

public class Folder {
    private String name;
    private HashMap<String, Object> noteList;

    public Folder() {
    }

    public Folder(String name, HashMap<String, Object> noteList) {
        this.name = name;
        this.noteList = noteList;
    }

    public String getName() {
        return name;
    }

    public HashMap<String, Object> getNoteList() {
        return noteList;
    }
}