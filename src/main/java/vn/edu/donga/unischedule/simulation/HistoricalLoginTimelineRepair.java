package vn.edu.donga.unischedule.simulation;

import vn.edu.donga.unischedule.repository.jdbc.ConnectionFactory;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Backfills demo login history without touching real login events or existing non-null original values. */
public final class HistoricalLoginTimelineRepair {
    public static final String MARKER="REPAIR_HISTORY_LOGIN_TIMELINE_V2";
    private HistoricalLoginTimelineRepair() { }
    private record Account(long id,String username,String status,LocalDate created,String lastLogin,boolean synthetic) { }

    public static void main(String[] args) throws Exception {
        if(args.length!=0)throw new IllegalArgumentException("No options accepted");
        try(var c=new ConnectionFactory().open()) {
            int changed=apply(c);
            System.out.println(changed<0?"ALREADY_REPAIRED: no rows changed.":"Repaired "+changed+" last-login dates; existing real logins preserved.");
        }
    }

    public static int apply(Connection c) throws Exception {
        c.setAutoCommit(false);
        try {
            LocalDate asOf;
            try(var p=c.prepareStatement("SELECT JSON_UNQUOTE(JSON_EXTRACT(details,'$.as_of')) FROM audit_logs WHERE action=?")) {
                p.setString(1,HistoricalSeedImporter.MARKER);
                try(var r=p.executeQuery()){if(!r.next())throw new IllegalStateException("Completed history seed is required");asOf=LocalDate.parse(r.getString(1));}
            }
            try(var p=c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action=?")) {
                p.setString(1,MARKER);try(var r=p.executeQuery()){r.next();if(r.getInt(1)>0){c.rollback();return -1;}}
            }
            var departures=new HashMap<Long,LocalDate>();
            try(var p=c.prepareStatement("SELECT entity_id,MIN(created_at) FROM audit_logs WHERE action='DEACTIVATE_USER' AND entity_type='users' AND JSON_UNQUOTE(JSON_EXTRACT(details,'$.seed'))='HISTORY_5Y' GROUP BY entity_id");var r=p.executeQuery()) {
                while(r.next())departures.put(r.getLong(1),LocalDate.parse(r.getString(2).substring(0,10)));
            }
            var realLogins=new HashSet<Long>();
            try(var p=c.prepareStatement("SELECT DISTINCT user_id FROM audit_logs WHERE action='LOGIN' AND JSON_UNQUOTE(JSON_EXTRACT(details,'$.result'))='SUCCESS'");var r=p.executeQuery()) {
                while(r.next())realLogins.add(r.getLong(1));
            }
            var earlierEvents=new HashMap<Long,Long>();
            try(var p=c.prepareStatement("SELECT id,user_id FROM audit_logs WHERE action='LOGIN' AND JSON_UNQUOTE(JSON_EXTRACT(details,'$.source'))='login_timeline_repair'");var r=p.executeQuery()) {
                while(r.next())earlierEvents.put(r.getLong(2),r.getLong(1));
            }
            var accounts=new ArrayList<Account>();
            String sql="SELECT u.id,u.username,u.status,u.created_at,u.last_login_at,"+
                    "EXISTS(SELECT 1 FROM audit_logs a WHERE a.entity_type='users' AND a.entity_id=u.id AND a.action='CREATE_USER' AND JSON_UNQUOTE(JSON_EXTRACT(a.details,'$.seed'))='HISTORY_5Y') AS synthetic " +
                    "FROM users u WHERE u.id BETWEEN 5 AND 28 OR EXISTS(SELECT 1 FROM audit_logs a WHERE a.entity_type='users' AND a.entity_id=u.id AND a.action='CREATE_USER' AND JSON_UNQUOTE(JSON_EXTRACT(a.details,'$.seed'))='HISTORY_5Y') ORDER BY u.id FOR UPDATE";
            try(var p=c.prepareStatement(sql);var r=p.executeQuery()) {
                while(r.next())accounts.add(new Account(r.getLong(1),r.getString(2),r.getString(3),LocalDate.parse(r.getString(4).substring(0,10)),r.getString(5),r.getBoolean(6)));
            }
            int changed=0;
            for(var user:accounts) {
                String old=user.lastLogin();
                if(realLogins.contains(user.id()))continue;
                if(!user.synthetic()) {
                    if(!user.status().equals("ACTIVE") || (old!=null&&!earlierEvents.containsKey(user.id())))continue;
                } else {
                    if(old!=null) {
                        LocalDate oldDate=LocalDate.parse(old.substring(0,10));
                        boolean originalSeedValue=old.endsWith("08:00:00")
                                && !oldDate.isBefore(asOf.minusDays(7))&&!oldDate.isAfter(asOf);
                        if(!originalSeedValue&&!earlierEvents.containsKey(user.id()))continue;
                    }
                }
                LocalDate target=user.synthetic()
                        ?HistoricalLoginTimeline.lastLogin(user.username(),user.created(),departures.get(user.id()),asOf,user.username().startsWith("sv"))
                        :user.created().plusDays(1+Math.floorMod(Long.hashCode(user.id()),10));
                if(target.isAfter(asOf))target=asOf;
                if(target.isBefore(user.created()))throw new IllegalStateException("Login predates account: "+user.id());
                String time=target+" 08:00:00";
                try(var p=c.prepareStatement("UPDATE users SET last_login_at=?,updated_at=updated_at WHERE id=? AND last_login_at <=> ?")) {
                    p.setString(1,time);p.setLong(2,user.id());p.setString(3,old);
                    if(p.executeUpdate()!=1)throw new IllegalStateException("Account changed during repair: "+user.id());
                }
                if(earlierEvents.containsKey(user.id())) {
                    try(var p=c.prepareStatement("UPDATE audit_logs SET created_at=? WHERE id=? AND user_id=? AND action='LOGIN' AND JSON_UNQUOTE(JSON_EXTRACT(details,'$.source'))='login_timeline_repair'")) {
                        p.setString(1,time);p.setLong(2,earlierEvents.get(user.id()));p.setLong(3,user.id());
                        if(p.executeUpdate()!=1)throw new IllegalStateException("Backfill event changed: "+user.id());
                    }
                } else try(var p=c.prepareStatement("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details,created_at) VALUES (?,'LOGIN','users',?,JSON_OBJECT('seed','HISTORY_5Y','source','login_timeline_repair'),?)")) {
                    p.setLong(1,user.id());p.setLong(2,user.id());p.setString(3,time);p.executeUpdate();
                }
                changed++;
            }
            try(var p=c.prepareStatement("INSERT INTO audit_logs(action,entity_type,details,created_at) VALUES (?,'database',JSON_OBJECT('seed','HISTORY_5Y','count',?),UTC_TIMESTAMP())")) {
                p.setString(1,MARKER);p.setInt(2,changed);p.executeUpdate();
            }
            c.commit();return changed;
        } catch(Exception ex) {c.rollback();throw ex;}
        finally {c.setAutoCommit(true);}
    }
}
