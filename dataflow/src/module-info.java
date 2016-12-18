module org.checkerframework.dataflow {
	requires public org.checkerframework.javacutil;
	requires java.xml.ws;
	requires annotationtools.scenelib;
	requires guava;

	exports org.checkerframework.dataflow.analysis;
	exports org.checkerframework.dataflow.cfg;
	//exports org.checkerframework.dataflow.qual;
	exports org.checkerframework.dataflow.util;

}
