	
 <!DOCTYPE html>
 

  <html>
    <head>

	
	 <?php
 header("Access-Control-Allow-Origin: *");?>
      <!--Import Google Icon Font-->
      <link href="http://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
      <!--Import materialize.css-->
      <link type="text/css" rel="stylesheet" href="materialize/css/materialize.min.css"  media="screen,projection"/>

      <!--Let browser know website is optimized for mobile-->
      <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    </head>

    <body>
      <!--Import jQuery before materialize.js-->
      <script type="text/javascript" src="https://code.jquery.com/jquery-2.1.1.min.js"></script>
      <script type="text/javascript" src="materialize/js/materialize.min.js"></script>
	  <script src="addinput.js" language="Javascript" type="text/javascript"></script>


<nav>
    <div class="nav-wrapper">
      <a href="index.html" class="brand-logo">Local Search</a>
      <ul class="right hide-on-med-and-down">
        <li><a href="search.html"><i class="material-icons">search</i></a></li>
        <li><a href="setting.html"><i class="material-icons">settings</i></a></li>
        
      </ul>
    </div>
  </nav>

  <div class="container">
<br> </br>
<h2>SETTING </h2>
<br> </br>
<h5>    Indexing </h5>

<blockquote>
<h6>In order to Index your Documents.</h6><br>
First,   Please choose your Method to Index your Document. <br>
Second,   Enter Folder Path for Storing your Indexed Files.

</blockquote>
<br></br>
  
        

  <div class="row">
      <div class="col s7 push-s5">

<div class="row">
        <div class="input-field col s12">
          <input id="storage" type="text" class="validate" method >
          <label for="storage">Storage Location</label>
        </div>
<div class="input-field col s12">
          <input id="path" type="text" class="validate">
          <label for="path" >Path To Index</label>
        </div>
		<form method="POST">
     <div id="dynamicInput">
     </div>
     <t><a class="btn-floating btn-large waves-effect waves-light red" onClick="addInput('dynamicInput');" ><i class="material-icons">add</i></a>
	
  </button>
</form>
      </div>



</div>

      <div class="col s5 pull-s7">
  <div class="input-field col s12">
    <select>
      <option value="" disabled selected>Choose your option</option>
      <option value="1">Use Current Index</option>
      <option value="2">Re-Indexing (Reccomended for First time usage)</option>
     
    </select>
    <label>Indexing Selection</label>

	<button class="btn waves-effect waves-light" type="button"  onclick="getFormValue()">Submit
    <i class="material-icons right">send</i>
  </button>
  </div></div>
    </div>
      </div>
   
<div class="divider"></div>
    </body>
  </html>

<script>
 $(document).ready(function() {
    $('select').material_select();
  });

</script>



</html>


<h2>Using the XMLHttpRequest object</h2>

<button type="button" onclick="loadXMLDoc()">Change Content</button>
<p id="demo"></p>
	
<script>
function getFormValue(){

var path = document.getElementById("path").value;
alert("running")
alert(path);

}


/*


function loadXMLDoc(){
	alert("running");
	var data = new FormData();
data.append("path", "C://ice");

var xhr = new XMLHttpRequest();
xhr.withCredentials = true;

xhr.addEventListener("readystatechange", function () {
  if (this.readyState === 4) {
    console.log(this.responseText);
  }
});

xhr.open("POST", "http://localhost:8080/setIndexDir");
xhr.setRequestHeader("cache-control", "no-cache");
xhr.setRequestHeader("postman-token", "34e2d9bf-371d-8b25-8a6e-cee52d82b3a0");

xhr.send(data);


*/



	/*var path = "path=C%3A%2F%2Fice&=";
	var xhr = new XMLHttpRequest();
	xhr.withCredentials = true;

	xhr.addEventListener("readystatechange", function () {
	if (this.readyState === 4) {
		console.log(this.responseText);
	}
	});

	xhr.open("POST", "http://localhost:8080/setIndexDir");
	xhr.setRequestHeader("cache-control", "no-cache");
	xhr.setRequestHeader("postman-token", "1b159f9b-561c-1e99-dbba-a1c47d12035a");
	xhr.setRequestHeader("content-type", "application/x-www-form-urlencoded");

	xhr.send(path);*/





/*
  var xmlhttp;
  if (window.XMLHttpRequest) {
    xmlhttp = new XMLHttpRequest();
  } else {
    // code for older browsers
    xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
  }
  xmlhttp.onreadystatechange = function() {
    if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
      document.getElementById("demo").innerHTML =
      xmlhttp.responseText;
    }
  };
  xmlhttp.open("GET", "http://localhost:8080/status" );
  //xmlhttp.setRequestHeader("Content-Type","application/json");
  xmlhttp.send();*/




}




</script>
