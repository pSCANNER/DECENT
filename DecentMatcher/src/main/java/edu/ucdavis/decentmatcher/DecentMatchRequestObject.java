package edu.ucdavis.decentmatcher;

public class DecentMatchRequestObject
{
	private static final String DELIMITER = "|";
	
	private String firstName;
	private String middleName;
	private String lastName;
	private String akaFirst;
	private String akaMiddle;
	private String akaLast;
	
	private String dob;
	private Character gender;

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getMiddleName() {
		return middleName;
	}
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getAkaFirst() {
		return akaFirst;
	}
	public void setAkaFirst(String akaFirst) {
		this.akaFirst = akaFirst;
	}
	public String getAkaMiddle() {
		return akaMiddle;
	}
	public void setAkaMiddle(String akaMiddle) {
		this.akaMiddle = akaMiddle;
	}
	public String getAkaLast() {
		return akaLast;
	}
	public void setAkaLast(String akaLast) {
		this.akaLast = akaLast;
	}
	public String getDob() {
		return dob;
	}
	public void setDob(String dob) {
		this.dob = dob;
	}
	public Character getGender() {
		return gender;
	}
	public void setGender(Character gender) {
		this.gender = gender;
	}
	
	/**
	 * 	<Item Name="FileID" Attribute="@RefID" Pos="0"/>
     *  <Item Name="F1" Attribute="FirstName" Pos="1"/>
     *  <Item Name="M1" Attribute="MiddleName" Pos="2"/>
     *  <Item Name="L1" Attribute="LastName" Pos="3"/>
     *  <Item Name="CS1" Attribute="CS1" Pos="4"/>
     *  <Item Name="CS2" Attribute="CS2" Pos="5"/>
     *  <Item Name="AF1" Attribute="AkaFirst" Pos="6"/>
     *  <Item Name="AM1" Attribute="AkaMiddle" Pos="7"/>
     *  <Item Name="AL1" Attribute="AkaLast" Pos="8"/>
     *  <Item Name="DOB" Attribute="DOB" Pos="9"/>
     *  <Item Name="Gender" Attribute="Gender" Pos="10"/>
	 */
	@Override
	public String toString() {
		return 	DELIMITER +						//FileID
				this.firstName + DELIMITER +	//FirstName
				this.middleName + DELIMITER +	//MiddleName
				this.lastName + DELIMITER +		//LastName
				DELIMITER +						// CS1
				DELIMITER +						// CS2
				DELIMITER +						// AkaFirst
				DELIMITER +						// AkaMiddle
				DELIMITER +						// Akalast
				this.dob + DELIMITER +			// DOB
				this.gender;					// Gender
	}
}
