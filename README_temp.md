# TECHNICAL DESCRIPTION #


DATE : 12 August 2013
SOFTWARE : Mkey+ Interactive Identification Webservice
AUTHORS : Antoine Bergamaschi, Thomas Burguiere


# Description

Mkey+ is a webservice that provides interactive identification capabilities to
any third-party software that can interact with a webservice, using standard
HTTP requests. The preferred data exchange format is the JSON (JavaScript Object
Notation) format.



# Requirements

The initial descriptive data must be stored in a valid SDD file. SDD is the
standard type file used in systematic biology to store descriptive data. This
SDD file must be reachable on the internet, via a public url (e.g.
http://your.server.adress/yourSDDfile.xml).

If your descriptive data is stored in the delta format you can convert it to sdd
using the [deltaToSdd](http://www.identificationkey.fr/deltatosdd/) webservice.
If your descriptive data is stored in the Xper2 format you can convert it to sdd
using the Xper2 software Export option.

Change heap space memory if necessary : in /etc/default/tomcat7 file `JAVA_OPTS="-Djava.awt.headless=true -Xms2048m -Xmx8192m -XX:+UseConcMarkSweepGC"`

mkey-plus-API-1.0.jar and xper3API-1.0.jar have to be present in WebContent/WEB-INF/lib/ directory

# Using Mkey+ with our web client.

If you want to perform an interactive identification without having to
develop your own webservice client, you can use the simple web client we
developed internally, which will act as the client, allowing you to interact
with Mkey+. This web client is available at the following address: http://www.identificationkey.fr/mkeyplus/

Five points to present and use :

1. Load the SDD file in the modal window displayed when opening the web client.
If this window is locked or if you want to perform an interactive
		identification using another SDD file, simply reload the web page.

2. Descriptors are displayed on the left, sorted by discriminating power in descending order. Clicking on a Descriptor displays its states.

3. The states are not sorted, clicking on a state toggles a submit button on
the Descriptors block, which allows you to submit the created description (association
Descriptor + states). If more than one selected state is selected, the selected
states are considered to be linked by an "OR" association.

4. The remaining items block on the left will be updated each time a
description is submitted. Clicking on each item displays its complete description

5. repeat from point 2 while the identification is not completed. The reset
button and history button on the top of the Descriptor block, allow you to
modify the current description, and re-start the identification.

# Functional description


Webservice function :

The Mkey+ webservice provides 7 functions. These functions can be queried using
HTTP requests. They can be reached using URLs created with the webservice
address (http://queen.snv.jussieu.fr:8080/mkey-plus-webservice-REST) and a path
to a webservice function. The seven functions are:

1. `/identification/getDescriptiveData`

2. `/identification/getRemainingItemsAndDescriptorsUsingIDs`

3. `/identification/getDescription`

4. `/identification/removeSDD`

5. `/identification/changeDescriptionHistory`

6. *`/identification/getSimilarityMap`*

7. *`/identification/getSimilarityMapForRemainingItem`*


For instance, the URL to call the first function is:
http://queen.snv.jussieu.fr:8080/mkey-plus-webservice-REST/identification/getDescriptiveData

These functions return Json objects, and each element returned has a specific
name (see Development API).


# Performing an Interactive Identification using Mkey+ and a custom client



## REST API

The parameters in entry of the Mkey+ API functions only use the stringified (using `JSON.stringify()`) version of the Mkey+ JSON objects.

The webservice listen to GET query methods and the dataType must be Jsonp.

Here is an example of a JQuery javascript code snippet that calls one of MKey+
function:
```javascript
  var webserviceURL = http://queen.snv.jussieu.fr:8080/mkey-plus-webservice-REST
  var sddFileURL = http://yourServer.com/yourSddFile.xml
  $.ajax({
	  url : webserviceURL + '/identification/getDescriptiveData',
	  data : {
		      sddURL : sddFileURL,
		      withGlobalWeigth : true
			 },
	method : 'GET',
	dataType : 'jsonp'

  }).done(function(data) {
 	… response management …
  }
```


### Basic objects used and returned by Mkey+

#### Item
 This object represents an item which can be described in a knowledge base.
For taxonomists, it usually is a taxon.

| field name 		  | type   | Description |
| :-------------- | :----- | :---------- |
| name            | String | |
| alternativeName | String | |
| detail          | String | |
| resourceIds     | [int]  | A list of associated media resource's ids|
| id              | int    |  ||

```javascript
 {
	"name" : "Canis Lupus",
	"alternativeName" : "Wolf",
	"detail" : "longer description",
	"resourceIds" : [1,2,3],
	"id" : 1
}
```

#### Descriptor
This object is a tool that serves to describe Items, essentially a
Character for taxonomists. A Descriptor can be described with States (if it is a
categorical Descriptor), or a Quantitative Measure (If it is a quantitative
Descriptor).

| field name 				 | type    | Description |
| :----------------- | :------ | :---------- |
| name               | String  | a Descriptor's name
| detail             | String  | a Descriptor's detailed description
| resourceIds        | [int]   | the IDs of the resources associated to a Descriptor
| stateIds           | [int]   | the IDs of the states associated to a Descriptor
| inapplicableState  | [int]   | the states of a parent Descriptor for which a Descriptor is inapplicable
| isCategoricalType  | boolean | true if the Descriptor is a categorical						Descriptor, false otherwise
| isQuantitativeType | boolean | true if the Descriptor is a quantitative						Descriptor, false otherwise
| isCalculatedType   | boolean | true if the Descriptor is a calculated						Descriptor, false otherwise
| id                 | int     | this Descriptor's id, an unique identifier generated sequentially by Mkey+

```javascript
{
	name : "Number of teeth",
	detail : "more details ........",
	resourceIds : [3,12],
	stateIds : [1,3],
	inapplicableState : [2],
	isCategoricalType : false,
	isQuantitativeType : true,
	isCalculatedType : false,
	id : 3		
}
```

#### State

This object is a component of Categorical Descriptors, e.g. for a Descriptor named "Color of the eye", its States could be "Blue", "Black", etc...


| field name 		  | type   | Description |
| :-------------- | :----- | :---------- |
| name            | String | |
| detail          | String | |
| resourceIds     | [int]  | A list of associated media resource's ids|
| id              | int    |  ||

```javascript
{
	name : "String",
	detail : "String",
	resourceIds : [1],
	id : 3,
}
```

#### Resource

This object is a storage object, and is used to store media resources,
which can be associated to several objects, such as Items, Descriptors, States,
etc...

```javascript
{
	name : "String", //a resource name
	author : "String", //a resource author
	type : "String", //a resourcemedia type ("video","image","sound")
	url : "String", //a resource url
	legend : "String", // a resource legend
	keywords : "String", //a resource keywords
	id : 33,
}

```

#### DescriptionElement

This object stores the description of an Item, according to
a single Descriptor, it represents the content of a single cell of the taxa /
characters matrix. It may contain the list of selected States if the Descriptor
is a categorical Descriptor, or a QuantitativeMeasure object, if the Descriptor
is a quantitative Descriptor.

```javascript
{
	calculatedStates : [{...}, {...}],
				// the calculated states representing an item's description
	contextualWeight : int,
				// the weight of this description element
	quantitativeMeasure : {...},
				// the quantitative measure representing an item description
	states : [{...}, {...}],
				//the states representing an item's description
	unknown : boolean,
				// true if this description element is unkwnown
}
```

#### QuantitativeMeasure
This object is associated to a DescriptionElement object, for a given quantitative Descriptor and Item, it contains the quantitative measures used to describe a specific Item for a given quantitative Descriptor.

```javascript
{
	min : long,  // this QuantitativeMeasure's minimum value
	max : long,  // this QuantitativeMeasure's maximum value
	mean : long, // this QuantitativeMeasure's mean value
}
```

### 2 `getDescriptiveData`

First function to be called, `getDescriptiveData` initializes the webservice both on the client and server side. It returns every element used in the identification process, parsed from the SDD file.

path :

```http
/identification/getDescriptiveData?sddURL={your url}&withGlobalWeigth={true}
```

parameter (in the javascript before stringify):
SDDurl = String
withGlobalWeigth = Boolean

returns :
{descriptorsScoreMap,Items,Descriptors,States,Resources,DescriptorRootId,Depend-
-ancyTable,InverteddependencyTable}


Items = [item], every item contained in the SDD
Descriptors = [Descriptor], every Descriptor contained in the SDD
States = [state], every states contained in the SDD
Resources = [resource], every resources contained in the SDD
descriptorsScoreMap = {Descriptor,float}, an associative map, which associates
	to each Descriptor its discriminant power.
DescriptorRootId = [int], ids of the node roots ( no dependency)
InvertedDependencyTable = {long,long}, the first long is the ID of the descrip-
	-tor which parent is the second long.



2 - getJSONRemainingItemsAndRemainingDescriptorsScoreUsingIds

Main function, used to retrieve the remaining (i.e. non-discarded) Items based
on a submitted description. It also returns the remaining Descriptors, along
with their new discriminant power.

path :
/identification/getJSONRemainingItemsAndRemainingDescriptorsScoreUsingIds(String
SDDurl, String descriptions, String remainingItemsIDs, String discardedDescrip-
-torsIDs, Boolean withScoreMap, Boolean withGlobalWeigth)

parameter (in the javascript before stringify):
SDDurl = String
description = {
	selectedStatesNames : [int]
	quantitativeMeasure : {min=int,max=int,mean=int}
}
remainingItemsID = [int]
discardedDescriptorsID = [int]
withScoreMap = boolean
withGlobalWeigth = boolean



returns :
{discardedDescriptorsInIteration,remainingItems,descriptorScoreMap}

remainingItems = [item], array of remaining items
discardedDescriptorsInIteration = [Descriptor], array of discarded descriptor,
	child descriptor can be discarded with its parents.
descriptorsScoreMap = {Descriptor,float}, an associative map, which associates
	to each Descriptor its discriminant power.


3 - getDescription

Returns the description (i.e. the description of all the Descriptors) for an
Item whose name has been passed as an argument.
Used to display the item description window.

path :
/identification/getDescription(String itemName, String SDDurl)

parameter (in the javascript before stringify):
itemName : String
SDDurl : String


returns :
{description,innapDescriptorId}

description = [descriptionElement], array of description elements describing
	this item.
innapDescriptorId = [int], array of inapplicable descriptor IDs


4 - removeSDD

Delete the sdd from the Mkey+ server memory.

/identification/removeSDD(String SDDurl)

parameter (in the javascript before stringify):
SDDurl : String

returns :
  nothing


5- changeDescriptionHistory

Compute identification based on the description given in parameter. This func-
-tion is used to modify at all time user's identification.

/identification/changeDescriptionHistory

parameter (in the javascript before stringify):
SDDurl : String
descriptions : [{
					selectedStatesNames : [int],
					quantitativeMeasure : {min=int,max=int,mean=int}
			   }]

returns :
{discardedDescriptors,remainingItems,descriptorScoreMap,descriptions}

remainingItems = [item], array of remaining items
discardedDescriptorsInIteration = [Descriptor], array of discarded descriptor,
	child descriptor can be discarded with its parents.
descriptorsScoreMap = {Descriptor,float}, an associative map, which associates
	to each Descriptor its discriminant power.
descriptions = [{selectedStatesNames : [int],
				quantitativeMeasure : {min=int,max=int,mean=int}
			   }], the new description computed by the function.


/**BETA**/
6- getSimilarityMap

Return a map which link items with a float representing a similarity score.
This score is computed base on the descriptions submitted by the user. Items
which have few difference with the descriptions given in parameter will have a
high score.

/identification/getSimilarityMap

returns :
{similarityMap}

similarityMap = [[int,float]], int is the item ID and float the corresponding si
milarity score.

7- getSimilarityMapForRemainingItem

Return a map which link items with a float representing a similarity score.
This score is computed base on a global description regrouping every remaining
items description elements. Items which have few difference with the descrip-
tions given in parameter will have a high score.

/identification/getSimilarityMapForRemainingItem

returns :
returns :
{similarityMap}

similarityMap = [[int,float]], int is the item ID and float the corresponding si
milarity score.
