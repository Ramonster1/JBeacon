module com.jbeacon.aeron {
	requires org.apache.logging.log4j;
	requires com.lmax.disruptor;
	requires io.aeron.all;

	requires com.jbeacon.core;

	exports com.jbeacon.aeron.command;
}
