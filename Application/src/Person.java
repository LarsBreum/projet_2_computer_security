import java.util.HashSet;

public class Person {
	String name;
	String ssn;
	String role;
	String division;
	HashSet<String> patients;
	
	public Person(String name, String ssn, String role, String division) {
		this.name = name;
		this.ssn = ssn;
		this.role = role;
		this.division = division;
		patients = new HashSet<>();
	}
	
	public String getName() {
		return name;
	}
	
	public String getSsn() {
		return ssn;
	}
	
	public String getRole() {
		return role;
	}
	
	public String getDivision() {
		return division;
	}
	
	public void addPatient(String p) {
		patients.add(p);
	}
	
	public boolean isRole(String r) {
		return role.equals(r);
	}
	
	public boolean isTreating(String p) {
		return patients.contains(p);
	}
	
	public boolean inDivision(String d) {
		return division.equals(d);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		Person p = (Person) o;
		return name.equals(p.getName());
	}
	
	public String toString() {
		//StringBuilder sb = new StringBuilder();
		return "Role: "+ role + "Name: " + name + ", ssn: " + ssn;
	}
	
}
