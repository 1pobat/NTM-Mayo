package dev.hbmmayo.neoforge;
import dev.hbmmayo.core.InfusionState;
import java.io.*;
import java.util.Base64;

public final class StateCodec {
    private StateCodec() {}
    public static String encode(InfusionState state) {
        try {
            var bytes=new ByteArrayOutputStream(); state.write(new DataOutputStream(bytes));
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch(IOException e) { throw new IllegalStateException(e); }
    }
    public static InfusionState decode(String encoded) {
        try {
            byte[] bytes=Base64.getDecoder().decode(encoded);
            try(var in=new DataInputStream(new ByteArrayInputStream(bytes))) {
                var state=InfusionState.read(in);
                if(in.available()!=0) throw new IOException("Trailing infusion data");
                return state;
            }
        } catch(IOException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid saved Flashgold Infusion",e);
        }
    }
}
