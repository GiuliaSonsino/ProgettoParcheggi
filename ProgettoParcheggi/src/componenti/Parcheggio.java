package componenti;



public class Parcheggio {
    private String quartiere;
    private String via;
    private int postiTotali;
    private int postiLiberi;
    private Boolean attivo;
    private int idMQTT;
   

    
    public Parcheggio(String quartiere,String via,int postiTotali, int postiLiberi, Boolean attivo, int idMQTT) {
        this.quartiere = quartiere;
        this.via=via;
        this.postiTotali = postiTotali;
        this.postiLiberi = postiLiberi;
        this.attivo = attivo;
        this.idMQTT= idMQTT;
       
    }

    public Parcheggio(String quartiere,String via,int postiTotali, int postiLiberi, Boolean attivo) {
        this.quartiere = quartiere;
        this.via=via;
        this.postiTotali = postiTotali;
        this.postiLiberi = postiLiberi;
        this.attivo = attivo;
        
       
    }

    



	public String getQuartiere() {
		
		return quartiere;
	}


	public void setAttivo(boolean b) {
		attivo = true;
		
	}



	public String getNome() {
		return quartiere;
	}



	public int getPostiLiberi() {
		return postiLiberi;
	}



	public String getVia() {
		return via;
	}



	public int getPostiTotali() {
		return postiTotali;
	}



	//1 è true, 0 false
	public Boolean getAttivita() {
		return attivo;
	}
	
	public int getId() {
		return idMQTT;
	}





}

