package componenti;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.JOptionPane;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;


import database.CreateTables;

public class MqttClients {
	CreateTables db = new CreateTables();
	private MqttClient client;
	
	
	
	
	public void subscribeTopic (int parkingId, int count, ArrayList<String> seq) throws MqttSecurityException, MqttException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, KeyStoreException, KeyManagementException {
		String id= String.valueOf(parkingId);
		if(count==0) {
			client = new MqttClient("ssl://test.mosquitto.org:8883","gestore");

	        MqttConnectOptions options = new MqttConnectOptions();

	        // Carico il truststore JKS
	        char[] truststorePassword = "abc123".toCharArray();
	        KeyStore truststore = KeyStore.getInstance("JKS");
	        truststore.load(new FileInputStream("src/componenti/truststore.jks"), truststorePassword);

	        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	        trustManagerFactory.init(truststore);

	        SSLContext sslContext = SSLContext.getInstance("TLS");
	        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

	        options.setSocketFactory(sslContext.getSocketFactory());

	        client.connect(options);
	        
	       
			
		}
		
        
		
		try {
		    client.subscribe("from/" + id + "/ingresso");
		    client.subscribe("from/" + id + "/uscita");
		    client.subscribe("from/" + id + "/cassa");

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connessione MQTT persa.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    for (String id : seq) {
                    	// Gestisco il messaggio arrivato sul topic
                        if (topic.equals("from/" + id + "/ingresso")) {
                        	
                           System.out.println("Apri transenne del parcheggio: " + id);
                            
            
                        } else if (topic.equals("from/" + id + "/uscita")) {
                        	if(payload.equals("Richiesta uscita negata")) {
                        		System.out.println("Uscita negata per parcheggio: " + id);
                        		
                        	} else if(payload.equals("Richiesta uscita approvata")) {
                        		System.out.println("Uscita approvata. Apri transenne del parcheggio: " + id);
                        	}
                        			
                           
                        } else if (topic.equals("from/" + id + "/cassa")) {
                        	 System.out.println("Pagamento in corso, emettere ticket pagato nel parcheggio: " + id);
                        	
                        }
                       
					}
                    
                    }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                   
                }
            });
        
		}   

           catch (MqttException e) {
            e.printStackTrace();
        }
	}
	
   

   
}
