package vn.edu.donga.unischedule.config;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import vn.edu.donga.unischedule.repository.jdbc.ConnectionFactory;
/** Explicit setup command; never runs automatically when the desktop app starts. */
public final class DatabaseSetup {
    public static void main(String[] args) throws Exception {
        boolean seed=Arrays.asList(args).contains("--seed");
        try(Connection c=new ConnectionFactory().open()) {
            try(var p=c.prepareStatement("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE'");var r=p.executeQuery()) {
                r.next();if(r.getInt(1)!=0) throw new IllegalStateException("Database phải rỗng. Không ghi đè database hiện có.");
            }
            for(String file:List.of("01_create_identity_tables.sql","02_create_academic_tables.sql","03_create_facility_tables.sql","04_create_schedule_tables.sql","05_create_request_tables.sql","06_create_indexes.sql","07_create_views.sql")) run(c,file);
            if(seed) {
                c.setAutoCommit(false);
                try { for(String file:List.of("09_seed_roles_users.sql","10_seed_academic_data.sql","11_seed_facility_data.sql","12_seed_schedule_data.sql")) run(c,file);c.commit(); }
                catch(Exception ex) {c.rollback();throw ex;}
            }
            System.out.println("Database initialized. Demo seed: "+seed);
        }
    }
    private static void run(Connection c,String file) throws Exception {
        try(var in=DatabaseSetup.class.getResourceAsStream("/db/"+file)) {
            if(in==null)throw new IllegalStateException(file);
            String sql=new String(in.readAllBytes(),StandardCharsets.UTF_8);
            // These versioned scripts contain no stored routines or semicolons in literals.
            for(String command:sql.split(";")) if(!command.isBlank()) try(var p=c.prepareStatement(command.trim())) {p.execute();}
        }
        System.out.println("Applied "+file);
    }
}
