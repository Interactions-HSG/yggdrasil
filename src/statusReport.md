# Status Report 08.02.2019

## Software Artifacts
### Definition Template
* Software Artifacts Templates are defined using Annotations
* The annotations are defined in ro/andreiciortea/yggdrasil/template/annotation package
  * @Artifact(): Properties type(defferred), name(), prefixes(defferred)
  * @Action(): Properties path(url to invoke action), name(), type(defferred)
  * @ObservableProperty(): path(url to get property value), name()
  * @Event(): path(defferred), name()
  
* Example artifact: ro/andreiciortea/yggdrasil/template/MyArtifact.java

### Endpoints
* GET /artifacts/templates returns RDF description of all available templates
* POST /artifacts/templates instantiates template object, returns id of created software artifact

* PUT /artifacts/:artid/definedActionPath calls an action defined in annotation, arguments as json payload
* PUT /artifacts/:artid/definedPropertyPath returns the value of the property as json payload (to be changed to GET)

### Postman Request Collection
Just some example requests
https://www.getpostman.com/collections/2069ddd0ab4c812b6165

