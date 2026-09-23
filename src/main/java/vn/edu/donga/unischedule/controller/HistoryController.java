package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.HistorySnapshot;
import vn.edu.donga.unischedule.service.HistoryService;

public final class HistoryController {
    private final HistoryService service;
    public HistoryController(HistoryService service) {this.service=service;}
    public HistorySnapshot load() {return service.load();}
}
