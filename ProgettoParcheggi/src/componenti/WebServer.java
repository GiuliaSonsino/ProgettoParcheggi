package componenti;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

import database.CreateTables;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class WebServer {
    ParkingManager pm = new ParkingManager();
    static CreateTables ct = new CreateTables();
    static MqttClients mqttClients = new MqttClients();
    static int count=0;
    static ArrayList<String> idSeq;
    static ExecutorService executorService;
    public static void main(String[] args) throws IOException, MqttException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
         
    	
    	//genero un thread pool di 10 thread
    	 executorService = Executors.newFixedThreadPool(10);
    	
    	//connessione https
    	 char[] keystorePassword = "123456".toCharArray();
         String keystorePath = "src/componenti/keystore.jks";

         KeyStore keyStore = KeyStore.getInstance("JKS");
         InputStream keystoreInputStream = new FileInputStream(keystorePath);
         keyStore.load(keystoreInputStream, keystorePassword);

         KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
         keyManagerFactory.init(keyStore, keystorePassword);

         SSLContext sslContext = SSLContext.getInstance("TLS");
         sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

         // Creo il server HTTPS
         HttpsServer server = HttpsServer.create(new InetSocketAddress(8443), 0);
         server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
             public void configure(HttpsParameters params) {
                 try {
                     SSLContext context = getSSLContext();
                     SSLEngine engine = context.createSSLEngine();
                     params.setNeedClientAuth(false);
                     params.setCipherSuites(engine.getEnabledCipherSuites());
                     params.setProtocols(engine.getEnabledProtocols());

                     SSLParameters sslParameters = context.getDefaultSSLParameters();
                     params.setSSLParameters(sslParameters);
                 } catch (Exception ex) {
                     ex.printStackTrace();
                 }
             }
         });

         // Gestisco le richieste
         server.createContext("/", new MyHttpHandler());
         server.createContext("/statoParcheggi", new MyHttpHandler());
         server.createContext("/opzioniAmministratore", new MyHttpHandler());
         server.createContext("/cambiaAttivitaParcheggi", new MyHttpHandler());
         server.createContext("/aggiungieliminaParcheggio", new MyHttpHandler());
         server.createContext("/areaUtente", new MyHttpHandler());
         server.createContext("/pagamento", new MyHttpHandler());
         server.createContext("/uscitaParcheggio", new MyHttpHandler());
         server.setExecutor(null); 

         // Avvio il server
         server.start();
         System.out.println("Server web in esecuzione su https://localhost:8443"  );
    	

        
        
        idSeq=new ArrayList<String>();
        List<Parcheggio> parcheggi = ct.getParcheggi();
        
        
        
        
       
        //per ogni parcheggio iscrivo l'MqttClient globale ai relativi topic 
        for( Parcheggio parcheggio: parcheggi) {
            int parkingId = parcheggio.getId();
            idSeq.add(String.valueOf(parkingId));
			mqttClients.subscribeTopic(parkingId,count,idSeq); 
			count=count+1;
        }
        
       

    }

    static class MyHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();

            if ("/".equals(requestPath)) {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Gestisco la richiesta POST
                    try {
						handlePostRequest(exchange);
					} catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException
							| CertificateException | IOException | MqttException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                } else {
                    // Gestisco la richiesta GET
                    handleGetRequest(exchange);
                }
            }
            else if ("/opzioniAmministratore".equals(requestPath)) {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Gestisco la richiesta POST
                    handlePostRequestOpzioniAmministratore(exchange);
                } else {
                    // Gestisco la richiesta GET
                    handleGetRequestOpzioniAmministratore(exchange);
                }
            }
            else if ("/cambiaAttivitaParcheggi".equals(requestPath)) {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Gestisco la richiesta POST
                    handlePostRequestCambiaAttivita(exchange);
                } else {
                    // Gestisco la richiesta GET
                    handleGetRequestCambiaAttivita(exchange);
                }
            }
            else if ("/aggiungieliminaParcheggio".equals(requestPath)) {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Gestisco la richiesta POST
                    handlePostRequestEliminaParcheggio(exchange);
                    try {
						handlePostRequest(exchange);
					} catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException
							| CertificateException | IOException | MqttException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
                } else {
                    // Gestisco la richiesta GET
                    handleGetRequestEliminaParcheggio(exchange);
                    
                }
            }
            else if ("/areaUtente".equals(requestPath)) {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Gestisco la richiesta POST
                    try {
						handlePostRequestAreaUtente(exchange);
					} catch (IOException | MqttException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                } else {
                    // Gestisco la richiesta GET
                    handleGetRequestAreaUtente(exchange);
                    
                }
            }
            else if ("/pagamento".equals(requestPath)) {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Gestisco la richiesta POST
						handlePostRequestPagamento(exchange);
					
                } else {
                    // Gestisco la richiesta GET
                    handleGetRequestPagamento(exchange);
                    
                }
            }
            else if ("/uscitaParcheggio".equals(requestPath)) {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Gestisco la richiesta POST
						try {
							handlePostRequestUscita(exchange);
						} catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException
								| CertificateException | IOException | MqttException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					
                } else {
                    // Gestisco la richiesta GET
                    handleGetRequestUscita(exchange);
                    
                }
            }
            else {
                String response = "Percorso non valido";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }

        

        //per visualizzare pagina home
		private void handleGetRequest(HttpExchange exchange) throws IOException {
        	String filePath = "src/main/webapp/home.html";
            byte[] responseBytes = getFileBytes(filePath);
      
            if (responseBytes != null) {
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(responseBytes);
                outputStream.close();
            } else {
                String response = "File non trovato";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }

		//post relativa all'aggiunta di un nuovo parcheggio
        private void handlePostRequest(HttpExchange exchange) throws IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, MqttException {
            // Codice per gestire la richiesta POST
            InputStream requestBody = exchange.getRequestBody();

            InputStreamReader reader = new InputStreamReader(requestBody, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(reader);
            String line;
            StringBuilder requestBodyBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            String requestBodyString = requestBodyBuilder.toString();

            try {
                JSONObject json = new JSONObject(requestBodyString);
                String quartiere = json.getString("quartiere");
                String via = json.getString("via");
                int postiTotali = json.getInt("postiTotali");
                int postiLiberi = json.getInt("postiLiberi");
                int statoAttivita = json.getInt("statoAttivita");
                
                
                ParkingManager parkingManager = new ParkingManager();
                parkingManager.addNuovoParcheggio2(quartiere, via, postiTotali, postiLiberi, statoAttivita);
                List<Parcheggio> parcheggi=ct.getParcheggi();
                int dim= parcheggi.size();
                Parcheggio ultimo= parcheggi.get(dim-1);
                idSeq.add(String.valueOf(ultimo.getId()));
                mqttClients.subscribeTopic(ultimo.getId(),count, idSeq);

                
                String response = "Richiesta POST elaborata con successo";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            } catch (JSONException e) {
              
                e.printStackTrace();
                String response = "Errore durante l'elaborazione della richiesta POST";
                exchange.sendResponseHeaders(500, response.length());
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }
        
        
      //get opzioni amministratore
        private void handleGetRequestOpzioniAmministratore(HttpExchange exchange) throws IOException {
        	String filePath ="src/main/webapp/opzioniAmministratore.html";
            byte[] responseBytes = getFileBytes(filePath);

            if (responseBytes != null) {
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(responseBytes);
                outputStream.close();
            } else {
                String response = "File non trovato";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }

         private void handlePostRequestOpzioniAmministratore(HttpExchange exchange) throws IOException {
         }
         
         //get cambio attività parcheggi
         private void handleGetRequestCambiaAttivita(HttpExchange exchange)  throws IOException {
             String filePath = "src/main/webapp/cambiaAttivitaParcheggi.html";
             byte[] responseBytes = getFileBytes(filePath);

             if (responseBytes != null) {
                 // Converte il contenuto in una stringa
                 String responseHtml = new String(responseBytes, StandardCharsets.UTF_8);

                
                 List<Parcheggio> parcheggi = ct.getParcheggi(); 
                 // Genera il codice HTML con i dati dinamici
                 StringBuilder dynamicHtml = new StringBuilder();
                 dynamicHtml.append("<table>");
                 dynamicHtml.append("<tr><th>Codice Parcheggio</th><th>Quartiere</th><th>Via</th><th>Posti Totali</th><th>Posti Liberi</th><th>STATO ATTUALE</th><th> </th><th> </th></tr>");
                 int countParc= 0;

                 for (Parcheggio parcheggio : parcheggi) {
                	 int id = parcheggio.getId();
                     String quartiere = parcheggio.getQuartiere();
                     String via = parcheggio.getVia();
                     int postiTotali = parcheggio.getPostiTotali();
                     int postiLiberi = parcheggio.getPostiLiberi();
                     Boolean attivo = parcheggio.getAttivita();
                     String stato="";
                     if(attivo) {
                    	 stato="Attivo";
                     }else {
                    	 stato ="Disattivo";
                     }

                     dynamicHtml.append("<tr>");
                     dynamicHtml.append("<td>").append(id).append("</td>");
                     dynamicHtml.append("<td>").append(quartiere).append("</td>");
                     dynamicHtml.append("<td>").append(via).append("</td>");
                     dynamicHtml.append("<td>").append(postiTotali).append("</td>");
                     dynamicHtml.append("<td>").append(postiLiberi).append("</td>");
                     dynamicHtml.append("<td>").append(stato).append("</td>");
                     dynamicHtml.append("<td><button name=\"attivaBtn\" id=\"cambiaBtn_" + countParc + "\" type=\"button\" onclick=\"attivaParcheggio('\" + id + \"')\">ATTIVA</button></td>"); 
                     dynamicHtml.append("<td><button name=\"disattivaBtn\" id=\"cambiaBtn_" + countParc + "\" type=\"button\" onclick=\"disattivaParcheggio('\" + id + \"')\">DISATTIVA</button></td>"); 
                     //dynamicHtml.append("<td><button name=\"attivaBtn_" + countParc + "\" id=\"cambiaBtn_" + countParc + "\" type=\"button\" onclick=\"attivaParcheggio('" + id + "')\">ATTIVA</button></td>"); 
                     //dynamicHtml.append("<td><button name=\"disattivaBtn_" + countParc + "\" id=\"cambiaBtn_" + countParc + "\" type=\"button\" onclick=\"disattivaParcheggio('" + id + "')\">DISATTIVA</button></td>");
                     dynamicHtml.append("</tr>");
                     
                     countParc = countParc +1;
                 }
                 

                 dynamicHtml.append("</table>");

                 // Inserisce i dati dinamici nel file HTML
                 responseHtml = responseHtml.replace("{{DYNAMIC_CONTENT}}", dynamicHtml.toString());

                 exchange.getResponseHeaders().set("Content-Type", "text/html");

                 exchange.sendResponseHeaders(200, responseHtml.getBytes().length);
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(responseHtml.getBytes());
                 outputStream.close();
             } else {
                 String response = "File non trovato";
                 exchange.sendResponseHeaders(404, response.length());
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(response.getBytes());
                 outputStream.close();
             }
         }

         //post cambia attività
 		private void handlePostRequestCambiaAttivita(HttpExchange exchange) throws IOException {
 			System.out.println("post di cambia attivita");
            InputStream requestBody = exchange.getRequestBody();
            System.out.println("gestisco la post");
            InputStreamReader reader = new InputStreamReader(requestBody, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(reader);
            String line;
            StringBuilder requestBodyBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            String requestBodyString = requestBodyBuilder.toString();

            try {
                JSONObject json = new JSONObject(requestBodyString);
                String stato =json.getString("stato");
                String id = json.getString("id");
                
                ParkingManager parkingManager = new ParkingManager();
                if(stato.equals("attiva")) {
                	parkingManager.attivaParcheggio(id);
                } else if(stato.equals("disattiva")) {
                	parkingManager.disattivaParcheggio(id);
                }
                
                
                String response = "Richiesta POST elaborata con successo";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            } catch (JSONException e) {
                // Gestione dell'errore JSON
                e.printStackTrace();
                String response = "Errore durante l'elaborazione della richiesta POST";
                exchange.sendResponseHeaders(500, response.length());
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
 			
 		}
 		
 		//get per eliminazione o aggiunta parcheggio
 		 private void handleGetRequestEliminaParcheggio(HttpExchange exchange) throws IOException {
             String filePath = "src/main/webapp/aggiungieliminaParcheggio.html";
             byte[] responseBytes = getFileBytes(filePath);

             if (responseBytes != null) {
                 // Converte il contenuto in una stringa
                 String responseHtml = new String(responseBytes, StandardCharsets.UTF_8);

                 // Recupera i dati dal database
                 List<Parcheggio> parcheggi = ct.getParcheggi(); 

              // Genera il codice HTML con i dati dinamici
                 StringBuilder dynamicHtml = new StringBuilder();
                 dynamicHtml.append("<table>");
                 dynamicHtml.append("<tr><th>Codice Parcheggio</th><th>Quartiere</th><th>Via</th><th>Posti Totali</th><th>Posti Liberi</th><th>STATO ATTUALE</th><th> </th></tr>");
                 int countParc= 0;

                 for (Parcheggio parcheggio : parcheggi) {
                	 int id = parcheggio.getId();
                     String quartiere = parcheggio.getQuartiere();
                     String via = parcheggio.getVia();
                     int postiTotali = parcheggio.getPostiTotali();
                     int postiLiberi = parcheggio.getPostiLiberi();
                     Boolean attivo = parcheggio.getAttivita();
                     String stato="";
                     if(attivo) {
                    	 stato="Attivo";
                     }else {
                    	 stato ="Disattivo";
                     }

                     dynamicHtml.append("<tr>");
                     dynamicHtml.append("<td>").append(id).append("</td>");
                     dynamicHtml.append("<td>").append(quartiere).append("</td>");
                     dynamicHtml.append("<td>").append(via).append("</td>");
                     dynamicHtml.append("<td>").append(postiTotali).append("</td>");
                     dynamicHtml.append("<td>").append(postiLiberi).append("</td>");
                     dynamicHtml.append("<td>").append(stato).append("</td>"); 
                     dynamicHtml.append("<td><button name=\"eliminaBtn\" id=\"eliminaBtn_" + countParc + "\" type=\"button\" onclick=\"eliminaParcheggio('\" + id + \"')\">ELIMINA</button></td>"); 
                     //dynamicHtml.append("<td><button name=\"attivaBtn_" + countParc + "\" id=\"cambiaBtn_" + countParc + "\" type=\"button\" onclick=\"attivaParcheggio('" + id + "')\">ATTIVA</button></td>"); 
                     //dynamicHtml.append("<td><button name=\"disattivaBtn_" + countParc + "\" id=\"cambiaBtn_" + countParc + "\" type=\"button\" onclick=\"disattivaParcheggio('" + id + "')\">DISATTIVA</button></td>");
                     dynamicHtml.append("</tr>");
                     
                     countParc = countParc +1;
                 }
                 

                 dynamicHtml.append("</table>");


                 // Inserisce i dati dinamici nel file HTML
                 responseHtml = responseHtml.replace("{{DYNAMIC_CONTENT}}", dynamicHtml.toString());

                 exchange.getResponseHeaders().set("Content-Type", "text/html");

             
                 exchange.sendResponseHeaders(200, responseHtml.getBytes().length);
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(responseHtml.getBytes());
                 outputStream.close();
             } else {
                 String response = "File non trovato";
                 exchange.sendResponseHeaders(404, response.length());
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(response.getBytes());
                 outputStream.close();
             }
         }


         //post per eliminazione parcheggio
         private void handlePostRequestEliminaParcheggio(HttpExchange exchange) throws IOException {
             // Codice per gestire la richiesta POST
             InputStream requestBody = exchange.getRequestBody();

             InputStreamReader reader = new InputStreamReader(requestBody, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader);
             String line;
             StringBuilder requestBodyBuilder = new StringBuilder();
             while ((line = br.readLine()) != null) {
                 requestBodyBuilder.append(line);
             }
             String requestBodyString = requestBodyBuilder.toString();

             try {
                 JSONObject json = new JSONObject(requestBodyString);
                 String id = json.getString("id");
                 
                 ParkingManager parkingManager = new ParkingManager();
                 parkingManager.deleteParcheggio(id);
                 
              
                 String response = "Richiesta POST elaborata con successo";
                 exchange.sendResponseHeaders(200, response.length());
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(response.getBytes());
                 outputStream.close();
             } catch (JSONException e) {
                 // Gestione dell'errore JSON
                 e.printStackTrace();
                 String response = "Errore durante l'elaborazione della richiesta POST";
                 exchange.sendResponseHeaders(500, response.length());
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(response.getBytes());
                 outputStream.close();
             }
         }
         
         
         //get area utente
         private void handleGetRequestAreaUtente(HttpExchange exchange) throws IOException {
             String filePath = "src/main/webapp/areaUtente.html";
             byte[] responseBytes = getFileBytes(filePath);

             if (responseBytes != null) {
               
                 String responseHtml = new String(responseBytes, StandardCharsets.UTF_8);
                 List<Parcheggio> parcheggi = ct.getParcheggi();
              
                 StringBuilder dynamicHtml = new StringBuilder();
                 dynamicHtml.append("<table>");
                 dynamicHtml.append("<tr><th>Codice Parcheggio</th><th>Quartiere</th><th>Via</th><th>Posti Totali</th><th>Posti Liberi</th><th>STATO ATTUALE</th><th> </th><th> </th></tr>");
                 int countParc= 0;

                 for (Parcheggio parcheggio : parcheggi) {
                	 int id = parcheggio.getId();
                     String quartiere = parcheggio.getQuartiere();
                     String via = parcheggio.getVia();
                     int postiTotali = parcheggio.getPostiTotali();
                     int postiLiberi = parcheggio.getPostiLiberi();
                     Boolean attivo = parcheggio.getAttivita();
                     String stato="";
                     if(attivo) {
                    	 stato="Attivo";
                     }else {
                    	 stato ="Disattivo";
                     }

                     dynamicHtml.append("<tr>");
                     dynamicHtml.append("<td>").append(id).append("</td>");
                     dynamicHtml.append("<td>").append(quartiere).append("</td>");
                     dynamicHtml.append("<td>").append(via).append("</td>");
                     dynamicHtml.append("<td>").append(postiTotali).append("</td>");
                     dynamicHtml.append("<td>").append(postiLiberi).append("</td>");
                     dynamicHtml.append("<td>").append(stato).append("</td>"); 
                     
                     if (attivo && postiLiberi>0) {
                    	    //dynamicHtml.append("<td><button name=\"entraBtn\" id=\"entraBtn_" + countParc + "\" type=\"button\" onclick=\"entraParcheggio('" + id + "')\">ENTRA</button></td>");
                            dynamicHtml.append("<td><button name=\"entraBtn\" id=\"entraBtn_" + countParc + "\" type=\"button\" onclick=\"entraParcheggio('\" + id + \"')\">ENTRA</button></td>"); 
                            dynamicHtml.append("<td><button name=\"esciBtn\" id=\"esciBtn_" + countParc + "\"  type=\"button\" onclick=\"apriDialog()\">ESCI</button></td>"); 

                    	    countParc = countParc +1;
                    	    
                    	} else {
                    	    //dynamicHtml.append("<td><button name=\"entraBtn\" id=\"entraBtn_" + countParc + "\" type=\"button\" onclick=\"entraParcheggio('" + id + "')\" style=\"display: none;\">ENTRA</button></td>");
                            dynamicHtml.append("<td><button name=\"entraBtn\" id=\"entraBtn_" + countParc + "\" type=\"button\" onclick=\"entraParcheggio('\" + id + \"')\" style=\"display: none;\">ENTRA</button></td>"); 
                            dynamicHtml.append("<td><button name=\"esciBtn\" id=\"esciBtn_" + countParc + "\"  type=\"button\" onclick=\"apriDialog()\">ESCI</button></td>"); 

                    	    countParc = countParc +1;
                    	}

                     dynamicHtml.append("</tr>");
                    
                 }
                 

                 dynamicHtml.append("</table>");
                
                 responseHtml = responseHtml.replace("{{DYNAMIC_CONTENT}}", dynamicHtml.toString());

                 exchange.getResponseHeaders().set("Content-Type", "text/html");

                 exchange.sendResponseHeaders(200, responseHtml.getBytes().length);
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(responseHtml.getBytes());
                 outputStream.close();
             } else {
                 String response = "File non trovato";
                 exchange.sendResponseHeaders(404, response.length());
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(response.getBytes());
                 outputStream.close();
             }
         }


         //post area utente per entrare nel parcheggio
         private void handlePostRequestAreaUtente(HttpExchange exchange) throws IOException, MqttException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
             InputStream requestBody = exchange.getRequestBody();

             InputStreamReader reader = new InputStreamReader(requestBody, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader);
             String line;
             StringBuilder requestBodyBuilder = new StringBuilder();
             while ((line = br.readLine()) != null) {
                 requestBodyBuilder.append(line);
             }
             String requestBodyString = requestBodyBuilder.toString();

             try {
                 JSONObject json = new JSONObject(requestBodyString);
                 String id = json.getString("id");
                 int numTicket = json.getInt("numTicket");
                 ParkingManager parkingManager = new ParkingManager();
                 Parcheggio p = parkingManager.getParcheggioFromPosizone(id);
                 int idParc= p.getId();
                 
                 if(ct.decrementaParcheggiLiberi(idParc)) {
                	 //creazione ticket
                	 ct.createTicket2(numTicket, idParc);
                	 System.out.println(p.getId());
                     MqttClient client = null;
                     int idParch = p.getId();
                     String idParchString = String.valueOf(idParch);
                     
                     Ingresso ingr = new Ingresso(idParchString);
                	 executorService.submit(ingr);
                     }
                
                 
             } catch (JSONException e) {
                 // Gestione dell'errore JSON
                 e.printStackTrace();
                 String response = "Errore durante l'elaborazione della richiesta POST";
                 exchange.sendResponseHeaders(500, response.length());
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(response.getBytes());
                 outputStream.close();
             }
         }
         
         
         //post uscita
         private void handlePostRequestUscita(HttpExchange exchange) throws IOException, MqttException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
             // Codice per gestire la richiesta POST
             InputStream requestBody = exchange.getRequestBody();

             InputStreamReader reader = new InputStreamReader(requestBody, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader);
             String line;
             StringBuilder requestBodyBuilder = new StringBuilder();
             while ((line = br.readLine()) != null) {
                 requestBodyBuilder.append(line);
             }
             String requestBodyString = requestBodyBuilder.toString();

             try {
                 JSONObject json = new JSONObject(requestBodyString);
                 String posizionePark = json.getString("idParcheggio");
                 int numTicket = json.getInt("ticket");
                 boolean isPagato= ct.checkPagamento(numTicket);
                 ParkingManager parkingManager2 = new ParkingManager();
                 Parcheggio p2 = parkingManager2.getParcheggioFromPosizone(posizionePark);
                 int idParc2= p2.getId();
                 String idparcheggio= String.valueOf(idParc2);
                 int idParcheggioFromTicket = ct.idParcFromTicket(numTicket);
                 String idFromTicket = String.valueOf(idParcheggioFromTicket);
                 //se il ticket è stato pagato correttamente e se corrisponde al parcheggio in cui è stato effettivamente rilasciato 
                 if(isPagato && idFromTicket.equals(idparcheggio)) {
                	 ParkingManager parkingManager = new ParkingManager();
                     Parcheggio p = parkingManager.getParcheggioFromPosizone(posizionePark);
                     int idParc= p.getId();
                     //elimino ticket dalla lista dei ticket nel database
                     ct.deleteTicketById(numTicket);
                     ct.aumentaParcheggiLiberi(idParc);
                     String idP= String.valueOf(idParc);
                     Uscita uscita = new Uscita(idP,0);
                	 executorService.submit(uscita);
                	 }
                 //se il ticket non risulta pagato
                 else {
                	 ParkingManager parkingManager = new ParkingManager();
                     Parcheggio p = parkingManager.getParcheggioFromPosizone(posizionePark);
                     int idParc= p.getId();
                     String idP= String.valueOf(idParc);
                     Uscita uscita = new Uscita(idP,1);
                	 executorService.submit(uscita);
                	   }
                
             } catch (JSONException e) {
                 e.printStackTrace();
                 String response = "Errore durante l'elaborazione della richiesta POST";
                 exchange.sendResponseHeaders(500, response.length());
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(response.getBytes());
                 outputStream.close();
             }
         }
         
         
         private void handleGetRequestUscita(HttpExchange exchange) throws IOException {
         	String filePath = "src/main/webapp/areaUtente.html";
             byte[] responseBytes = getFileBytes(filePath);
       
             if (responseBytes != null) {
                 exchange.sendResponseHeaders(200, responseBytes.length);
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(responseBytes);
                 outputStream.close();
             } else {
                 String response = "File non trovato";
                 exchange.sendResponseHeaders(404, response.length());
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(response.getBytes());
                 outputStream.close();
             }
            
         }
         
         //get per pagina di pagamento ticket
         private void handleGetRequestPagamento(HttpExchange exchange) throws IOException {
         	String filePath = "src/main/webapp/pagamento.html";
             byte[] responseBytes = getFileBytes(filePath);
       
             if (responseBytes != null) {
                 exchange.sendResponseHeaders(200, responseBytes.length);
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(responseBytes);
                 outputStream.close();
             } else {
                 String response = "File non trovato";
                 exchange.sendResponseHeaders(404, response.length());
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(response.getBytes());
                 outputStream.close();
             }
            
         }
         
         //post per pagamento ticket
         private void handlePostRequestPagamento(HttpExchange exchange) throws IOException {
             InputStream requestBody = exchange.getRequestBody();

             InputStreamReader reader = new InputStreamReader(requestBody, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader);
             String line;
             StringBuilder requestBodyBuilder = new StringBuilder();
             while ((line = br.readLine()) != null) {
                 requestBodyBuilder.append(line);
             }
             String requestBodyString = requestBodyBuilder.toString();

             try {
                 JSONObject json = new JSONObject(requestBodyString);
                 int id = json.getInt("id");
                 ct.setParcheggioPagato(id);
                 int idParcheggio= ct.idParcFromTicket(id);
                 String idParc= String.valueOf(idParcheggio);
                 if(idParcheggio!=-1) {
                	 Cassa cassa = new Cassa(idParc);
                	 executorService.submit(cassa);
                 } 
                
             } catch (JSONException | MqttException e) {
                 e.printStackTrace();
                 String response = "Errore durante l'elaborazione della richiesta POST";
                 exchange.sendResponseHeaders(500, response.length());
                 OutputStream outputStream = exchange.getResponseBody();
                 outputStream.write(response.getBytes());
                 outputStream.close();
             }

            
         }



        private byte[] getFileBytes(String filePath) {
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    return Files.readAllBytes(path);
                } else {
                    System.out.println("Il file non esiste: " + filePath);
                }
            } catch (IOException e) {
                System.out.println("Si e' verificato un errore durante la lettura del file: " + e.getMessage());
            }
            return null;
        }
    }

}




