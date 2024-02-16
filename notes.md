# Notes to integrate hmas

The TD representation is deeply coupled with the entire source code. Adding Hmas on top seems crude.
Should either refactor existing code to be more extensible and rely less on TD or copy all classes and rewrite for Hmas.


## Relevant Classes
* ### HypermediaArtifact.java
  Class that extends Cartago Functionality -> handles Artifact modification etc.
  Tightly linked to TDs as it holds its actionAffordances in TD Class.
  - Should be more generic class to enable multiple representations.
  - Same holds true for most operations of this class.
  - Class is not HypermediaArtifact but TDArtifact imo.

  - ### possible solutions
    - Refactor the Class
    - Create additional HmasHypermediaArtifact Class

* ### HttpEntityHandler.java
  - Handler -> uses methods
  - Can change once methods signatures are known for hmas impl.

* ### CartagoVerticle.java
  - Can change once methods signatures are known for hmas impl.

* ### RepresentationFactoryImpl.java
  * #### createPlatform
    - What is equivalent Hmas Platform to TD platform?
    - Hmas-Java does not offer functionality to add affordance to platform
      - How do we add createWorkspace signifier?
  * #### createWorkspace
    - Similar problem as above
    - How do we add Signifiers to a Workspace
  * #### createArtifact
    - Artifact gets sent ActionAffordances -> TD
    - addGraph functionality does not exist for Hmas
  * #### createBody
    - addGraph functionality does not exist for Hmas
    - securityScheme doesnt exist



* ### RepresentationFactory.java
  - adjust interface

* ### RdfModelUtils.java
  - It currently overwrites the namespace lines
  - didnt matter for TD but matter for hmas
    - @base gets deleted


# General Questions
- Difference between printing the .write() and publishing it
  - Where do the URIs come from
- How is hmas actually structured?
  - What is the correct translation of TD to hmas
  - Where do I add which IRI
- How does Signifier actually work?
- What is the need for cartago / why do we need artifact code in the src code?
  - If we want hypermedia shouldnt all artifacts be semantic
  - Agents (3rd parties) want to add new artifacts at runtime (clashes with having to add class)
  - Why not just purely use semantic types?
