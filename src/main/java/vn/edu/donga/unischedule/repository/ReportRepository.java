package vn.edu.donga.unischedule.repository;
import vn.edu.donga.unischedule.model.Report;
import vn.edu.donga.unischedule.model.User;
@FunctionalInterface
public interface ReportRepository { Report.Source load(User user); }
