package examples;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class UsefulClasses {
    public static void main(String[] args) {

        // ByteBuffer examples

        ByteBuffer buff = ByteBuffer.allocate(16);
        buff.put(1, (byte)0xE5); // write a char at index 1
        assert(buff.get(1)==(byte)0xE5);
        buff.putInt(4, 0xFFFFFFFF); // write a char at index 1
        assert(buff.getInt(4)==0xFFFFFFFF);

        byte[] tab = new byte[20];
        ByteBuffer buff_wrap = ByteBuffer.wrap(tab, 0, 16);
        buff_wrap.putInt(4, 0xFFFFFFFF); // write a char at index 1
        buff_wrap.put(1, (byte)0xE5); // write a char at index 1
        assert(buff_wrap.equals(buff));

        //access tp array if it exists
        assert(buff_wrap.array()[1]==(byte)0xE5);

        buff_wrap = ByteBuffer.wrap(tab,8,3); // useful for comparisons but , but beware of following cases
        assert(buff_wrap.array()[1]==(byte)0xE5);
        assert(buff_wrap.array()[4]==(byte)0xFF);

      //  buff_wrap.put((byte)0x05); // write a char at index 1


        // byte[] to string
        buff = ByteBuffer.allocate(3);
        buff.put(0,(byte)'a');
        buff.put(1,(byte)'b');
        buff.put(2,(byte)'c');
        System.out.println(StandardCharsets.UTF_8.decode(buff).toString());

        // System.arraycopy

        // Optional
        Optional<fs.ClusterId> optin = Optional.empty();
        assert(optin.isEmpty());
        optin = Optional.of(new fs.ClusterId(10));
        assert(!optin.isEmpty());

        // bitwise operations
        int val = (0xEFE0FAF8 & 0xFF) | 0x08;

        // ternary operator
        boolean cond = true;
        int test = cond ? 0x00000000 : 0x0FFFFFFF;
        assert(test==0x00000000);
    }

}
