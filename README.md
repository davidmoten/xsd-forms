# xsd-forms

<img src="https://raw.github.com/davidmoten/xsd-forms/master/xsd-forms-generator/src/docs/diagram01.png"/>

You want to make a web form that submits structured data (XML/JSON). 

*xsd-forms* generates a web form for you based on an xml schema (XSD) that has been annotated with some presentation information. 

*xsd-forms* has been deployed to a freely available [web service](http://xsd-forms-generator.xuml-tools.cloudbees.net/) for your convenience and immediate testing.

The form is javascript/html only and performs all validation using javascript.

This project is written in Scala with a small amount of Java. 

**Status:** Released to Maven Central

Continuous integration with Jenkins: <a href="https://xuml-tools.ci.cloudbees.com/"><img src="https://xuml-tools.ci.cloudbees.com/job/xuml-tools/badge/icon"/></a>

## Design

Xml schemas contain nearly everything we need for many web forms except for presentation information. Instead of seeking full separation of presentation from the data model (the xml schema) there are significant advantages in [annotating the xsd itself](https://github.com/davidmoten/xsd-forms/blob/master/demo-scalaxb/src/main/resources/demo.xsd) according to a [special schema](https://github.com/davidmoten/xsd-forms/blob/master/xsd-scalaxb/src/main/xsd/xsd-forms.xsd) to indicate presentation details. For one, refactoring is much easier without specialized IDE tooling that can read both our xsd and our presentation format. 

**Primary Use Case**
  * A developer needs to create a web form. 
  * The developer creates an annotated schema as per xsd-forms and generates pure html and javascript (no server side scripting). 
  * The developer links the form to a server side component that processes the submitted xml. 

As the submitted xml is compliant with a schema, the xml can be unmarshalled by a tool (see [jaxb](http://www.oracle.com/technetwork/articles/javase/index-140168.html), [scalaxb](http://scalaxb.org), [xsd.exe](http://msdn.microsoft.com/en-us/library/x6c1kb0s.aspx) into schema generated objects. Consequently any change in the schema will be reflected by compilation errors in the processing code if a statically typed language is used (such as Java, Scala, C#, etc.).

The features of javascript libraries like [http://jquery.com](JQuery) mean that generating an html/javascript form that on submit posts xml that is compliant with the schema is a viable approach. The resultant css/html/javascript can be plugged in to any web server and presentation aspects can be overriden using annotations on the schema, css overrides and custom jquery script (the ability to separate script from structure is a nice feature of jquery in this instance).

## Examples

*Generated Form* | *Annotated schema* | *Comment* 
---------------- | ------------------ | ---------
[Demo form](http://htmlpreview.github.io/?https://github.com/davidmoten/xsd-forms/blob/master/xsd-forms-generator/src/docs/showcase/demo-form.html)|[schema](https://github.com/davidmoten/xsd-forms/blob/master/demo-scalaxb/src/main/resources/demo.xsd)|Feature showcase
[Australian Census 2011](http://htmlpreview.github.io/?https://github.com/davidmoten/xsd-forms/blob/master/xsd-forms-generator/src/docs/showcase/census-form.html)|[schema](https://github.com/davidmoten/xsd-forms/blob/master/xsd-forms-generator/src/test/resources/australian-census-2011.xsd)|Based on the 2011 Australian Census form ([pdf](http://www.abs.gov.au/ausstats/abs@.nsf/Lookup/2901.0Main%20Features802011))
[AMSA Pollution report](http://htmlpreview.github.io/?https://github.com/davidmoten/xsd-forms/blob/master/xsd-forms-generator/src/docs/showcase/polrep-form.html)|[schema](https://github.com/davidmoten/xsd-forms/blob/master/xsd-forms-generator/src/test/resources/polrep.xsd)|[pdf](https://github.com/davidmoten/xsd-forms/blob/master/xsd-forms-generator/src/docs/showcase/Appendix7.pdf?raw=true)

Note. The examples work fine in the *Chrome*, *Firefox* and *Internet Explorer 9* browsers (other browsers not tested yet). IE 9 will not display the above demos because they are sourced from htmlpreview.github.io which does not furnish the js with the appropriate mime-type. You can use the web service to display the demos succesfully for IE 9. IE is not being selenium tested yet but a SauceConnect job may be set up for that soon.

FAQ
-------------------

### I've already got a schema, can I generate a form from it?

Probably not! *xsd-forms* only supports a subset of xsd features. It was enough work for me to get this far, believe me! Supported features are documented in another section below. Your options are:

  * use an XSLT to translate xml to match your schema
  * translate the xml using some standard parser (xpath expressions etc)
  * generate classes from both schemas and write code to translate using the generated classes. This method gives you compile-time indications as the schemas change through time (and isn't change inevitable!). *This is my preferred option*.

### What xsd features are supported?
These xsd features are supported:

  * elements only, *not* attributes
  * sequence, choice
  * minOccurs, maxOccurs on elements or anonymous complex types
  * extension of complex types
  * restrictions by regex pattern, enumerations, ranges on numerics, maxLength, minLength, length
  * base simple types: string, boolean, date, datetime, time, integer, decimal, int, short, long, positiveInteger, negativeInteger, nonPositiveInteger, nonNegativeInteger, double, float
  * no explicit support for populating form fields (say from xml). xsd defaults will be honoured. JQuery overrides can be used to do custom initialization.

Bearing in mind the above restrictions, these features are supported:

  * top level elements 
  * top level complex types
  * top level simple types
  * anonymous complex types
  * anonymous complex content
  * anonymous simple types


### How do I generate a form?

You need to make a schema using only the elements and types that are supported by *xsd-forms*. You can generate the form without adding any annotations at that point and it will use element names and such as defaults. A starter schema looks like this:

```
<xs:schema targetNamespace="http://org.moten.david/example"
  xmlns="http://org.moten.david/example" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:i="http://moten.david.org/xsd-forms">
  <xs:annotation i:numberItems="true">
    <xs:appinfo>
      <i:header><![CDATA[
<h2>Title of the form</h2>
]]></i:header>
      <i:footer><![CDATA[
    <p>Thanks for your time.</p>
]]></i:footer>
      <i:extraImports><![CDATA[
    <!-- more imports here -->
]]></i:extraImports>
      <i:extraScript><![CDATA[
  // extra script would go here
]]></i:extraScript>
      <i:extraCss><![CDATA[
  /* extra css would go here */
]]></i:extraCss>
    </xs:appinfo>
  </xs:annotation>
  <xs:element name="name" type="xs:string">
    <xs:annotation i:label="Full name" />
  </xs:element>
</xs:schema>
```

Deploying this simple schema to the web service mentioned below gives you [this form](http://xsd-forms-generator.xuml-tools.cloudbees.net/generate?schema=<xs%3Aschema+targetNamespace%3D"http%3A%2F%2Forg.moten.david%2Fexample"%0D%0A++xmlns%3D"http%3A%2F%2Forg.moten.david%2Fexample"+xmlns%3Axs%3D"http%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema"%0D%0A++xmlns%3Ai%3D"http%3A%2F%2Fmoten.david.org%2Fxsd-forms">%0D%0A++<xs%3Aannotation+i%3AnumberItems%3D"true">%0D%0A++++<xs%3Aappinfo>%0D%0A++++++<i%3Aheader><%21%5BCDATA%5B%0D%0A<h2>Title+of+the+form<%2Fh2>%0D%0A%5D%5D><%2Fi%3Aheader>%0D%0A++++++<i%3Afooter><%21%5BCDATA%5B%0D%0A++++<p>Thanks+for+your+time.<%2Fp>%0D%0A%5D%5D><%2Fi%3Afooter>%0D%0A++++++<i%3AextraImports><%21%5BCDATA%5B%0D%0A++++<%21--+more+imports+here+-->%0D%0A%5D%5D><%2Fi%3AextraImports>%0D%0A++++++<i%3AextraScript><%21%5BCDATA%5B%0D%0A++%2F%2F+extra+script+would+go+here%0D%0A%5D%5D><%2Fi%3AextraScript>%0D%0A++++<%2Fxs%3Aappinfo>%0D%0A++<%2Fxs%3Aannotation>%0D%0A++<xs%3Aelement+name%3D"name"+type%3D"xs%3Astring">%0D%0A++++<xs%3Aannotation+i%3Alabel%3D"Full+name"+%2F>%0D%0A++<%2Fxs%3Aelement>%0D%0A<%2Fxs%3Aschema>&rootElement=&idPrefix=&action=view&method=get&submit=Submit).

#### How do I annotate my schema?

Every element in your schema optionally can have a contained &lt;xs:annotation/&gt; element. Place attributes on that element from the xsd-forms schema like below:

[annotations-demo.xsd](https://github.com/davidmoten/xsd-forms/blob/master/xsd-forms-generator/src/test/resources/annotations-demo.xsd) ---> [form](http://htmlpreview.github.io/?https://raw.github.com/davidmoten/xsd-forms/master/xsd-forms-generator/src/docs/showcase/annotations-demo.html)

For a full list of annotations and their meanings see the top of the file [annotation.scala](https://github.com/davidmoten/xsd-forms/blob/master/xsd-forms-generator/src/main/scala/com/github/davidmoten/xsdforms/presentation/annotation.scala).

This is what the form from annotations-demo.xsd looks like:

<img src="https://raw.github.com/davidmoten/xsd-forms/master/xsd-forms-generator/src/docs/screen1.png"/>

#### Generate using web service 
A [web service](http://xsd-forms-generator.xuml-tools.cloudbees.net/) is available. At this service you can 

  * Submit a schema document to the service and receive a zip archive of the generated site.
  * Submit a schema document and view the generated form 

#### Generate using a local web service

You need to have built everything once only:

    mvn clean install

Then 

    cd xsd-forms-generator-webapp
    mvn package jetty:run

Then go to [http://localhost:8080](http://localhost:8080) and fill in the form.

#### Generate using java/scala:

Add the following maven dependency to your pom.xml (you will need to have built the project from source so that the dependency is available in your local repository):
```
<dependency>
  <groupId>com.github.davidmoten.xsdforms</groupId>
  <artifactId>xsd-forms-generator</artifactId>
  <version>0.2.1</version>
</dependency>
```

and call

    xsdForms.generateZip(..) 
or 

    xsdForms.generateHtml(...)
or

    xsdForms.generateDirectory(...)

#### Generate using maven plugin

```
<plugin>
  <groupId>com.github.davidmoten.xsdforms</groupId>
  <artifactId>xsd-forms-maven-plugin</artifactId>
  <version>0.2.1</version>
  <executions>
    <execution>
	  <goals>
		<goal>generate</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <!-- schema location can be on classpath or file system. Classpath is checked first -->
    <schema>/demo.xsd</schema>
  </configuration>
</plugin>
```

Here is a [demonstration](https://github.com/davidmoten/xsd-forms/tree/master/xsd-forms-maven-plugin-demo) using *xsd-forms-maven-plugin*.

###What do I need to do after I have designed a schema?

The default generated form just displays the captured xml on the page under the submit button. You will likely want to post the generated xml to a web server or perhaps email the xml to an address. To do that just set *extraScript* to the script below to override the submit behaviour:

    //for example, display the xml in an alert box
    processXml = function (xml) {
      alert(xml);
    } 

###Can I submit JSON instead?
Yep. Use this *extraScript* (using [xml2json.js](https://code.google.com/p/x2js/)):

    //for example, display json in an alert box
    processXml = function (xml) {
      var result = toJson(xml);
      //alert or you could do a jquery ajax call to submit to a web server
      alert(result);
    }

###How do I override the appearance of the generated form?

Easy, just use javascript (jquery) in the *extraScript*. For instance, building on the above example:

```
//override the appearance of first input box
$('#item-6-instance-1_1_1').css("background", "aqua");

//override the default value of first input box
$('#item-6-instance-1_1_1').val("bingo");
```

An alternative is to put your own css overrides in xsd-forms-overrides.css.

###How can I do my own thing with the xml?
Use this *extraScript*:
```
//override the processXml function
processXml = function(xml) {
  //do whatever you want here!!
};
```

###How do I submit the xml/json to a web server?
Use this *extraScript*:
```
processXml = postXml('http://posttestserver456.com/post.php');
```
Or for more control use this *extraScript* and modify it:
```
//submit the xml to a web service using http post
processXml = function (xml) {
  var data = new Object();
  data.xml = xml;
  //disable submit button
  $('#submit').hide();
  $.ajax({
  type: 'POST',
  url: 'http://posttestserver456.com/post.php',
  data: data,
  success: 
    function (dat,textStatus,jqXHR) {
      $('#submit').hide();
    },
  error:
    function (jqXHR,textStatus,errorThrown) {
      alert(textStatus + '\n'+ errorThrown);
     $('#submit').show();
    },
  async:false
});
}
```

###How do I pre-populate a form?

Schema default values will be set but if you want to for instance restore a form so a user can edit it then you need to write the necessary javascript as in the above examples. Using javascript/jquery you can call web services or extract parameters from the current url to populate the form.

###Can I use the same schema for multiple different forms?

Yes. Just choose a different root element for each form.

Scope
--------------
The generated form should
  * perform all schema defined validation (e.g. regex pattern checks etc) 
  * notify validation failures in a _good practice_ way
  * build schema compliant xml from the input fields
  * not allow submission until all validation passes
  * offer ui options for handling xs:choice
  * offer ui options for handling xs:enumeration
  * follow _good practice_ presentation standards
  * facilitate tweaking of presentation using css override on every element
  * support small screen/large screen
  * support common mobile platforms

Building 
---------------------

### Technical overview
The use case is

* *Given an xml schema, generate html, javascript and css files that will capture input, perform validation and prepare an xml representation of the form detail compliant with the schema.*

### Building from source 
You need maven 3 installed and git binaries.

    git clone https://github.com/davidmoten/xsd-forms.git
    cd xsd-forms
    mvn clean install

### Running selenium tests
To run selenium tests (firefox, and chrome if setup):

    mvn clean install -Dselenium=true

### More options
Disable the chrome driver:

    -Dchrome=false
    
Disable the firefox driver:

    -Dfirefox=false

If you have the chromedriver executable installed ensure it is on the $PATH. For example, on linux

    export PATH=/opt/chromedriver:$PATH

where /opt/chromedriver is the directory containing the chromedriver executable.

### Viewing a sample form

    cd xsd-forms-generator
    mvn jetty:run

Then go to [http://localhost:8080/demo-form.html](http://localhost:8080/demo-form.html).

### Run the generator

    cd xsd-forms-generator-webapp
    mvn package jetty:run

Then go to [http://localhost:8080/](http://localhost:8080/)

### Development plan
  * use jquery to insert clones of div blocks to model maxOccurs > 1
  * demonstrate choice presentation options (inline and post, labelled and unlabelled)
  * implement choice presentation options
  * clean up generated jquery code
  * improve presentation 
  * enable css override
  * generate xml
  * unit tests of form generation
  * unit tests of form behaviour (selenium?) including xml schema compliance
  * use templating instead of coding divs directly in scala?
  * combine templating with css to make themes

