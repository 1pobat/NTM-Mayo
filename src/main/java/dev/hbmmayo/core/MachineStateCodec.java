package dev.hbmmayo.core;
import java.io.*;
import java.util.Base64;

/** Identical persisted format for a placed machine and its dropped block item. */
public final class MachineStateCodec {
    private MachineStateCodec() {}
    public record Saved(long power,CoolantTank coolant) {}
    public static String encode(long power,CoolantTank coolant) {
        if(power<0 || power>1000000 || coolant.capacity()!=16000) throw new IllegalArgumentException("Machine state");
        try {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            out.writeInt(1);out.writeLong(power);coolant.write(out);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch(IOException e) { throw new IllegalStateException(e); }
    }
    public static Saved decode(String encoded) {
        try(var in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))) {
            if(in.readInt()!=1) throw new IOException("Unknown machine schema");
            long power=in.readLong();var tank=CoolantTank.read(in);
            if(in.available()!=0 || power<0 || power>1000000 || tank.capacity()!=16000)
                throw new IOException("Invalid persisted machine state");
            return new Saved(power,tank);
        } catch(IOException | IllegalArgumentException e) { throw new IllegalArgumentException("Invalid machine data",e); }
    }
}
