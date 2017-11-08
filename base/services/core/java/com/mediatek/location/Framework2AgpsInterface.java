// This source code is generated by UdpGeneratorTool, not recommend to modify it directly
package com.mediatek.location;

import java.util.Arrays;

import com.mediatek.socket.base.SocketUtils;
import com.mediatek.socket.base.SocketUtils.BaseBuffer;
import com.mediatek.socket.base.SocketUtils.ProtocolHandler;
import com.mediatek.socket.base.SocketUtils.UdpServerInterface;
import com.mediatek.socket.base.UdpClient;

/**
 * The interface from Framework  to AGPS <br>
 */
public class Framework2AgpsInterface {
    public final static int PROTOCOL_TYPE = 302;
    public final static int MAX_BUFF_SIZE = 35;
    public final static int DNS_QUERY_RESULT = 0;

    public static class Framework2AgpsInterfaceSender {
        public boolean DnsQueryResult(UdpClient client, boolean isSuccess, boolean hasIpv4,
                int ipv4, boolean hasIpv6, byte[] ipv6) {
            if(!client.connect()) {
                return false;
            }
            BaseBuffer buff = client.getBuff();
            buff.putInt(PROTOCOL_TYPE);
            buff.putInt(DNS_QUERY_RESULT);
            buff.putBool(isSuccess);
            buff.putBool(hasIpv4);
            buff.putInt(ipv4);
            buff.putBool(hasIpv6);
            SocketUtils.assertSize(ipv6, 16, 0);
            buff.putArrayByte(ipv6);
            boolean _ret = client.write();
            client.close();
            return _ret;
        }

    }
/*
    public static abstract class Framework2AgpsInterfaceReceiver implements ProtocolHandler {

        public abstract void DnsQueryResult(boolean isSuccess, boolean hasIpv4, int ipv4,
                boolean hasIpv6, byte[] ipv6);


        public boolean readAndDecode(UdpServerInterface server) {
            if (!server.read()) {
                return false;
            }
            return decode(server);
        }

        @Override
        public int getProtocolType() {
            return PROTOCOL_TYPE;
        }

        @Override
        public boolean decode(UdpServerInterface server) {
            boolean _ret = true;
            BaseBuffer buff = server.getBuff();
            buff.setOffset(4); // skip protocol type
            int _type = buff.getInt();
            switch (_type) {
            case DNS_QUERY_RESULT: {
                boolean isSuccess = (boolean) buff.getBool();
                boolean hasIpv4 = (boolean) buff.getBool();
                int ipv4 = (int) buff.getInt();
                boolean hasIpv6 = (boolean) buff.getBool();
                byte[] ipv6 = (byte[]) buff.getArrayByte();
                DnsQueryResult(isSuccess, hasIpv4, ipv4, hasIpv6, ipv6);
                break;
            }
            default: {
                _ret = false;
                break;
            }
            }
            return _ret;
        }
    }
*/
}
