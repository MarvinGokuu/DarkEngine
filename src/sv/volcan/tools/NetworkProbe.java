package sv.volcan.tools;

import java.net.InetAddress;

public class NetworkProbe {
    public static void main(String[] args) {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            System.out.println("NEURAL_COORDINATES:" + ip.getHostAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
