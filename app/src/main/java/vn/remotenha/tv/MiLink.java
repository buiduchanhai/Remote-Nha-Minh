package vn.remotenha.tv;

import java.io.*;
import java.net.*;

final class MiLink {
    private static byte[] event(int id, int key, int action) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        DataOutputStream fields = new DataOutputStream(body);
        int[] values = {0, action, key, 0, 0, 0};
        for (int i = 0; i < values.length; i++) {
            fields.writeByte(i + 1);
            fields.writeInt(values[i]);
        }
        for (int tag : new int[]{7,8}) {
            fields.writeByte(tag);
            fields.writeLong(0);
        }
        fields.writeByte(10); fields.writeInt(-1);
        fields.writeByte(11); fields.writeInt(0x101);
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        DataOutputStream frame = new DataOutputStream(packet);
        frame.writeByte(4); frame.writeShort(7 + body.size());
        frame.writeByte(1); frame.writeInt(id); frame.writeShort(body.size());
        body.writeTo(frame);
        return packet.toByteArray();
    }

    static void press(String host, int key) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, 6091), 1800);
            socket.setSoTimeout(2200);
            socket.setTcpNoDelay(true);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.write(event(1,key,0));
            out.write(event(2,key,1));
            out.flush();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            boolean down = false, up = false;
            for (int i = 0; i < 8 && !(down && up); i++) {
                int type = in.readUnsignedByte();
                byte[] data = new byte[in.readUnsignedShort()];
                in.readFully(data);
                if (type == 4 && data.length >= 5 && data[0] == 2) {
                    int id = java.nio.ByteBuffer.wrap(data,1,4).getInt();
                    down |= id == 1;
                    up |= id == 2;
                }
            }
            if (!(down && up)) throw new IOException("Thiếu phản hồi phím");
        }
    }
}
