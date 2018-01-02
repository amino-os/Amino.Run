# Design of Code Offloading

## Introduction

Mobile applications are commonly modeled as client server architecture where client components run on mobile devices and server logics are provided by web services in the cloud. DCAP, however, provides a mechanism to migrate *client components* between mobile devices and servers in cloud. With DCAP framework, mobile application developers can annotate some client components as `offload-enabled` and specify an offload objective function, e.g. to improve responsiveness, DCAP runtime will automatically migrate objects between mobile devices and servers in cloud to maximize the objective function.

* **Client Component Offload**: This design only focuses on client component offload. Migration of server side objects are not considered. What are client components? Client components are objects that are inteneded to be run on devices. They serve for one specific user, rather than shared by multiple users. 

* **Offload between Device and Cloud**: This design only focuses on migrating objects between mobile devices and servers in cloud. While cloud can be further categorized as *central cloud* and *edge cloud*, to keep things simple, this design does not make such a distinguish.

## Use Cases

*  	**Moving Object to Improve Responsiveness**: The optimal placement of an object, on a device or on a server, depends on the configuration of the device hardware and the characteristics of the object. The following diagram demonstrates both points. For example, for the OCR application, cloud offloading over Wifi connection works for tablets, but not for phones. Similar, on the same mobile phone, cloud offloading over 3G connection works for Sudoku, but not for ChessAI.

![](./images/CodeOffloadPerformance.png)

* **Moving Object to Save Power Consumption**: When a mobile device is running low on battery power, we may consider moving objects from device to server, or vice versa, to reduce the power consumption on device. 

## Application Model



