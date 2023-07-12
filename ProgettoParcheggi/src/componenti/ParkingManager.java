package componenti;




import database.CreateTables;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;

import org.eclipse.paho.client.mqttv3.*;




public class ParkingManager {
    
    private static final String DB_URL = "jdbc:sqlite:src/database/DatabaseParcheggio.db";
    CreateTables db = new CreateTables();
 
    
    public ParkingManager() {
    	
    }

	public void addNuovoParcheggio2(String quartiere, String via, int postiTotali, int postiLiberi, int attivo) throws KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, MqttException, IOException {
		db.insertParcheggio2(quartiere, via, postiTotali, postiLiberi, attivo);	
	}
	

	public void deleteParcheggio(String posizione) { 
		List<Parcheggio> listaParc= db.getParcheggi();
		// Converti la stringa posizione in un numero intero
		int posId = Integer.parseInt(posizione);
		Parcheggio p=listaParc.get(posId);
		int id= p.getId();
	    db.deleteParcheggioById(id);
    }

	
	public Parcheggio getParcheggioFromPosizone(String idPosizione) {
		List<Parcheggio> listaParc= db.getParcheggi();
		// Converti la stringa posizione in un numero intero
		int posId = Integer.parseInt(idPosizione);
		Parcheggio p=listaParc.get(posId);
		return p;   
	}


	public void attivaParcheggio(String posizione) {
		List<Parcheggio> listaParc= db.getParcheggi();
		// Converti la stringa posizione in un numero intero
		int posId = Integer.parseInt(posizione);
		Parcheggio p=listaParc.get(posId);
		int id= p.getId();
		db.setParcheggioAttivo(id);
	}
	
	public void disattivaParcheggio(String posizione) {
		//db.setParcheggioDisattivo(idParcheggio);
		List<Parcheggio> listaParc= db.getParcheggi();
		// Converti la stringa posizione in un numero intero
		int posId = Integer.parseInt(posizione);
		Parcheggio p=listaParc.get(posId);
		int id= p.getId();
		db.setParcheggioDisattivo(id);
	}

	
}
