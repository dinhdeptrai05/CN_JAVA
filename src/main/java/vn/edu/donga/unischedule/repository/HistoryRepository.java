package vn.edu.donga.unischedule.repository;

import vn.edu.donga.unischedule.model.HistorySnapshot;

public interface HistoryRepository {
    HistorySnapshot load();
}
