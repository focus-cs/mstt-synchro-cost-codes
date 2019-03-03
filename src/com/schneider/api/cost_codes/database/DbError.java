package com.schneider.api.cost_codes.database;

/**
 *
 * @author lahoudie
 */
public class DbError extends Exception
{
   private int numErreur;

    /** Creates a new instance of DbControllerError */
    public DbError() {
        super("Erreur: ");
    }

  public DbError(int pNumErreur) {
    super("Erreur: ");
    setNumErreur(pNumErreur);
  }

  public String toString(){
    return super.getMessage()+ getErreur();
  }

  public String getErreur(){
    switch(getNumErreur()){
      case 1: return "Code " + getNumErreur() + ": Ne trouve pas le fichier DbConfiguration.properties !";
      default: return " Erreur inconnue !";
    }
  }

    public int getNumErreur() {
        return numErreur;
    }

    public void setNumErreur(int numErreur) {
        this.numErreur = numErreur;
    }
}

