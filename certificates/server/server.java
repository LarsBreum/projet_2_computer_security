import java.io.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class server implements Runnable {
  private ServerSocket serverSocket = null;
  private static int numConnectedClients = 0;
  private List<Person> persons;
  private List<Journal> journals;
  private ActionAuthenticator authenticator = new ActionAuthenticator("./log.txt");
  
  public server(ServerSocket ss) throws IOException {
    serverSocket = ss;
    newListener();
    persons = new ArrayList<>();
    journals = new ArrayList<>();
  }
  // Initializes and loads persons into "database" (arraylist), and creates a few associations
  private void loadPersons(){
    //All the persons(instead of a database)
    Patient alice = new Patient("Alice", "0001011234", "Patient", "ER");
    Patient pat = new Patient("Pat", "12", "Patient", "RE");
    Nurse bob = new Nurse("Bob", "0102035678", "Nurse", "ER");
    Nurse bill = new Nurse("Bill", "4224", "Nurse", "RE");
    Doctor uncle = new Doctor("Uncle", "1234", "Doctor", "ER");
    Doctor phil = new Doctor("Phil", "9998979876", "Doctor", "RE");
    GovernmentRep gr = new GovernmentRep("gr", "0127630000", "GovernmentRep");
    
    persons.add(alice);
    persons.add(pat);
    persons.add(bob);
    persons.add(bill);
    persons.add(phil);
    persons.add(uncle);
    persons.add(gr);

    uncle.createAsso(alice); // Alice blir patient till uncle
    bob.addAsso(alice); // Bob blir Alice nurse
    uncle.createAsso(pat); // Pat blir uncles patient
    
  }
  
  // Handles client actions and requests to server
  private String actionHandler(String message, Person p, BufferedReader in, PrintWriter out){
    String[] words = message.split(" ");
    switch(words[0]){
      case "read":
        try{
          Patient recPatient = (Patient) persons.get(Integer.parseInt(words[1]));
          Journal reqJournal = recPatient.getJournal();
          if(reqJournal!=null && authenticator.canRead(p, recPatient)){
            return recPatient.getJournal().toString();
          }
          else{
            return "No reading access!";
          }
        }
        catch (NumberFormatException e){
          return "The second argument needs to be a number.";
        }
      case "write":
        try{
          Patient recPatient = (Patient) persons.get(Integer.parseInt(words[1]));
          String info = words[2];
          Journal reqJournal = recPatient.getJournal();
          if(reqJournal!=null && authenticator.canWrite(p, recPatient)){
            recPatient.getJournal().newEntry(info);
            return "You wrote to: " + recPatient.getName();
          }
          else{
            return "No writing access!";
          }
        }
        catch(NumberFormatException e){
          return "The second argument needs to be a number.";
        }
      case "delete":
        try{
          Patient recPatient = (Patient) persons.get(Integer.parseInt(words[1]));
          Journal reqJournal = recPatient.getJournal();
          if(reqJournal!=null && authenticator.canDelete(p, recPatient)){
            recPatient.getJournal().deleteAll();
            return "You deleted journal of " + recPatient;
          }
          else{
            return "No deleting access!";
          }
        }
        catch(NumberFormatException e) {
          return "The second argument needs to be a number.";
        }
      case "create":
      	try {
            Patient recPatient = (Patient) persons.get(Integer.parseInt(words[1]));
            Journal reqJournal = recPatient.getJournal();
            if(reqJournal!=null && authenticator.canCreate(p, recPatient)){
            	for(Person person : persons) {
            		if(person instanceof Nurse) {
            			if(person.getDivision().equals(recPatient.getDivision())) {
                          ((Nurse) person).addAsso(recPatient);
                          recPatient.getJournal().newEntry(words[2]);
                          return "You created new entry in journal of " + recPatient;
            			}
            		}
            	}
            	return "No access to create new entry!";
            }
            else{
              return "No access to create new entry!";
            }
      	}
        catch(NumberFormatException e) {
          return "The second argument needs to be a number.";
        }
    }
    return "Incorrect input";
  }
  
  public void run() {
    try {
      SSLSocket socket=(SSLSocket)serverSocket.accept();
      newListener();
      SSLSession session = socket.getSession();
      Certificate[] cert = session.getPeerCertificates();
      String subject = ((X509Certificate) cert[0]).getSubjectX500Principal().getName();
      String issuer = ((X509Certificate) cert[0]).getIssuerX500Principal().getName();
      String serial = ((X509Certificate) cert[0]).getSerialNumber().toString();
      numConnectedClients++;
      
      String subjectNameCN = subject.split(" ")[0]; // Client name from certificate
      String subjectName = subjectNameCN.substring(3, subjectNameCN.length()); // Client firstname
      
      int i = 0;
      boolean isFound = false;
      Person currentClient = null;
      loadPersons();
      
      while (!isFound && i<persons.size()) {  // Searches "database" for client match
        Person p = persons.get(i);
        if (p.getName().equals(subjectName)) {
          currentClient = p;
          isFound = true;
        }
        i++;
      }
      
      if (!isFound) { // Denies client access if not in "database"
        socket.close();
        numConnectedClients--;
        System.out.println("subjectName: " + subjectName);
        System.out.println("invalid login");
        //Här vill vi kasta ut klienten, just nu händer inget för klienten. Den får dock ingen åtkomst.
        return;
      }
      
      System.out.println("client connected");
      System.out.println("client name (cert subject DN field): " + subject);
      System.out.println("issuer: " + issuer);
      System.out.println("Serial: " + serial);
      System.out.println(numConnectedClients + " concurrent connection(s)\n");
      
      PrintWriter out = null;
      BufferedReader in = null;
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      
      String clientMsg = null;
      while ((clientMsg = in.readLine()) != null) {
        String response = actionHandler(clientMsg, currentClient, in, out); // Client response
        //String rev = new StringBuilder(clientMsg).reverse().toString();
        //System.out.println("received '" + clientMsg + "' from client");
        //System.out.print("sending '" + rev + "' to client...");
        out.println(response);
        out.flush();
        System.out.println("done\n");
      }
      in.close();
      out.close();
      socket.close();
      numConnectedClients--;
      System.out.println("client disconnected");
      System.out.println(numConnectedClients + " concurrent connection(s)\n");
    } catch (IOException e) {
      System.out.println("Client died: " + e.getMessage());
      e.printStackTrace();
      return;
    }
  }
  
  private void newListener() { (new Thread(this)).start(); } // calls run()
  public static void main(String args[]) {
    System.out.println("\nServer Started\n");
    int port = -1;
    if (args.length >= 1) {
      port = Integer.parseInt(args[0]);
    }
    String type = "TLSv1.2";
    try {
      ServerSocketFactory ssf = getServerSocketFactory(type);
      ServerSocket ss = ssf.createServerSocket(port);
      ((SSLServerSocket)ss).setNeedClientAuth(true); // enables client authentication
      new server(ss);
    } catch (IOException e) {
      System.out.println("Unable to start Server: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  private static ServerSocketFactory getServerSocketFactory(String type) {
    if (type.equals("TLSv1.2")) {
      SSLServerSocketFactory ssf = null;
      try { // set up key manager to perform server authentication
        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");
        char[] password = "password".toCharArray();
        // keystore password (storepass)
        ks.load(new FileInputStream("serverkeystore"), password);
        // truststore password (storepass)
        ts.load(new FileInputStream("servertruststore"), password);
        kmf.init(ks, password); // certificate password (keypass)
        tmf.init(ts);  // possible to use keystore as truststore here
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        ssf = ctx.getServerSocketFactory();
        return ssf;
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      return ServerSocketFactory.getDefault();
    }
    return null;
  }
}
