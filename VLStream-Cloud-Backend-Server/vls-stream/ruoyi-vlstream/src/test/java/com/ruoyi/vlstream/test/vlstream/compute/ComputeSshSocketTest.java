package com.ruoyi.vlstream.test.vlstream.compute;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

class ComputeSshSocketTest {
    @Test void connectsOnlyToValidatedAddress() throws Exception {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        try (ServerSocket server = new ServerSocket(0, 1, loopback);
             Socket client = ComputeSsh.verifiedSockets(new InetAddress[]{loopback}).createSocket("127.0.0.1", server.getLocalPort());
             Socket peer = server.accept()) {
            assertTrue(client.isConnected());
            assertEquals(loopback, client.getInetAddress());
        }
    }

    @Test void changedAddressClosesConnectionBeforeAnySshData() throws Exception {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        try (ServerSocket server = new ServerSocket(0, 1, loopback)) {
            assertThrows(IOException.class, () -> ComputeSsh.verifiedSockets(new InetAddress[]{InetAddress.getByName("192.0.2.1")})
                .createSocket("127.0.0.1", server.getLocalPort()));
            server.setSoTimeout(2000);
            try (Socket peer = server.accept()) {
                peer.setSoTimeout(2000);
                assertEquals(-1, peer.getInputStream().read());
            }
        }
    }
}
