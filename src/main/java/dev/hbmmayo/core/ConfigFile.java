package dev.hbmmayo.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Standalone server config loader; platform must load on the server and synchronize HUD separately. */
public final class ConfigFile {
    private ConfigFile() {}
    public record Loaded(MayoConfig server, int hudX, int hudY) {}
    public static Loaded load(Path path) throws IOException {
        Properties p = new Properties();
        try (Reader in=Files.newBufferedReader(path)) { p.load(in); }
        return parse(p);
    }
    public static Loaded parse(Properties p) throws IOException {
        var defaults=MayoConfig.defaults();
        var components=MayoConfig.class.getRecordComponents();
        var allowed=new HashSet<String>();
        var arguments=new Object[components.length];
        var types=new Class<?>[components.length];
        try {
            for(int i=0;i<components.length;i++) {
                var part=components[i]; String key=part.getName(); allowed.add(key); types[i]=part.getType();
                String value=p.getProperty(key);
                if(value==null) arguments[i]=part.getAccessor().invoke(defaults);
                else if(types[i]==int.class) arguments[i]=Integer.parseInt(value.trim());
                else if(types[i]==long.class) arguments[i]=Long.parseLong(value.trim());
                else arguments[i]=Double.parseDouble(value.trim());
            }
            allowed.add("hudX"); allowed.add("hudY");
            for(String key:p.stringPropertyNames()) if(!allowed.contains(key)) throw new IOException("Unknown config key: "+key);
            return new Loaded(MayoConfig.class.getDeclaredConstructor(types).newInstance(arguments),
                Integer.parseInt(p.getProperty("hudX","8").trim()),Integer.parseInt(p.getProperty("hudY","8").trim()));
        } catch(ReflectiveOperationException | IllegalArgumentException e) {
            throw new IOException("Invalid HBM Mayonnaise configuration",e);
        }
    }
}
