xsd-forms
=========

Moved from http://code.google.com/p/xsd-forms/

Xml schemas contain nearly everything we need for many web forms except for presentation information. Instead of seeking full separation of presentation from the data model (the xml schema) there are significant advantages in [annotating the xsd itself](http://code.google.com/p/xsd-forms/source/browse/xsd-forms/trunk/demo-scalaxb/src/main/resources/demo.xsd) according to a [special schema](http://code.google.com/p/xsd-forms/source/browse/xsd-forms/trunk/xsd-scalaxb/src/main/xsd/xsd-forms.xsd) to indicate presentation details. For one, refactoring is much easier without specialized IDE tooling that can read both our xsd and our presentation format. 

*Status:* pre-alpha. Alpha release planned for December 2013 (excuse delay, very busy on other projects!). UI elements mostly done, xml generation from the completed form not working properly yet. Update 27 Oct 2013: Doing major rewrite of xsd-forms to represent and process the schema as an abstract syntax tree. Have realized that the xml extraction step needs some major rework. Repetitions of elements requires quite a lot of extra smarts in the javascript. If interested can view progress in the [tree branch](https://github.com/davidmoten/xsd-forms/tree/tree).

Primary Use Case
-----------------
  * A developer needs to create a web form. 
  * The developer creates an annotated schema as per xsd-forms and generates pure html and javascript (no server side scripting). 
  * The developer links the form to a server side component that processes the submitted xml. 

As the submitted xml is compliant with a schema, the xml can be unmarshalled by a tool (see [jaxb](http://www.oracle.com/technetwork/articles/javase/index-140168.html), [scalaxb](http://scalaxb.org), [xsd.exe](http://msdn.microsoft.com/en-us/library/x6c1kb0s.aspx) into schema generated objects. Consequently any change in the schema will be reflected by compilation errors in the processing code if a statically typed language is used (such as Java, Scala, C#, etc.).

The features of javascript libraries like [http://jquery.com](JQuery) mean that generating an html/javascript form that on submit posts xml that is compliant with the schema is a viable approach. The resultant css/html/javascript can be plugged in to any web server and presentation aspects can be overriden using annotations on the schema, css overrides and custom jquery script (the ability to separate script from structure is a nice feature of jquery in this instance).

<img src="https://raw.github.com/davidmoten/xsd-forms/master/xsd-forms-generator/src/docs/diagram01.png"/>

*Examples:*

*Generated Form* | *Annotated schema* | *Comment* 
---------------- | ------------------ | ---------
[Demo form](http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/main/webapp/demo.html)|[schema](http://code.google.com/p/xsd-forms/source/browse/xsd-forms/trunk/demo-scalaxb/src/main/resources/demo.xsd)|Feature showcase
[Australian Census 2011](http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/main/webapp/census.html)|[schema](http://code.google.com/p/xsd-forms/source/browse/xsd-forms/trunk/xsd-forms-generator/src/test/resources/australian-census-2011.xsd)|Based on the 2011 Australian Census form ([pdf](http://www.abs.gov.au/ausstats/abs@.nsf/Lookup/2901.0Main%20Features802011))


Note that the examples are not fully working and are still in development. The examples  look ok in the *Chrome* and *Firefox* browsers (other browsers not tested yet). Many features like schema sourced validation are working, and xml generation is partially working (click Submit on a form). 

How to use it?
----------------
There will be options for that. 

  * maven plugin (java developer)
  * ant script (java developer)
  * web service (other)

Once a beta version is ready, we will deploy a web service (Rest/SOAP) to Google App Engine at http://xsd-forms.appspot.com. Submit a schema document to the service and receive a zip archive in return of the generated files.

Getting Started
----------------

### Technical overview
The use case is
 Given an xml schema, generate html, javascript and css files that will capture input, perform validation and prepare an xml representation of the form detail compliant with the schema.

### Building from source 
You need maven 3 installed and subversion binaries.
{{{
git clone https://github.com/davidmoten/xsd-forms.git
cd xsd-forms
mvn clean install
}}}

### Running selenium tests
To run selenium tests (firefox, and chrome if setup):
{{{
mvn clean install -Dselenium=true
}}}

### More options
Disable the chrome driver:
{{{
-Dchrome=false
}}}
If you have the chromedriver executable installed ensure it is on the $PATH. For example, on linux
{{{
export PATH=/opt/chromedriver:$PATH
}}}
where /opt/chromedriver is the directory containing the chromedriver executable.

### Viewing a sample form
{{{
cd xsd-forms-generator
mvn jetty:run
}}}
Then go to http://localhost:8080/demo-form.html.

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


### Features
Types|Simple base types (string, decimal, integer, date, datetime, time, boolean)|&#10004;
-----|---------------------------------------------------------------------------|--------

|| ||Named simple types||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
|| ||Named complex types||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
|| ||Anonmyous complex types||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
||Restriction||base=string||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
|| ||other bases||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
|| ||minLength||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
|| ||maxLength||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
|| ||regex pattern||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
|| ||OR on multiple regex pattern||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif ||
||Element||minOccurs=0,1||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
|| ||minOccurs>1|| ||
|| ||maxOccurs=1,unbounded||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
|| ||maxOccurs>1||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
||Structure||Sequence||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
|| ||Choice||http://xsd-forms.googlecode.com/svn/xsd-forms/trunk/xsd-forms-generator/src/test/resources/tick.gif||
|| ||All|| ||
||Generate xml|| || ||

Scope
--------------
The initial proposal is to only support certain xsd features including:

  * elements only, not attributes
  * top level elements 
  * top level complex types
  * top level simple types
  * anonymous complex types
  * anonymous complex content
  * anonymous simple types
  * sequence, choice
  * maxOccurs >= 1 on elements or anonymous complex types
  * restrictions by regex pattern, enumerations, ranges on numerics, maxLength, minLength, length
  * base simple types: string, boolean, date, datetime, time, integer, decimal
  * no explicit support for populating form fields (say from xml). xsd defaults will be honoured. JQuery overrides can be used to do custom initialization.

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


  
