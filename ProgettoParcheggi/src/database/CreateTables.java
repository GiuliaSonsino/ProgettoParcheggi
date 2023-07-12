package database;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import componenti.MqttClients;
import componenti.Parcheggio;




public class CreateTables {
    private static final String DB_URL = "jdbc:sqlite:src/database/DatabaseParcheggio.db";
    static MqttClients mqttClients = new MqttClients();
    

    
    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement()) {
            createParcheggiTable(statement);
            tickets(statement);
            
            System.out.println("Tabelle create con successo.");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static void createParcheggiTable(Statement statement) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS parcheggi (" +
        		     "quartiere TEXT," + 
        		     "via TEXT," + 
        		     "postiTotali INT," +
        		     "postiLiberi INT," +
        		     "attivo INTEGER," +
                     "idMQTT INTEGER PRIMARY KEY" +
                     ")";
        statement.executeUpdate(sql);
    }
    
 
    private static void tickets(Statement statement) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS tickets (" +
        		     "numeroTicket INT PRIMARY KEY," + 
        		     "isPagato STRING," + 
        		     "idParcheggio INT" + 
                     ")";
        statement.executeUpdate(sql);
    }
    
  
    
    public void insertParcheggio2( String quartiere, String via, int postiTotali, int postiLiberi, int attivo) throws KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, MqttException, IOException  {
        String sql = "INSERT INTO parcheggi (quartiere, via, postiTotali, postiLiberi, attivo) " +
                     "VALUES (?, ?, ?, ?, ?)";
        Boolean stato=false;
        if(attivo==1) {
        	stato=true;
        }
        Parcheggio p = new Parcheggio(quartiere,via,postiTotali,postiLiberi,stato);
       
        try (Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement statement = connection.prepareStatement(sql)) {
         
            statement.setString(1, quartiere);
            statement.setString(2, via);
            statement.setInt(3, postiTotali);
            statement.setInt(4, postiLiberi);
            statement.setInt(5, attivo);
           
            
            statement.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    
    
    
    public List<Parcheggio> getParcheggi() {
        List<Parcheggio> parcheggi = new ArrayList<>();
        String sql = "SELECT * FROM parcheggi";

        try (Connection connection = DriverManager.getConnection(DB_URL);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                String quartiere = resultSet.getString("quartiere");
                String via = resultSet.getString("via");
                int postiTotali = resultSet.getInt("postiTotali");
                int postiLiberi = resultSet.getInt("postiLiberi");
                int attivo = resultSet.getInt("attivo");
                int idMQTT = resultSet.getInt("idMQTT");
                Boolean bool=false;
                if(attivo==1) {
                	bool=true;
                }
                
                

                Parcheggio parcheggio = new Parcheggio(quartiere, via, postiTotali, postiLiberi, bool, idMQTT);
                parcheggi.add(parcheggio);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return parcheggi;
    }

   
    
    public void deleteParcheggioById(int idMQTT) {
        String sql = "DELETE FROM parcheggi WHERE idMQTT = ?";
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, idMQTT);
            
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Parcheggio eliminato con successo.");
            } else {
                System.out.println("Nessun parcheggio trovato con l'ID MQTT specificato.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    
    public void setParcheggioAttivo(int idMQTT) {
        String sql = "UPDATE parcheggi SET attivo = 1 WHERE idMQTT = ?";
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, idMQTT);
            
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Stato del parcheggio aggiornato con successo.");
            } else {
                System.out.println("Nessun parcheggio trovato con l'ID MQTT specificato.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void setParcheggioDisattivo(int idMQTT) {
        String sql = "UPDATE parcheggi SET attivo = 0 WHERE idMQTT = ?";
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, idMQTT);
            
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Stato del parcheggio aggiornato con successo.");
            } else {
                System.out.println("Nessun parcheggio trovato con l'ID MQTT specificato.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void cambiaAttivitàParcheggio(int idMQTT) {
        String sql = "UPDATE parcheggi SET attivo = 0 WHERE idMQTT = ?";
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, idMQTT);
            
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Stato del parcheggio aggiornato con successo.");
            } else {
                System.out.println("Nessun parcheggio trovato con l'ID MQTT specificato.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    public void createTicket2(int numTicket, int idParcheggio)  {
        String sql = "INSERT INTO tickets (numeroTicket, isPagato, idParcheggio) " +
                     "VALUES (?, ?, ?)";
       
     
        try (Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement statement = connection.prepareStatement(sql)) {
         
            statement.setInt(1, numTicket);
            statement.setString(2, "no");
            statement.setInt(3, idParcheggio);
            statement.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
       
    }

    public boolean checkDisponibilitaParcheggio(int idMQTT) throws MqttException {
        String sql = "UPDATE parcheggi SET postiLiberi = postiLiberi - 1 WHERE idMQTT = ? AND postiLiberi >= 1";
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, idMQTT);
            
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            } else {
                System.out.println("Nessun parcheggio trovato con l'ID MQTT specificato o posti esauriti.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    
    
    public boolean checkPagamento(int ticket) throws MqttException {
        String sql = "SELECT isPagato FROM tickets WHERE numeroTicket = ?";
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, ticket);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String isPagato = resultSet.getString("isPagato");
                    return isPagato.equals("si");
                } else {
                    System.out.println("Nessun ticket trovato con il numero specificato.");
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    //restituisce l'id del parcheggio che ha rilasciato un certo ticket. Se non viene trovato alcun parcheggio si restituisce -1
    public int idParcFromTicket(int ticket) throws MqttException {
        String sql = "SELECT idParcheggio FROM tickets WHERE numeroTicket = ?";
        int idParcheggio = -1; // Valore di default se nessun parcheggio è trovato
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, ticket);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    idParcheggio = resultSet.getInt("idParcheggio");
                } else {
                    System.out.println("Nessun parcheggio trovato con il numeroTicket specificato.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return idParcheggio;
    }

    public void deleteTicketById(int numeroTicket) {
        String sql = "DELETE FROM tickets WHERE numeroTicket = ?";
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, numeroTicket);
            
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Ticket eliminato con successo.");
            } else {
                System.out.println("Nessun ticket trovato con l'ID  specificato.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    


    public boolean decrementaParcheggiLiberi(int idMQTT) throws MqttException {
        String sql = "UPDATE parcheggi SET postiLiberi = postiLiberi - 1 WHERE idMQTT = CAST(? AS INT)";
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            System.out.println("id da decrementare" + idMQTT);
            statement.setInt(1, idMQTT);
            
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            } else {
                
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean aumentaParcheggiLiberi(int idMQTT) throws MqttException {
    	System.out.println("id parch da aumentare: "+ idMQTT);
        String sql = "UPDATE parcheggi SET postiLiberi = postiLiberi + 1 WHERE idMQTT =?";
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idMQTT);
            
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            } else {
               
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    public void setParcheggioPagato(int numeroTicket) {
    	String si ="si";
        String sql = "UPDATE tickets SET isPagato = ? WHERE numeroTicket = ?";
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
        	statement.setString(1, si);
            statement.setInt(2, numeroTicket);
            
            
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Parcheggio Pagato.");
            } else {
                System.out.println("Problemi con pagamento");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}

