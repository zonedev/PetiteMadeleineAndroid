# petite madeleine client for Android

petite madeleine uses image recognition to extend print publications with supplemental media.

## Example Data

petite madeleine retrieves all content from the web. Therefore it is neccessary to provide data on a server accessible via HTTP to use the client. See the [petite madeleine example data](https://github.com/zonedev/PetiteMadeleineExampleData) repository for a description of the data format.

## Image Recognition Service

petite madeleine relies on webservices for image recognition. The generalized interface `IRService` is provided to enable easy implementation of different services. `PixlinqSearch` is provided as an example implementation for [pixlinQ mobile visual search](http://www.pixlinq.com).

## Notes

`PetiteMadeleineBasicActivity` is a sample implementation of the main activity of a basic petite madeleine client. A valid URL to master property list data must be provided to the `PetiteMadeleineCore` Object for the sample to work (see [petite madeleine example data](https://github.com/zonedev/PetiteMadeleineExampleData) for further details).