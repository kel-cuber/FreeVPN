package com.keldev.mavenproject1;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Mavenproject1 {

    private static final Logger LOGGER = Logger.getLogger(Mavenproject1.class.getName());
    private static JLabel statusLabel;
    private static JLabel ipLabel;
    private static JTextArea logArea;
    private static String lastKnownIP = "";

    public static void main(String[] args) {
        System.out.println("Hello World!");

        // Cria a interface gráfica e inicia a varredura de rede
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
            monitorNetworkConnectivity();
        });

        // Inicia o servidor VPN
        startVpnServer();
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Status da Rede");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        statusLabel = new JLabel("Verificando conectividade...");
        frame.add(statusLabel, BorderLayout.NORTH);

        ipLabel = new JLabel("IP atual: Verificando...");
        frame.add(ipLabel, BorderLayout.CENTER);

        logArea = new JTextArea();
        logArea.setEditable(false);
        frame.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static void monitorNetworkConnectivity() {
        Timer timer = new Timer(5000, e -> {
            boolean isNetworkAvailable = isNetworkAvailable();
            String statusText = isNetworkAvailable ? "Conectividade de rede disponível." : "Conectividade de rede não disponível.";
            statusLabel.setText(statusText);
            logArea.append(statusText + "\n");
            LOGGER.info(statusText);

            if (isNetworkAvailable) {
                List<String> connectedDevices = scanNetwork("192.168.1"); // Altere conforme necessário
                updateDeviceList(connectedDevices);
                checkExternalIP(); // Verifica mudança de IP
            }
        });
        timer.start();
    }

    private static void checkExternalIP() {
        try {
            URL url = new URL("https://api.ipify.org");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String currentIP = in.readLine();
            if (!currentIP.equals(lastKnownIP)) {
                String logText = "Endereço IP externo mudou: " + currentIP;
                LOGGER.info(logText);
                logArea.append(logText + "\n");
                lastKnownIP = currentIP;
                ipLabel.setText("IP atual: " + currentIP);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar o endereço IP externo", e);
            logArea.append("Erro ao verificar o endereço IP externo: " + e.getMessage() + "\n");
        }
    }

    private static void updateDeviceList(List<String> connectedDevices) {
        if (connectedDevices.isEmpty()) {
            String logText = "Nenhum dispositivo encontrado.";
            LOGGER.warning(logText);
            logArea.append(logText + "\n");
        } else {
            logArea.append("Dispositivos conectados:\n");
            LOGGER.info("Dispositivos conectados:");
            for (String device : connectedDevices) {
                String logText = "Dispositivo encontrado: " + device;
                LOGGER.info(logText);
                logArea.append(logText + "\n");
            }
        }
    }

    private static void startVpnServer() {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            LOGGER.info("Servidor VPN iniciado na porta 8080");
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        out.println(inputLine); // Encaminha a mensagem de volta ao cliente
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Erro ao comunicar com o cliente", e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao iniciar o servidor", e);
        }
    }

    public static boolean isNetworkAvailable() {
        try {
            // Pinga o servidor Google DNS para checar conectividade
            InetAddress address = InetAddress.getByName("8.8.8.8");
            return address.isReachable(2000); // Timeout em milissegundos
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar a conectividade de rede", e);
            return false;
        }
    }

    public static List<String> scanNetwork(String subnet) {
        List<String> devices = new ArrayList<>();
        for (int i = 1; i < 255; i++) {
            String host = subnet + "." + i;
            try {
                InetAddress address = InetAddress.getByName(host);
                if (address.isReachable(100)) {
                    String logText = "Dispositivo acessível: " + host;
                    LOGGER.info(logText);
                    logArea.append(logText + "\n");
                    devices.add(host);
                } else {
                    LOGGER.fine("Dispositivo não acessível: " + host);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Erro ao acessar o dispositivo: " + host, e);
                logArea.append("Erro ao acessar o dispositivo: " + host + " - " + e.getMessage() + "\n");
            }
        }
        return devices;
    }
}
