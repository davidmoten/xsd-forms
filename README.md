xsd-forms
=========

Moved from http://code.google.com/p/xsd-forms/

Xml schemas contain nearly everything we need for many web forms except for presentation information. Instead of seeking full separation of presentation from the data model (the xml schema) there are significant advantages in [annotating the xsd itself](http://code.google.com/p/xsd-forms/source/browse/xsd-forms/trunk/demo-scalaxb/src/main/resources/demo.xsd) according to a [special schema](http://code.google.com/p/xsd-forms/source/browse/xsd-forms/trunk/xsd-scalaxb/src/main/xsd/xsd-forms.xsd) to indicate presentation details. For one, refactoring is much easier without specialized IDE tooling that can read both our xsd and our presentation format. 

*Status:* pre-alpha. Alpha release planned for December 2013 (excuse delay, very busy on other projects!). UI elements mostly done, a major rewrite was merged to master on 15 Nov 2013 to use an abstract syntax tree and to handle element repetition in a simplistic way. Lots of bits and pieces to chase down but many simple forms will be correctly generated now (try it and see).

Continuous integration with Jenkins for this project is [here](https://xuml-tools.ci.cloudbees.com/). <a href="https://xuml-tools.ci.cloudbees.com/"><img  src="http://web-static-cloudfront.s3.amazonaws.com/images/badges/BuiltOnDEV.png"/></a>

Maven site is [here](https://xuml-tools.ci.cloudbees.com/job/xsd-forms_site/site/). 

Design
-----------------
<img src="https://raw.github.com/davidmoten/xsd-forms/master/xsd-forms-generator/src/docs/diagram01.png"/>

**Primary Use Case**
  * A developer needs to create a web form. 
  * The developer creates an annotated schema as per xsd-forms and generates pure html and javascript (no server side scripting). 
  * The developer links the form to a server side component that processes the submitted xml. 

As the submitted xml is compliant with a schema, the xml can be unmarshalled by a tool (see [jaxb](http://www.oracle.com/technetwork/articles/javase/index-140168.html), [scalaxb](http://scalaxb.org), [xsd.exe](http://msdn.microsoft.com/en-us/library/x6c1kb0s.aspx) into schema generated objects. Consequently any change in the schema will be reflected by compilation errors in the processing code if a statically typed language is used (such as Java, Scala, C#, etc.).

The features of javascript libraries like [http://jquery.com](JQuery) mean that generating an html/javascript form that on submit posts xml that is compliant with the schema is a viable approach. The resultant css/html/javascript can be plugged in to any web server and presentation aspects can be overriden using annotations on the schema, css overrides and custom jquery script (the ability to separate script from structure is a nice feature of jquery in this instance).

*Examples:*

*Generated Form* | *Annotated schema* | *Comment* 
---------------- | ------------------ | ---------
[Demo form](http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/main/webapp/demo.html)|[schema](http://code.google.com/p/xsd-forms/source/browse/xsd-forms/trunk/demo-scalaxb/src/main/resources/demo.xsd)|Feature showcase
[Australian Census 2011](http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/main/webapp/census.html)|[schema](http://code.google.com/p/xsd-forms/source/browse/xsd-forms/trunk/xsd-forms-generator/src/test/resources/australian-census-2011.xsd)|Based on the 2011 Australian Census form ([pdf](http://www.abs.gov.au/ausstats/abs@.nsf/Lookup/2901.0Main%20Features802011))


Note that the examples are not fully working and are still in development. The examples look ok in the *Chrome* and *Firefox* browsers (other browsers not tested yet). Many features like schema sourced validation are working, and xml generation is working (click Submit on a form). 

FAQ
-------------------

###I've already got a schema, can I generate a form from it?

Probably not! *xsd-forms* only supports a subset of xsd features. It was enough work for me to get this far, believe me! Supported features are documented in another section below. Your options are:

  * use an XSLT to translate xml to match your schema
  * translate the xml using some standard parser (xpath expressions etc)
  * generate classes from both schemas and write code to translate using the generated classes. This method gives you compile-time indications as the schemas change through time (and isn't change inevitable!). *This is my preferred option*.

###What xsd features are supported?
These xsd features are supported:

  * elements only, *not* attributes
  * sequence, choice
  * maxOccurs >= 1 on elements or anonymous complex types
  * restrictions by regex pattern, enumerations, ranges on numerics, maxLength, minLength, length
  * base simple types: string, boolean, date, datetime, time, integer, decimal
  * no explicit support for populating form fields (say from xml). xsd defaults will be honoured. JQuery overrides can be used to do custom initialization.

Bearing in mind the above restrictions, these features are supported:

  * top level elements 
  * top level complex types
  * top level simple types
  * anonymous complex types
  * anonymous complex content
  * anonymous simple types


###How do I generate a form?

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
  <groupId>org.moten.david</groupId>
  <artifactId>xsd-forms-generator</artifactId>
  <version>${project.parent.version}</version>
</dependency>
```

and call

    xsdForms.generateZip(..) 
or 

    xsdForms.generateHtml(...)
or

    xsdForms.generateDirectory(...)

#### Generate using maven plugin

The project includes a [demonstration](https://github.com/davidmoten/xsd-forms/tree/master/xsd-forms-maven-plugin-demo) using *xsd-forms-maven-plugin*.

###What do I need to do after I have designed a schema?

The default generated form just displays the captured xml on the page under the submit button. You will likely want to post the generated xml to a web server or perhaps email the xml to an address. To do that just set *extraScript* to the script below to override the submit behaviour:

    //for example, display the xml in an alert box
    processXml = function (xml) {
      alert(xml);
    } 

###How do I override the appearance/behaviour of the generated form?

Easy, just use javascript (jquery) in the *extraScript*. For instance, building on the above example:

```
// this script will be included in the jquery on-load block

//override the processXml function
processXml = function(xml) {
  alert(xml);
};

//override the appearance of first input box
$('#item-6-instance-1_1_1').css("background", "aqua");

//override the default value of first input box
$('#item-6-instance-1_1_1').val("bingo");
```

###How do I pre-populate a form?

Schema default values will be set but if you want to for instance restore a form so a user can edit it then you need to write the necessary javascript as in the above examples. Using javascript/jquery you can call web services or extract parameters from the current url to populate the form.

###Can I use the same schema for multiple different forms?

Yes. Just choose a different root element for each form.

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

##Schema annotations
TODO

## Current work
A major redesign is occurring now. The plan is this:

  * use abstract syntax tree to represent xsd simplified to enable recursive traversal of the xsd in scala
  * against all elements using an element number include an instance number to support maxOccurs>1
  * in the generated page store a hidden copy of the form html with instance=1 to be used as the source for repeating nodes in the tree 
  * the initial view will be with of the form html with instance=2
  * store a global variable in js to record instance counter
  * whenever a node is added increment the instance counter and use it in the newly created node and its descendants


