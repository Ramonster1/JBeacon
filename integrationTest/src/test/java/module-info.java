module com.jbeacon.integrationTest {
	opens com.jbeacon.integrationTest.aeron.command to org.junit.platform.commons;
	opens com.jbeacon.integrationTest.core.poll to org.junit.platform.commons;

	requires org.apache.logging.log4j;
	requires com.lmax.disruptor;
	requires io.aeron.all;
	requires org.junit.jupiter.api;

	// Aeron modules
	requires java.compiler;
	requires java.management;
	requires jdk.unsupported;

	requires static lombok;

	requires com.jbeacon.core;
	requires com.jbeacon.aeron;
}