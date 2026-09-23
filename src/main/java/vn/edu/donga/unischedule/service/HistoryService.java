package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.HistorySnapshot;
import vn.edu.donga.unischedule.repository.HistoryRepository;

public final class HistoryService {
    private final HistoryRepository repository;
    public HistoryService(HistoryRepository repository) {this.repository=repository;}
    public HistorySnapshot load() {return repository.load();}
}
