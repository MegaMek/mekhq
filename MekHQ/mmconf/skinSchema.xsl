<?xml version="1.0" encoding="UTF-8"?>

<!-- Schema for defining images and colors to be used for different UI elements in Megamek -->
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
	elementFormDefault="qualified">

	<xs:element name="skin">
		<xs:complexType>
			<xs:sequence>
				<xs:element minOccurs="1" maxOccurs="unbounded" ref="UI_Element" />
			</xs:sequence>
		</xs:complexType>
	</xs:element>

	<xs:element name="UI_Element">
		<xs:complexType>
			<xs:sequence>
				<!-- The name of the UI element -->
				<xs:element minOccurs="1" maxOccurs="1" name="name" type="xs:string" />
				<!-- Specification of border images -->
				<xs:element minOccurs="1" maxOccurs="1" ref="border" />
				<!-- Specification of background images -->
				<xs:element minOccurs="0" maxOccurs="unbounded" name="background_image" type="xs:string" />
			</xs:sequence>
		</xs:complexType>
	</xs:element>

	<!-- Defines the images that will be used in a border -->
	<xs:element name="border">
		<xs:complexType>
			<xs:sequence>
				<!-- Corner images -->
				<xs:element minOccurs="1" maxOccurs="1" name="corner_top_left" type="xs:string" />
				<xs:element minOccurs="1" maxOccurs="1" name="corner_top_right" type="xs:string" />
				<xs:element minOccurs="1" maxOccurs="1" name="corner_bottom_left" type="xs:string" />
				<xs:element minOccurs="1" maxOccurs="1" name="corner_bottom_right" type="xs:string" />
				<!-- Border lines: these images will be tiled -->
				<xs:element minOccurs="1" maxOccurs="1" name="line_top" type="xs:string" />
				<xs:element minOccurs="1" maxOccurs="1" name="line_right" type="xs:string" />
				<xs:element minOccurs="1" maxOccurs="1" name="line_left" type="xs:string" />
				<xs:element minOccurs="1" maxOccurs="1" name="line_bottom" type="xs:string" />
			</xs:sequence>
		</xs:complexType>
	</xs:element>


</xs:schema>
