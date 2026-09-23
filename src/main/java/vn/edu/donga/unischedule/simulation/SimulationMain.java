package vn.edu.donga.unischedule.simulation;

import java.nio.file.*;
import java.util.*;

public final class SimulationMain {
    private SimulationMain() { }
    public static void main(String[] args) throws Exception {
        var options=new LinkedHashMap<String,String>();
        for(int i=0;i<args.length;i+=2) {
            if(i+1>=args.length || !List.of("--capture-baseline","--baseline","--start-year","--seed","--out").contains(args[i]) || options.put(args[i],args[i+1])!=null)
                throw new IllegalArgumentException("Usage: --baseline FILE --start-year 2027 --seed 42 --out NEW_DIRECTORY; or --capture-baseline NEW_FILE");
        }
        if(options.containsKey("--capture-baseline")) {
            if(options.size()!=1) throw new IllegalArgumentException("Capture must be a separate command");
            SimulationBaseline.capture(Path.of(options.get("--capture-baseline"))); System.out.println("Saved read-only, anonymized baseline."); return;
        }
        int year=Integer.parseInt(options.getOrDefault("--start-year","2027")); long seed=Long.parseLong(options.getOrDefault("--seed","42"));
        Path baseline=Path.of(options.getOrDefault("--baseline","simulation/baseline-2026-09-22.properties"));
        Path out=Path.of(options.getOrDefault("--out","simulation/results/"+year+"-"+(year+4)+"-seed"+seed));
        if(Files.exists(out)) throw new IllegalArgumentException("Output exists. Choose a NEW --out directory; no existing data is overwritten: "+out);
        var data=new FiveYearSimulation(new SimulationBaseline(baseline),year,seed).run();
        SimulationExport.write(data,baseline,out,year,seed);
        System.out.println("SIMULATION ONLY | all validation checks passed | "+out.toAbsolutePath());
        System.out.println("Year | Users start + new - left = end | Sessions | Requests");
        for(var y:data.annual) System.out.printf("%s | %s + %s - %s = %s | %s | %s%n",y.get("year"),y.get("users_start"),y.get("users_new"),y.get("users_left"),y.get("users_end"),y.get("sessions"),y.get("requests"));
    }
}
