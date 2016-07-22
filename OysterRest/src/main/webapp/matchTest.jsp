<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
		<title>Fact of Death Verification Service Test</title>
		<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
		<style>
			table, tr {
    			border: 1px solid black;
    			text-align: left;
    			vertical-align: top;
    			padding-left: 5px;
    			padding-right: 5px;
			}
			#response {
				font-family: "Courier New", Courier, monospace;
				font-size:22px;
			}
		</style>
</head>
<body style="font-family: verdana;">
<script>
	$(document).ready(function() {

		$("#submitMatch").click(function() {
			clearResponse();
			$.ajax({
				  contentType: 'application/json',
				  dataType: "json",
				  url: "/OysterRest/oyster/rest/get/deceased",
				  data: {qs : getDataString()},
				  success: function(data) {
				    $("#response").html(JSON.stringify(data));
				    $("#response").css("color", "green");
				  },
			      error: function (xhr, ajaxOptions, thrownError) {
				      $("#response").html(xhr.responseText);
				      $("#response").css("color", "red");
			        }
			});
		});

		$("#resetResponse").click(function() {
			clearResponse();
                });
 
    		$('#datatable tr').click(function() {
        		var href = $(this).find("a").attr("href");
        		if(href) {
				var $tds = $(this).find('td');
				$('#firstName').val($tds.eq(1).text());
				$('#middleName').val($tds.eq(2).text());
				$('#lastName').val($tds.eq(3).text());
				$('#dob').val($tds.eq(4).text());
				clearResponse();

				var gender = $tds.eq(5).text();
				if (gender == "Female") {
					$('input:radio[name=gender]')[0].checked = true;
				} else {
					$('input:radio[name=gender]')[1].checked = true;
				}
        		}
			return false;
    		});
	})
	
	function clearResponse() {
		$("#response").html("");
	}

	function createJsonString() {
		var value = $("#request").val();
		var obj = JSON.parse($("#request").val());
		var jsonString = JSON.stringify(obj);
		return jsonString;
	}

	function getDataString() {
    		var data_params = {
            		"firstName": $('#firstName').val(),
            		"middleName": $('#middleName').val(),
            		"lastName": $('#lastName').val(),
            		"dob": $('#dob').val(),
            		"gender": $('input[name=gender]:checked', '#matchForm').val()
       		 };
        	return JSON.stringify(data_params);
	}  	
	
</script>

<p>This is a RESTful web service that verifies if the data submitted matches a decedent.<br/>
To make sure the testing surfaces any performance issues with repeated high-volume use,
we have implemented this test page using a dataset of 3 million synthetic records with gender correct names.<br/>
The Fact of Death Verification Service Match system is made available in order that you can develop
modules that correctly submit queries and can process the resulting JSON response.</p>
<p>Please contact the Pathology Information Department at 916-734-1620, or email mahogarth@ucdavis.edu for further information.</p>

<h2>Fact of Death Verification Service Test</h2>

<table>
<tbody><tr>
	<td style="width:50%;">
		<form id="matchForm">
		<table>
			<tbody><tr>
				<th>First Name</th>
				<td><input type="text" size="30" id="firstName"></td>
			</tr>
			<tr>
				<th>Middle Name</th>
				<td><input type="text" size="30" id="middleName"></td>
			</tr>
			<tr>
				<th>Last Name</th>
				<td><input type="text" size="30" id="lastName"></td>
			</tr>
			<tr>
				<th>Date Of Birth (mm/dd/yyyy):</th>
				<td><input type="text" size="10" id="dob"></td>
			</tr>
			<tr>
				<th>Gender:</th>
				<td>
					<input type="radio" id="gender" name="gender" value="F" checked="checked">Female
					<input type="radio" id="gender" name="gender" value="M">Male<br>
				</td>
			</tr>
		</tbody></table>
		<div>
			<button id="submitMatch" type="button">Submit and Match</button>
			<button id="resetResponse" type="button">Clear Response Box</button>
		</div>
		</form>
	</td>
	<td rowspan="2" align="right" style="width:50%;">
		<table id="datatable">
		<caption>Sample Data (click link to test)</caption>
		 <tbody>
		<tr>
    			<th>&nbsp;</th>
    			<th>First Name:</th>
    			<th>Middle Name:</th>
    			<th>Last Name:</th>
    			<th>Date of Birth:</th>
    			<th>Gender:</th>
  		</tr>
  		<tr>
			<td><a href="#">Match</a></td>
  			<td>ERNA</td>
  			<td>MARI</td>
  			<td>RANDALL</td>
  			<td>01/09/1979</td>
  			<td>Female</td>
  		</tr>
  		<tr>
			<td><a href="#">Match</a></td>
  			<td>CORINNE</td>
  			<td>JODY</td>
  			<td>STEPHENSON</td>
  			<td>11/12/1951</td>
  			<td>Female</td>
  		</tr>
  		<tr>
			<td><a href="#">Match</a></td>
  			<td>MARJORIE</td>
  			<td>KERRI</td>
  			<td>GUTIERREZ</td>
  			<td>10/12/1976</td>
  			<td>Female</td>
  		</tr>
  		<tr>
			<td><a href="#">Match</a></td>
  			<td>OLIVIA</td>
  			<td>CINDY</td>
  			<td>MEYERS</td>
  			<td>10/23/1956</td>
  			<td>Female</td>
  		</tr>
  		<tr>
			<td><a href="#">Match</a></td>
  			<td>CLIFTON</td>
  			<td>NATHAN</td>
  			<td>BIRD</td>
  			<td>07/11/1938</td>
  			<td>Male</td>
  		</tr>

  		<tr>
			<td><a href="#">Match</a></td>
  			<td>MARC</td>
  			<td>CALVIN</td>
  			<td>LINDSEY</td>
  			<td>07/22/1946</td>
  			<td>Male</td>
  		</tr>
  		<tr>
			<td><a href="#">Match</a></td>
  			<td>ALVIN</td>
  			<td>JASON</td>
  			<td>NORTON</td>
  			<td>10/17/1924</td>
  			<td>Male</td>
  		</tr>
  		<tr>
			<td><a href="#">Match</a></td>
  			<td>TONY</td>
  			<td>ALFRED</td>
  			<td>MAXWELL</td>
  			<td>08/13/1939</td>
  			<td>Male</td>
  		</tr>
  		<tr>
			<td><a href="#">Match</a></td>
  			<td>TONIA</td>
  			<td>MORGAN</td>
  			<td>COLLINS</td>
  			<td>04/09/1950</td>
  			<td>Female</td>
  		</tr>
  		<tr>
			<td><a href="#">Match</a></td>
  			<td>LYDIA</td>
  			<td>MAXINE</td>
  			<td>DONALDSON</td>
  			<td>07/01/1927</td>
  			<td>Female</td>
  		</tr>
		</tbody></table>
	</td>
</tr>
<tr>	
	<td style="width:50%;">
	<h3>JSON Match Response:</h3>
	<div>
		<textarea id="response" name="response" rows="5" cols="50" disabled="disabled"></textarea>
	</div>
	</td>
</tr>	
	
</tbody></table>	


</body></html>
