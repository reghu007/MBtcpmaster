package com.reghutcpmaster;

import java.net.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.Modbus;

/**
 * Class that implements a simple commandline
 * tool for reading a digital input.
 *
 * @author Dieter Wimberger
 * @version 1.2rc1 (09/11/2004)
 */
public class ReghuTCPmaster {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
    private static final String URL_MBPOST = "http://13.233.228.209/190508/mb.php";

    public static void main(String[] args) {

        TCPMasterConnection con = null;
        ModbusTCPTransaction trans = null;
        ReadInputDiscretesRequest req = null;
        ReadInputDiscretesResponse res = null;

        InetAddress addr = null;
        int ref = 0;
        int count = 0;
        int repeat = 2;
        int slaves = 1;
        int port = Modbus.DEFAULT_PORT;

        try {
            ReghuTCPmaster objReghuTCPmaster = new ReghuTCPmaster();

            //1. Setup the parameters
            if (args.length < 3) {
                printUsage();
                System.exit(1);
            } else {
                try {
                    String astr = args[0];
                    int idx = astr.indexOf(':');
                    if(idx > 0) {
                        port = Integer.parseInt(astr.substring(idx+1));
                        astr = astr.substring(0,idx);
                    }
                    addr = InetAddress.getByName(astr);
                    ref = Integer.parseInt(args[1]);
                    count = Integer.parseInt(args[2]);
                    if (args.length > 3) {
                        slaves = Integer.parseInt(args[3]);
                    }
                    if (args.length == 5) {
                        repeat = Integer.parseInt(args[4]);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    printUsage();
                    System.exit(1);
                }
            }
            //5. Execute the transaction repeat times
            int k = 0;
            int sl = 0;
            do {
                do{
                    //2. Open the connection
                    con = new TCPMasterConnection(addr);
                    con.setPort(port+sl);
                    System.out.println("Connected to " + addr.toString() + ":" + con.getPort());
                    con.connect();

                    //3. Prepare the request
                    req = new ReadInputDiscretesRequest(ref, count);
                    //ReadCoilsRequest req = new ReadCoilsRequest(ref, count);
                    req.setUnitID(15);
                    //4. Prepare the transaction
                    trans = new ModbusTCPTransaction(con);
                    trans.setRequest(req);
                    trans.setReconnecting(false);

                    System.out.println("Request: " + req.getHexMessage());
                    trans.execute();

                    res = (ReadInputDiscretesResponse) trans.getResponse();
                    System.out.println("Response: " + res.getHexMessage() );
                    System.out.println("Digital Inputs Status=" + res.getDiscretes().toString());
                    String sSignals=res.getDiscretes().toString();
                    objReghuTCPmaster.sendPost(sSignals,sl);

                    sl++;
                    //6. Close the connection
                    con.close();
                    Thread.sleep(200);
                } while (sl < slaves);
                Thread.sleep(10000-(2*sl));
                sl=0;
                k++;

            } while (k < repeat);

        } catch (Exception ex) {
            System.out.println("Exception " + ex.toString());
            ex.printStackTrace();
        }
    }//main

    private static void printUsage() {
        System.out.println(
                "Command LIne Arguments required <address{:<port>} [String]> <register [int16]> <bitcount [int16]> {<noOfSlaves [int]>} {<repeat [int]>}"
        );
    }//printUsage



    private void sendPost(String sSignals, int iSlave) throws Exception {

        // form parameters
        Map<Object, Object> data = new HashMap<>();
//        data.put("username", "abc");
//        data.put("password", "123");
        data.put("masterid", "101");
        data.put("slaveid", Integer.toString(15+iSlave));
        data.put("signals", sSignals);
        data.put("ts", System.currentTimeMillis());

        HttpRequest request = HttpRequest.newBuilder()
                .POST(buildFormDataFromMap(data))
                .uri(URI.create(URL_MBPOST))
                .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
//        System.out.println(response.statusCode());

        // print response body
//        System.out.println(response.body());

    }

    private static HttpRequest.BodyPublisher buildFormDataFromMap(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }

        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

}//class MBtcpmaster
