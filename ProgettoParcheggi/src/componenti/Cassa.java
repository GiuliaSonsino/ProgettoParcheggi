package componenti;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public class Cassa implements Runnable{
	private String idParch;
	

	public Cassa(String idParch) {
		this.idParch=idParch;
	
	}

	@Override
	public void run() {
		MqttClient client = null;
		try {
			String broker = "ssl://test.mosquitto.org:8883";
	          client = new MqttClient(broker,idParch);
	          MqttConnectOptions options = new MqttConnectOptions();

	          // Carica il truststore JKS
	          char[] truststorePassword = "abc123".toCharArray();
	          KeyStore truststore = KeyStore.getInstance("JKS");
	          truststore.load(new FileInputStream("src/componenti/truststore.jks"), truststorePassword);

	          TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	          trustManagerFactory.init(truststore);

	          SSLContext sslContext = SSLContext.getInstance("TLS");
	          sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

	          options.setSocketFactory(sslContext.getSocketFactory());

	          client.connect(options);
		} catch (MqttException | NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException | KeyManagementException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
	        

	      String topicCassa = "from/" + idParch + "/cassa";
	      try {
				client.subscribe(topicCassa);
			} catch (MqttException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
           MqttMessage responseMessageSucc = new MqttMessage("Pagamento in corso per ticket".getBytes());
           try {
        	   
    		  client.publish(topicCassa, responseMessageSucc);
			  client.disconnect();
        	   
				
			} catch (MqttPersistenceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      
	     
	      
	  }
	  
}
